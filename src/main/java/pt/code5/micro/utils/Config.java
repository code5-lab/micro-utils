package pt.code5.micro.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.SharedData;
import pt.code5.micro.utils.vertx.VertxManager;

import java.util.Map;

/**
 * Created by eduardo on 17/03/2017.
 */
public class Config {

    private static Config instance = new Config();

    private Vertx vertx;
    private JsonObject localConfig = new JsonObject();

    private Config() {
        this.vertx = VertxManager.getInstance().getVertx();
    }

    public static Config getInstance() {
        return instance;
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
                readEnvConfigs();
            });
        } else {
            System.out.println("env_file not defined");
            // fallback if there is no config file
            readEnvConfigs();
        }
    }

    public void getConfigFromCluster(String key, Handler<AsyncResult<JsonObject>> resolve) {
        SharedData sd = vertx.sharedData();
        try {
            sd.<String, JsonObject>getClusterWideMap("config", res -> {
                if (res.succeeded()) {
                    AsyncMap<String, JsonObject> map = res.result();

                    map.get(key, resultingValue -> {
                        if (resultingValue.succeeded() && resultingValue.result() != null) {
                            resolve.handle(new ConfigResult(resultingValue.result()));
                        } else {
                            resolve.handle(new ConfigResult(Fail.SHARED_KEY_NOT_DEFINED));

                        }
                    });
                } else {
                    resolve.handle(new ConfigResult(Fail.SHARED_CONFIG_NOT_DEFINED));
                }
            });
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            resolve.handle(new ConfigResult(Fail.NO_CLUSTER));
        }
    }

    public void getConfigFromEnv(String key, Handler<AsyncResult<JsonObject>> resolve) {
        if (!this.localConfig.isEmpty()) {

            Object value = this.localConfig.getValue(key);
            if (value != null) {
                resolve.handle(new ConfigResult(value));
            } else {
                resolve.handle(new ConfigResult(Fail.LOCAL_KEY_NOT_DEFINED));
            }
        } else {
            resolve.handle(new ConfigResult(Fail.LOCAL_CONFIG_NOT_DEFINED));
        }
    }

    public void getConfig(String key, Handler<AsyncResult<JsonObject>> resolve) {
        this.getConfigFromEnv(key, event -> {
            if (event.succeeded()){
                resolve.handle(event);
            }
            else{
                this.getConfigFromCluster(key, event1 -> {
                    if (event1.succeeded())
                        resolve.handle(event1);
                    else
                        resolve.handle(new ConfigResult(Fail.KEY_NOT_DEFINED));
                });
            }
        });
    }

    public void readEnvConfigs() {
        Map<String, String> envs = System.getenv();
        for (String envVar : System.getenv().keySet()) {
            writeEnvConfig(localConfig, envVar, envs.get(envVar));
        }
    }

    private void writeEnvConfig(JsonObject in, String key, String value) {
        String[] keys = key.split("\\.");
        JsonObject obj = in;
        if (keys.length == 0) return;

        for (int i = 0; i < keys.length - 1; i++) {
            if (obj.getValue(keys[i]) != null) {
                obj = obj.getJsonObject(keys[i]);
            } else {
                obj.put(keys[i], new JsonObject());
                obj = obj.getJsonObject(keys[i]);
            }
        }
        obj.put(keys[keys.length - 1], value);
    }

    public enum Fail {
        NO_CLUSTER, SHARED_CONFIG_NOT_DEFINED, SHARED_KEY_NOT_DEFINED,
        LOCAL_CONFIG_NOT_DEFINED, LOCAL_KEY_NOT_DEFINED,
        KEY_NOT_DEFINED;

        public JsonObject toJson() {
            return (new JsonObject()).put("reason", this);
        }
    }

    public class ConfigResult implements AsyncResult<JsonObject> {

        private Throwable error;
        private JsonObject result;

        public ConfigResult(JsonObject result, Throwable error) {
            this.result = result;
            this.error = error;
        }

        public ConfigResult(Fail fail) {
            this.error = new Throwable(String.valueOf(fail));
        }

        public ConfigResult(Object result) {
            this.result = (new JsonObject()).put("result", result);
        }

        @Override
        public JsonObject result() {
            return this.result;
        }

        @Override
        public Throwable cause() {
            return error;
        }

        @Override
        public boolean succeeded() {
            return error == null;
        }

        @Override
        public boolean failed() {
            return error != null;
        }
    }
}
