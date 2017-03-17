package pt.code5.micro.utils.database;

import com.mongodb.MongoClient;
import io.vertx.core.json.JsonObject;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

/**
 * Created by eduardo on 17/03/2017.
 */
public class MorphiaManager {
    private static MorphiaManager ourInstance = new MorphiaManager();
    private Datastore dataStore;
    private Morphia morphia;
    private MongoClient mongoClient;

    private MorphiaManager() {
    }

    public static MorphiaManager getInstance() {
        return ourInstance;
    }

    public void boot(Package p, JsonObject config) {
        this.morphia = new Morphia();
        this.morphia.mapPackage(p.getName());

        this.mongoClient = new MongoClient(config.getString("host"), config.getInteger("port"));
        dataStore = morphia.createDatastore(mongoClient, config.getString("database"));
    }

    public Datastore getDataStore() {
        return dataStore;
    }
}
