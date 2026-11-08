package ies.dam.servidor.models;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class Community extends Connectable{
    public Community(String _id, MongoCollection<Document> collection) {
        super(_id, collection);
    }

    @Override
    public void run() {

    }
}
