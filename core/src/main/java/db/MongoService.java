package db;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import common.LogUtil;
import server.EchoServer;

public class MongoService {

    private final MongoClient client;
    private final MongoDatabase db;
    private final MongoCollection<Document> userCollection;

    private final FindIterable<Document> allUserDocs;

    public MongoService(String uri, String dbName) {
        System.out.println("Debug " + uri + " " + dbName);
        this.client = MongoClients.create(uri);
        this.db = client.getDatabase(dbName);
        if (EchoServer.DEBUG_SERVER) LogUtil.log("✅ MongoDB Initialized.");


        this.userCollection = db.getCollection("Users");
        
        allUserDocs = userCollection.find();

        if (EchoServer.DEBUG_SERVER) LogUtil.log("Users collection initialized successfully.");
    }

    public FindIterable<Document> getAllFromUserCollection() {
        return allUserDocs;
    }

    public void shutdown() {
        client.close();
        if (EchoServer.DEBUG_SERVER) LogUtil.log("✅ MongoDB closed gracefully.");
    }
}