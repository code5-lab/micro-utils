package pt.code5.micro.utils.vertx;

import io.vertx.core.Vertx;

/**
 * Created by eduardo on 20/03/2017.
 */
public class VertxManager {
    private static VertxManager ourInstance = new VertxManager();
    private Vertx vertx;

    private VertxManager() {
    }

    public static VertxManager getInstance() {
        return ourInstance;
    }

    public void boot(Vertx vertx) {
        this.vertx = vertx;
    }

    public Vertx getVertx() {
        return vertx;
    }
}
