package ies.dam.servidor.models;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class Group extends ExtConvo{

    public Group(MongoCollection<Document> typeCollection, MongoCollection<Document> messages, List<String> clientMap) {
        super(typeCollection, messages, clientMap);
    }

    public Group(MongoCollection<Document> typeCollection, MongoCollection<Document> messages, String _id) {
        super(typeCollection, messages, _id);
    }
}
