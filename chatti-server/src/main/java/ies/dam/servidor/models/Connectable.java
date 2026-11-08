package ies.dam.servidor.models;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.io.*;

/**
 * Abstract class that shapes the expected behavior for conversations. It is called
 * Connectable because our conversations are meant to be inserting and retrieving
 * messages from a database, from which they have to be connected through a server.
 * @version 1.0
 * */
public abstract class Connectable implements Runnable {
    protected String _id;
    // Named collection, because it could be either a thread, a group or  a conversation
    protected MongoCollection<Document> typeCollection;

    /**
     * Connectable constructor for newly created conversations. If it's
     * the first time the user engages in a conversation, a new conversation
     * should be created in the database.
     * @param typeCollection Message collection.
     * */
    public Connectable(MongoCollection<Document> typeCollection) {
        this.typeCollection = typeCollection;
    }

    /**
     * Connectable constructor for conversations that already exist, if conversation
     * is stored in cache, it's name is the id of the conversation. Hence, we know
     * what to fetch.
     * @param _id Conversation (regardless of type) id.
     * @param typeCollection Message collection
     * */
    public Connectable(String _id, MongoCollection<Document> typeCollection) {
        this._id = _id;
        this.typeCollection = typeCollection;
    }

    public Connectable() {

    }

    /**
     * TODO: Mongo Collections are not serializable
     * As described previously, our conversation objects are to be stored in a
     * cache, for ease of retrieval.
     * <p>
     * We could overwrite this in child classes if needed.
     * */
    protected void saveObject(String pair){
        try (FileOutputStream fos = new FileOutputStream("cache/" + (pair.isEmpty() ? _id : pair))){
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(new Test(_id));
            oos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
