package dao;

import dto.GeoData;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.SQLRowStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.UdpServer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DaoH2 implements IDao {

    private static Logger LOGGER = LoggerFactory.getLogger(DaoH2.class);

    private static final String driverClass = "org.h2.Driver";
    private static final String url = "jdbc:h2:~/JavaProjects/udpripper/db/geodb;MODE=PostgreSQL;TRACE_LEVEL_SYSTEM_OUT=1";
    private static final String userName = "sa";
    private static final String password = "";
    private static final int minPoolSize = 20;
    private static final int maxPoolSize = 100;
    private static final int initialPoolSize = 30;

    static JDBCClient client;

    private static long savedPacketCounter = 0;

    public DaoH2 (Vertx vertx) {

        client = JDBCClient.createShared(vertx, new JsonObject()
                .put("driver_class", driverClass)
                .put("url", url)
                .put("user", userName)
                .put("password", password)
                .put("initial_pool_size", initialPoolSize)
                .put("min_pool_size", minPoolSize)
                .put("max_pool_size", maxPoolSize));
    }

    public void setGeoData(GeoData geoData) {

        client.getConnection(conn -> {
            if (conn.failed()) {
                System.err.println(conn.cause().getMessage());
                return;
            }
            SQLConnection connection = conn.result();

            connection.execute("CREATE TABLE IF NOT EXISTS geodata " +
                            "(MAC int8," + "PT int2," + "ID int4," + "TIME int8," + "LAT float8," + "LON float8,"
                            + "PORT int4);",
                    res -> {
                        if (res.failed()) {
                            throw new RuntimeException(res.cause());
                        }
                        // insert some test data
                        connection.execute("INSERT INTO geodata VALUES (('" + geoData.getMac() + "'), " +
                                        "('" + geoData.getPt() + "'), ('" + geoData.getId() + "'), ('" +
                                        geoData.getTime() + "'), " + "('" + geoData.getLatitude() +
                                        "'), ('" + geoData.getLongitude() + "'), ('" + geoData.getClientPort() + "'));",

                                insert -> {
                                    if (insert.succeeded()) {
                                        ++savedPacketCounter;
                                        LOGGER.info("DaoH2 -> Пакетов записано: " + savedPacketCounter);
                                    }

                                    // выборка данных чтобы посмотреть на сделанную запись в БД

//                                    connection.queryStream("SELECT * FROM geodata WHERE LON='"
//                                            + geoData.getLongitude() + "';",
//                                            stream -> {
//
//                                        if (stream.succeeded()) {
//
//                                            SQLRowStream sqlRowStream = stream.result();
//
//                                            sqlRowStream
//                                                    .handler(row -> {
//                                                        System.out.println("БД: строка добавлена [" + row.encode() + "]");
//                                                    })
//                                                    .endHandler(v -> {
//                                                        connection.close(done -> {
//                                                            if (done.succeeded()) {
//                                                                System.out.println("БД: соединение закрыто");
//                                                            } else {
//                                                                throw new RuntimeException(done.cause());
//                                                            }
//                                                        });
//                                                    });
//                                        }
//                                    });
                                });
                    });
        });
    }

}
