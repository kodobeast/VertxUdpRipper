package dao;

import io.vertx.core.Vertx;

public class DaoFactory {

    public static IDao getDaoH2(Vertx vertx) {
        return new DaoH2(vertx);
    }
}
