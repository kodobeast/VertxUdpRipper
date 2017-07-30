package server;

import dao.DaoFactory;
import dto.GeoData;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaverWorkerThread implements Runnable {

    private static Logger LOGGER = LoggerFactory.getLogger(SaverWorkerThread.class);

    public void run() {
        LOGGER.info(" Стартовал");
        while (!UdpServer.packetQueue.isEmpty()) {
            GeoData geoData = getGeodata();
            if (geoData != null) {
                saveGeodata(Vertx.vertx(), geoData);
            }
        }
    }

    private void saveGeodata(Vertx vertx, GeoData geoData) {
        DaoFactory.getDaoH2(vertx).setGeoData(geoData);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private synchronized GeoData getGeodata() {
        if (!UdpServer.packetQueue.isEmpty() && UdpServer.packetQueue.peek() != null) {
            return UdpServer.packetQueue.remove();
        }
        return null;
    }
}