package client;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class UdpClient extends AbstractVerticle {

    private final AtomicInteger count = new AtomicInteger(0);

    private Map<Integer, Buffer> packetBufferMap = new LinkedHashMap<>();

    private static Logger LOGGER = LoggerFactory.getLogger(UdpClient.class);

    private long mac = UniqueNumberGenerator.getMac();
    private byte pt = 1;
    private int id = 0;
    private long time = 0;
    private double latitude = -0.1;
    private double longitude = -0.1;
    private int listenPort = UniqueNumberGenerator.getPort();

    // Счётчик количества пакетов в секунду. Когда = 0, то клиент перестанет отправлять пакеты
    private int packetSpamCounter = 1;

    private final int serverPort = 8181;
    private final String serverHost = "localhost";

    private static long sentPacketCounter = 0;
    private static long confirmedPacketCounter = 0;

    @Override
    public void start() throws Exception {
        super.start();

        // запускаем клиент для отправки UDP пакетов
        DatagramSocket udpClient = vertx.createDatagramSocket(new DatagramSocketOptions());

        // формируем пакет для отправки
        time = System.currentTimeMillis();
        latitude = getRandomDoubleInRange(-999999.0, 0.0);
        longitude = getRandomDoubleInRange(-999999.0, 0.0);
        id = count.getAndIncrement();

        Buffer buffer = Buffer.buffer(41);

        buffer.appendLong(mac).appendByte(pt).appendInt(id).appendLong(time).appendDouble(latitude).
                appendDouble(longitude).appendInt(listenPort);

        // отсылаем пакет
        sendPacket(udpClient, buffer, 1000);

        // получение пакета-подтверждения от сервера
        DatagramSocket clientReciever = vertx.createDatagramSocket(new DatagramSocketOptions());

        clientReciever.listen(listenPort, "localhost" , listenAsyncResult -> {
            if (listenAsyncResult.succeeded()) {

                clientReciever.handler(packet -> {

                    long mac = packet.data().getLong(0);
//                    byte pt = packet.data().getByte(8);
                    int id = packet.data().getInt(9);

                    // если id подтверждающего пакета имеется в пуле пакетов клиента и значение mac совпадают
                    // значит это пришёл действительно пакет-подтверждение для этого клиента и его можно из пула удалить
                    if (!packetBufferMap.isEmpty()) {
                        if (packetBufferMap.containsKey(id) &&
                                packetBufferMap.get(id).getLong(0) == mac) {
                            packetBufferMap.remove(id);
                        } else {
                            LOGGER.error("Клиент -> ошибка соответствия подтверждающего пакета! ");
                        }
                    }

                    confirmedPacketCounter++;
                    LOGGER.debug("Клиент -> подтверждено пакетов " + confirmedPacketCounter);
                });
            } else {
                LOGGER.error("Клиент -> ошибка подтверждения пакета ", listenAsyncResult.cause());

                if (packetBufferMap.size() > 1)
                    LOGGER.warn("Клиент -> пул неподтверждённых пакетов равен: " + packetBufferMap.size());

                LOGGER.info("Клиент -> посылка пакета(ов) заново");

                if (!packetBufferMap.isEmpty()) {
                    for (Map.Entry<Integer, Buffer> entry : packetBufferMap.entrySet()) {
                        packetSpamCounter = 3;
                        sendPacket(udpClient, entry.getValue(), 1000);
                    }
                }
            }
        });
    }

    private void sendPacket(DatagramSocket udpClient, Buffer buffer, int delay) {

        vertx.setPeriodic(delay, event -> {

            packetSpamCounter--;

            packetBufferMap.put(id, buffer);

            udpClient.send(buffer, serverPort, serverHost, sendAsyncResult -> {

                if (sendAsyncResult.succeeded()) {
                    sentPacketCounter++;
                    LOGGER.debug("Клиент -> Пакетов отправлено: " + sentPacketCounter);
                }
            });

            //отменяем периодическую отправку пакетов
            if (packetSpamCounter == 0) vertx.cancelTimer(event);
        });
    }


    private double getRandomDoubleInRange(double min, double max) {

        if (min >= max) { throw new IllegalArgumentException("максимум должен быть больше минимума"); }

        return min + new Random().nextDouble() * (max - min);
    }

    //TODO: сделать красивее
    // внутренний статический класс для выдачи уникальных чисел из очередей
    private static class UniqueNumberGenerator {

        static Queue<Integer> portStack = new PriorityQueue<>(1000);
        static Queue<Long> macStack = new PriorityQueue<>(1000);

        static {
            for (int i = 61000; i <= 62000; i++) {
                portStack.add(i);
            }

            for (long i = 7000000; i <= 7001000; i++) {
                macStack.add(i);
            }
        }

        static public int getPort() {

            if (portStack.peek() != null) {
                return portStack.poll();
            }
            return 0;
        }

        static public long getMac() {

            if (macStack.peek() != null) {
                return macStack.poll();
            }
            return 0;
        }
    }
}
