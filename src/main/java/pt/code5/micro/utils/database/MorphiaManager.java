package pt.code5.micro.utils.database;

import com.mongodb.MongoClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import pt.code5.micro.utils.Config;
import pt.code5.micro.utils.vertx.VertxManager;

public class MorphiaManager {
    private static MorphiaManager ourInstance = new MorphiaManager();
    private final Vertx vertx;
    private Datastore dataStore;
    private Morphia morphia;
    private MongoClient mongoClient;

    private MorphiaManager() {
        this.vertx = VertxManager.getInstance().getVertx();
    }

    public static MorphiaManager getInstance() {
        return ourInstance;
    }

    public void boot(Package p, String configKey, Handler<AsyncResult<Boolean>> onComplete) {
        this.morphia = new Morphia();
        this.morphia.mapPackage(p.getName());
        Config.getInstance().getConfig(configKey, config -> {
            if(config.failed()){
                System.err.println(configKey + "::" + config.cause());
                System.exit(-1);
            }
            JsonObject result = config.result().getJsonObject("result");
            this.vertx.executeBlocking(future -> {

                this.mongoClient = new MongoClient(result.getString("host"), result.getInteger("port"));
                this.dataStore = morphia.createDatastore(mongoClient, result.getString("database"));

                future.complete(true);
            }, onComplete);
        });

    }

    public Datastore getDataStore() {
        return dataStore;
    }
}
