package db;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import common.LogUtil;
import commonCore.EnvConfig;
import server.EchoServer;;

public class MongoService {

    private static final MongoClient client;
    public static final MongoDatabase db;
    public static final MongoCollection<Document> UserCollection;

    static {
        client = MongoClients.create(EnvConfig.DB_URI_STRING);
        db = client.getDatabase(EnvConfig.DB_NAME);

        
        if (EchoServer.DEBUG_SERVER) LogUtil.log("✅ MongoDB Initialized.");


        // collections
        UserCollection = db.getCollection("Users");
        if (EchoServer.DEBUG_SERVER) LogUtil.log("Users collection initialized successfully.");
    }


    public static MongoDatabase getDb() {
        return db;
    }

    public static void closeClient() {
        client.close();
        if (EchoServer.DEBUG_SERVER) LogUtil.log("MongoDB closed gracefully.");
    }

    private MongoService() {};
}
