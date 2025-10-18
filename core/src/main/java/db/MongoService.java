package db;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import common.LogUtil;
import commonCore.EnvConfig;;

public class MongoService {

    private static final MongoClient client;
    public static final MongoDatabase db;
    public static final MongoCollection<Document> UserCollection;

    static {
        client = MongoClients.create(EnvConfig.DB_URI_STRING);
        db = client.getDatabase(EnvConfig.DB_NAME);

        // collections
        UserCollection = db.getCollection("Users");

        LogUtil.log("✅ MongoDB Initialized.");
    }


    public static MongoDatabase getDb() {
        return db;
    }

    public static void closeClient() {
        client.close();
        LogUtil.log("MongoDB closed gracefully.");
    }

    private MongoService() {};
}
