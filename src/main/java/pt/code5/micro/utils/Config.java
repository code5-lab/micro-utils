package pt.code5.micro.utils;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.SharedData;
import pt.code5.micro.utils.vertx.VertxManager;

/**
 * Created by eduardo on 17/03/2017.
 */
public class Config { //todo: replace with http://vertx.io/docs/vertx-config/java/
    private static Config ourInstance = new Config();
    private Vertx vertx;
    private JsonObject localConfig = new JsonObject();

    private Config() {
        this.vertx = VertxManager.getInstance().getVertx();
    }

    public static Config getInstance() {
        return ourInstance;
    }

    public void boot(Handler<Boolean> ready) {

        String envFilePath = System.getenv("env_file");
        if (envFilePath != null) {
            this.vertx.fileSystem().readFile(envFilePath, response -> {
                if (response.succeeded()) {
                    this.localConfig = new JsonObject(response.result().toString());
                } else {
                    try {
                        throw response.cause();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
                ready.handle(true);
            });
        }
    }

    public void getConfigFromCluster(String key, Handler<JsonObject> success, Handler<JsonObject> error) {
        SharedData sd = vertx.sharedData();
        try {
            sd.<String, JsonObject>getClusterWideMap("config", res -> {
                if (res.succeeded()) {
                    AsyncMap<String, JsonObject> map = res.result();

                    map.get(key, resultingKey -> {
                        if (resultingKey.succeeded() && resultingKey.result() != null) {
                            success.handle((new JsonObject()).put("result", resultingKey.result()));
                        } else {
                            error.handle(Fail.SHARED_KEY_NOT_DEFINED.toJson());
                        }
                    });

                } else {
                    error.handle(Fail.SHARED_CONFIG_NOT_DEFINED.toJson());
                }
            });
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            error.handle(Fail.NO_CLUSTER.toJson());
        }
    }

    public void getConfigFromEnv(String key, Handler<JsonObject> success, Handler<JsonObject> error) {
        if (!this.localConfig.isEmpty()) {

            Object value = this.localConfig.getValue(key);
            if (value != null) {
                success.handle((new JsonObject()).put("result", value));
            } else {
                error.handle(Fail.LOCAL_KEY_NOT_DEFINED.toJson());
            }
        } else {
            error.handle(Fail.LOCAL_CONFIG_NOT_DEFINED.toJson());
        }
    }

    public void getConfig(String key, Handler<JsonObject> success, Handler<JsonObject> error) {
        this.getConfigFromCluster(key, success, event -> {
            this.getConfigFromEnv(key, success, event1 -> {
                error.handle(Fail.KEY_NOT_DEFINED.toJson());
            });
        });
    }

    public enum Fail {
        NO_CLUSTER, SHARED_CONFIG_NOT_DEFINED, SHARED_KEY_NOT_DEFINED,
        LOCAL_CONFIG_NOT_DEFINED, LOCAL_KEY_NOT_DEFINED,
        KEY_NOT_DEFINED;

        public JsonObject toJson() {
            return (new JsonObject()).put("reason", this);
        }
    }
}
