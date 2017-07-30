package server;

import dto.GeoData;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UdpServer extends AbstractVerticle {

    // количество потоков для реализации записи очереди пакетов в БД
    private static int workersPool = 30;

    //TODO: выбрать лучшую очередь
    public static Queue<GeoData> packetQueue = new ArrayDeque<>();

    private boolean needRunWorkers = true;

    private static Logger LOGGER = LoggerFactory.getLogger(UdpServer.class);

    @Override
    public void start() throws Exception {
        super.start();

        // запускаем сервер для ожидания UDP пакетов от клиентов
        DatagramSocket udpServer = vertx.createDatagramSocket(new DatagramSocketOptions());

        udpServer.listen(8181, "localhost", listenAsyncResult -> {
            if (listenAsyncResult.succeeded()) {
                udpServer.handler(packet -> {
                    long mac = packet.data().getLong(0); // int8
                    byte pt = packet.data().getByte(8); // int2
                    int id = packet.data().getInt(9); // int4
                    long time = packet.data().getLong(13); // int8
                    double latitude = packet.data().getDouble(21); // float8
                    double longitude = packet.data().getDouble(29); // float8
                    int clientPort = packet.data().getInt(37); // int4

                    // формируем объект данных
                    GeoData geoData = new GeoData(mac, pt, id, time, latitude, longitude, clientPort);

                    // помещаем данные в стэк для буфера записи в БД
                    synchronized (packetQueue) {
                        packetQueue.add(geoData);
                    }

                    LOGGER.info("Сервер -> Пакетов записано в очередь: " + packetQueue.size());

                    // отправка пакета-подтверждения назад клиенту
                    byte confirmationPT = 2;
                    Buffer buffer = Buffer.buffer(13);
                    buffer.appendLong(mac).appendByte(confirmationPT).appendInt(id);

                    udpServer.send(buffer, clientPort, "localhost", sendAsyncResult -> {
                        if (sendAsyncResult.succeeded()) {
                            LOGGER.debug("Сервер -> пакет-подтверждение отправлен: mac [" + mac + "] pt [" +
                                    confirmationPT + "] id [" + id + "] на порт: " + clientPort);
                        }
                    });

                    //TODO: вынести место запуска потоков за пределы start
                    // запуск потоков для записи пакетов из очереди в БД
                    if (needRunWorkers) {
                        doWorkers();
                        needRunWorkers = false;
                    }
                });
            } else {
                LOGGER.error("Сервер: ошибка получения пакета ", listenAsyncResult.cause());
            }
        });
    }

    private void doWorkers() {

        ExecutorService executor = Executors.newFixedThreadPool(workersPool + 5);

        Runnable runnable = () -> {
            // TODO: избавиться от сна
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LOGGER.info("Поток для пула рабочих стартовал ");
            for (int i = 0; i < workersPool; i++) {
                Runnable worker = new SaverWorkerThread();
                executor.execute(worker);
            }

            // ждём пока вся очередь пакетов запишется в БД
            while (!packetQueue.isEmpty()) {
            }

            try {
                LOGGER.info("Сервер -> Попытка остановки пула потоков");
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException cause) {
                LOGGER.error("Сервер -> Задачи прерваны", cause);
            } finally {
                if (!executor.isTerminated()) {
                    LOGGER.info("Сервер -> Отменить незавершенные задачи");
                }
                executor.shutdownNow();
                LOGGER.info("Сервер -> Остановка завершена");
                System.exit(0);
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }
}
