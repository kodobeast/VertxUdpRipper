import client.UdpClient;
import config.AppConfig;
import io.vertx.core.*;
import com.google.inject.Inject;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.UdpServer;

public class Main extends AbstractVerticle {

    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    /**
     * Основной метод, который запускает Vertx и развертывает лаунчер для объектов vertx
     *
     * @param args
     */
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        deploy(vertx, ServiceLauncher.class, new DeploymentOptions());
    }

    /**
     * Класс-лаунчер для сервера и клиента
     */
    private static class ServiceLauncher extends AbstractVerticle {

        private AppConfig appConfig;

        /**
         * "Впрыскиваем" класс-настройку для сервера и клиента
         *
         * @param appConfig - экземпляр класса AppConfig
         */
        @Inject
        public ServiceLauncher(AppConfig appConfig) {

            this.appConfig = appConfig;
        }

        /**
         * Метод использует CompositeFuture для развертывания сервера и клиента
         *
         * @param done
         */
        @Override
        public void start(Future<Void> done) {

            DeploymentOptions serverOpts = new DeploymentOptions()
                    .setWorkerPoolSize(appConfig.getServerThreads());

            DeploymentOptions workerOpts = new DeploymentOptions()
                    .setWorker(true)
                    .setWorkerPoolSize(1000)
                    .setInstances(appConfig.getWorkerThreads());

            CompositeFuture.all(
                    deploy(vertx, UdpServer.class, serverOpts),
                    deploy(vertx, UdpClient.class, workerOpts)
            ).setHandler(r -> {
                if (r.succeeded()) {
                    done.complete();
                } else {
                    done.fail(r.cause());
                }
            });
        }
    }

    /**
     * Метод развёртывания Verticle объектов с опциями
     *
     * @param vertx    - Vertx объект для развёртывания
     * @param verticle - Verticle класс для развёртывания
     * @param opts     - Опции для развёртывания
     * @return - Future может использоваться для обработки успешных или неудачных развертываний
     */
    private static Future<Void> deploy(Vertx vertx, Class verticle, DeploymentOptions opts) {
        Future<Void> done = Future.future();
        String deploymentName = "java-guice:" + verticle.getName();
        JsonObject config = new JsonObject()
                .put("guice_binder", ServiceBinder.class.getName());

        opts.setConfig(config);

        vertx.deployVerticle(deploymentName, opts, r -> {
            if (r.succeeded()) {
                LOGGER.info("Успешно проинициализирован verticle: " + deploymentName);
                done.complete();
            } else {
                LOGGER.info("Ошибка при инициализации verticle: " + deploymentName);
                done.fail(r.cause());
            }
        });

        return done;
    }
}