package ies.dam.servidor.models;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import ies.dam.servidor.RecSignals;
import ies.dam.servidor.SenSignals;
import ies.dam.servidor.Server;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Conversation MVC class for Chatti Server.
 * @version 1.0
 * */
public class Convo extends Connectable{
    private int maxFileSize;
    private int lastLoadedMsg;
    private final List<String> clientIDList;
    protected MongoCollection<Document> messages;

    /**
     * Constructor for random conversations that won't be saved in any collection.
     * @param clientIDList ID of clients to partake in an anonymous conversation.
     * */
    public Convo(List<String> clientIDList){
        super();
        this.clientIDList = clientIDList;
        System.out.printf("{%s, %s}%n", clientIDList.getFirst(), clientIDList.getLast());
    }
    /**
     * Constructor called when a new conversation is being initialized.
     * @param typeCollection Inherited type collection, could be any collection.
     * @param messages Collection that stores messages.
     * @param clientIDList Client map passed by the server.
     * */
    public Convo(MongoCollection<Document> typeCollection,
                 MongoCollection<Document> messages,
                 List<String> clientIDList) {
        super(typeCollection);
        InsertOneResult idFetcher = typeCollection.insertOne(
                new Document()
                        .append("conv_usr_ids", new ArrayList<String>())
                        .append("msg_ids", new ArrayList<String>())
        );
        this._id = idFetcher.getInsertedId().asObjectId().getValue().toHexString();
        System.out.println("Convo added: " + _id);
        this.clientIDList = clientIDList;
        clientIDList.forEach(this::addClientToCollection);
        ArrayList<String> b = new ArrayList<>();
        for (String id : clientIDList){
            String acn = Server.users.find(Filters.eq("_id", new ObjectId(id)))
                    .projection(Projections.include("acc_name")).first().getString("acc_name");
            b.add(acn);
        }
        Collections.sort(b);
        super.saveObject(String.format("%s@%s", b.getFirst(), b.getLast()));

        this.messages = messages;
        maxFileSize = 10;
    }

    /**
     * If server finds an object that indicates the existence of a conversation between n users, that object
     * stores a reference to the mongo id of that conversation, and passes it on onto this constructor.
     * @param _id _id stored by an object in our server's storage.
     * @param typeCollection Inherited type collection, could be any collection
     * @param messages Collection that stores messages
     * */
    public Convo(MongoCollection<Document> typeCollection,
                 MongoCollection<Document> messages,
                 String _id) {
        super(typeCollection);
        this._id = _id;
        // This is going to stay unchecked because it's a fucking pain in the ass
        ArrayList<String> e = (ArrayList<String>) typeCollection
                .find(Filters.eq("_id", new ObjectId(_id)))
                .first().get("conv_usr_ids");
        this.clientIDList = new ArrayList<>();
        e.forEach((s)->{
            String key = s;
            this.clientIDList.add(key);
        });
        this.messages = messages;
        maxFileSize = 10;
    }

    /**
     * Adds a client into our clientIDList.
     * @param id Client to be added.
     * */
    public synchronized void addClient(String id) {
        clientIDList.add(id);
    }

    /**
     * Adds a client into the conversation collection for our database.
     * @param id Client to be added.
     * */
    public void addClientToCollection(String id){
        // Iterate through type collection and add conv_usr_ids
        typeCollection.updateOne(
                Filters.eq("_id", new ObjectId(_id)),
                Updates.addToSet("conv_usr_ids", id)
        );
    }

    /**
     * Removes a client from the conversation clientIDList.
     * @param client Client to remove.
     * */
    public synchronized void removeClient(Socket client) {
        clientIDList.remove(client);
    }

    /**
     * This method takes our conversation id and fetches all it's existing messages. Accompanied by a
     * separator with the user who sent the message.
     * @return A stack of messages to be retrieved by the client.
     * */
    public Queue<String> retrieveMessages(){
        Queue<String> msg_list_2 = new LinkedList<>();
        //Stack<String> msg_list = new Stack<>();
        ArrayList<String> messages_ids =
                (ArrayList<String>) typeCollection
                .find(Filters.eq("_id", new ObjectId(_id))).first().get("msg_ids");
        messages_ids.forEach(System.out::println);
        this.lastLoadedMsg = messages_ids.size() - 1;
        // Index is just meant to establish a limit of messages we can load, then update the lastLoadedMsg var
        // Yet to implement.Shouldn't heavily impact performance if not done... unless you're running a Celeron
        int i = lastLoadedMsg;
        for (String message_id : messages_ids){
            Document doc = this.messages.find(Filters.eq("_id", new ObjectId(message_id))).first();
            String message = doc.getString("msg");
            String user_name =
                    Server.users.find(Filters.eq("_id", new ObjectId(doc.getString("user_id"))))
                                .first().getString("user_name");
            //msg_list.push(String.format("%s/%s", user_name, message));
            msg_list_2.offer(String.format("%s/%s", user_name, message));
        }
        return msg_list_2;
    }
    /**
     * A method in charge of message diffusion.
     * @param message Message to diffuse.
     * */
    public void serveMsg(DataOutputStream clientOutputStream, String message){
        try {
            clientOutputStream.writeByte(SenSignals.SEND_MESSAGE.getSignal());
            clientOutputStream.flush();
            clientOutputStream.writeUTF(message);
            clientOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * At thread, it will be used in conjunction with the serveMsg procedure, receiving
     * input that gets spread to the corresponding users and stored in the database.
     * @param msg Text message sent by the user
     * @param emb_files If not empty, files with arbitrary type.
     * */
    public void storeMessage(String msg, Map<Byte[], String> emb_files, String user_id){
        //Creates a result to create the message registry
        InsertOneResult result = messages.insertOne(
                new Document()
                        .append("user_id", user_id)
                        .append("msg", msg)
                        .append("emb_files", new Document())
        );
        //Manages any embed files (At this point it's going to end up unused)
        String objectID = result.getInsertedId().asObjectId().getValue().toHexString();
        Map<String, String> paths = new HashMap<>();
        emb_files.forEach((key, value) -> { //Creates the files for the embed files?
            try {
                String path = String.format("cache/emb_files/%s.%s", objectID, value);
                paths.put(path, value);
                File embed = new File(path);
                FileUtils.writeByteArrayToFile(embed, ArrayUtils.toPrimitive(key));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        MongoCursor<Document> update = messages.find(
                Filters.eq("_id", new ObjectId(objectID))
        ).iterator();
        update.forEachRemaining(document -> {
            document.remove("emb_files");
            Document fileNameDoc = new Document();
            paths.forEach((key, value) -> {
                switch (value) {
                    case "jpeg", "png", "jpg" -> {
                        fileNameDoc.append(key, "img");
                    }
                    case "bmp", "gif" -> {
                        fileNameDoc.append(key, "gif");
                    }
                    case "mp4", "mp3", "avi" -> {
                        fileNameDoc.append(key, "media");
                    }
                    default -> {
                        fileNameDoc.append(key, "unknown");
                    }
                }
            });
            document.append("emb_files", fileNameDoc);
        });
        typeCollection.updateOne(
                Filters.eq("_id", new ObjectId(_id)),
                Updates.addToSet("msg_ids", objectID)
        );
        update.close();
    }

    public void deleteMessage(){

    }

    public boolean handleConvoExit(){
        return true;
    }

    private String encryptMessage(String message){
        return "";
    }

    /**
     * It's going to read user messages and diffuse it to the rest of the sockets. Sockets are dynamic (they
     * can change in every iteration). It will also read signals, and return them into the server if needed.
     * @param client Client socket for local Server thread.
     * @param id Client id for ease of querying.
     * @return The signal the client might input, if needed.
     * */
    public byte expectMessage(Socket client, String id){
        try {
            //Starts the connection and checks if the other user is connected?
            Socket socket = client;
            Map<String, DataOutputStream> fr_dOss = new HashMap<>();
            checkUsrConnectivity(fr_dOss, id);
            Gson gson = new Gson();
            DataInputStream clientInputStream = new DataInputStream(socket.getInputStream());
            //Gets the database info?
            MongoCursor<Document> cursor = Server.users.find(Filters.eq("_id", new ObjectId(id))).iterator();
            String user_name = cursor.next().getString("user_name");

            byte controlByte;
            while (true) { //Loops constantly until the client exits the view or changes chat
                controlByte = clientInputStream.readByte();
                if (controlByte == RecSignals.MESSAGE_SENT.getSignal()){ //Triggers when a message is sent
                    //Gets the message
                    System.out.println("Received byte 99.");
                    String message = clientInputStream.readUTF();
                    String userAndMsg = message.concat(String.format("/%s", user_name));
                    Map<Byte[], String> emb_files = gson.fromJson(clientInputStream.readUTF(), Map.class);
                    System.out.printf("%s says: %s\n", user_name, userAndMsg);
                    storeMessage(message, emb_files, id); //Stores the message
                    checkUsrConnectivity(fr_dOss, id); //Checks for the connexion of the users
                    fr_dOss.forEach((fr_id, fr_dOs)-> {
                        serveMsg(fr_dOs, userAndMsg); //Sends the messages
                    });
                } else if (controlByte == RecSignals.EXIT_VIEW.getSignal()) { //The client exits the whole scene
                    break;
                } else if (controlByte == RecSignals.USER_SWITCH.getSignal()){ //The client switches chat
                    return controlByte;
                } else if (controlByte == RecSignals.DELETE_CONTACT.getSignal()) { //The client deletes the contact
                    Server.deleteFriends(clientIDList.get(0), clientIDList.get(1));
                    return controlByte;
                }
            }
            removeClient(socket); //Removes the client from the list
        } catch (IOException e) {
            System.err.printf("User %s disconnected prematurely.%n", id);
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.printf("User %s socket is not open %n", id);
            e.printStackTrace();
        }
        return -2;
    }

    /**
     * Modified version of the expectMessage method. It does no operations on the database for
     * complete anonymity.
     * @param client Client socket for local Server thread.
     * @param id Client id for ease of querying.
     * @return The signal the client might input, if needed.
     * */
    public byte expectAnonMsgs(Socket client, String id){
        try {
            //Starts the connection and checks if the other user is connected?
            Socket socket = client;
            Map<String, DataOutputStream> fr_dOss = new HashMap<>();
            checkUsrConnectivity(fr_dOss, id);
            DataInputStream clientInputStream = new DataInputStream(socket.getInputStream());
            fr_dOss.forEach((fr_id, fr_dOs)-> serveMsg(fr_dOs, "Has sido emparejado con otro usuario".concat("/Sistema"))); //Line added to warn users that the other has left the chat. Erase if it explodes
            byte controlByte;
            while (true) { //Loops until the client leaves the whole scene
                controlByte = clientInputStream.readByte();
                if (controlByte == RecSignals.MESSAGE_SENT.getSignal()){ //Triggers if a message has been sent
                    System.out.println("Received byte 99.");
                    String message = clientInputStream.readUTF();
                    System.out.printf("Anon says: %s\n", message);
                    checkUsrConnectivity(fr_dOss, id);
                    if (!usrAvailable()){ //Triggers if the other user ahs left the conversation
                        System.out.printf("User %s was forced into NEXT_RAND.%n", id);
                        fr_dOss.forEach((fr_id, fr_dOs)-> serveMsg(fr_dOs, "El otro usuario ha salido de la conversación".concat("/Sistema"))); //Line added to warn users that the other has left the chat. Erase if it explodes
                        return RecSignals.NEXT_RAND.getSignal();
                    }
                    fr_dOss.forEach((fr_id, fr_dOs)-> serveMsg(fr_dOs, message.concat("/Anon")));
                } else if (controlByte == RecSignals.EXIT_VIEW.getSignal()) { //Triggers if the client leaves the whole scene
                    System.out.printf("User %s exited the view.%n", id);
                    fr_dOss.forEach((fr_id, fr_dOs)-> serveMsg(fr_dOs, "El otro usuario ha salido de la conversación".concat("/Sistema"))); //Line added to warn the users that the other has left the chat. Erase if it explodes
                    break;
                } else if (controlByte == RecSignals.ADD_RAND.getSignal()){  //Triggers if the client sends a friend request to the other
                    System.out.printf("User %s added a random user.%n", id);
                    Server.sendFrReq(new DataOutputStream(client.getOutputStream()), id, clientIDList.getLast());
                    fr_dOss.forEach((fr_id, fr_dOs)-> serveMsg(fr_dOs, "El otro usuario le ha enviado una solicitud".concat("/Sistema"))); //Line added to warn the users that the other has sent a request. Erase if it explodes
                    return controlByte;
                } else if (controlByte == RecSignals.STOP_RAND.getSignal()){ //Triggers if the client stopped chatting
                    System.out.printf("User %s stopped a random conversation.%n", id);
                    fr_dOss.forEach((fr_id, fr_dOs)-> serveMsg(fr_dOs, "El otro usuario ha salido de la conversación".concat("/Sistema"))); //Line added to warn users that the other has left the chat. Erase if it explodes
                    return controlByte;
                } else if (controlByte == RecSignals.START_RAND.getSignal()){ //Triggers if the client started a random chat
                    System.out.printf("User %s started a random conversation.%n", id);
                    return controlByte;
                } else if (controlByte == RecSignals.NEXT_RAND.getSignal()){ //Triggers if the client skipped a random chat
                    System.out.printf("User %s skipped.%n", id);
                    fr_dOss.forEach((fr_id, fr_dOs)-> serveMsg(fr_dOs, "El otro usuario ha salido de la conversación".concat("/Sistema"))); //Line added to warn users that the other has left the chat. Erase if it explodes
                    return controlByte;
                }
            }
            removeClient(socket); //Removes the client from the list of clients
        } catch (IOException e) {
            System.err.printf("User %s disconnected prematurely.%n", id);
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.printf("User %s socket is not open %n", id);
            e.printStackTrace();
        }
        return -2;
    }

    /**
     * Observes Server.java clientMap to see if any client has made or stopped a connection.
     * @param fr_dOss Convo.java field to operate on by reference.
     * @param user_id Current user id, in order to iterate over it.
     * */
    private void checkUsrConnectivity(Map<String, DataOutputStream> fr_dOss, String user_id) {
        try {
            for (String fr_id : clientIDList) {
                if (Server.clientMap.containsKey(fr_id) && !fr_id.equals(user_id)) {
                    System.out.printf("User %s found.%n", fr_id);
                    fr_dOss.put(fr_id, new DataOutputStream(Server.clientMap.get(fr_id).getOutputStream()));
                } else {
                    System.out.printf("User %s not found.%n", fr_id);
                    fr_dOss.remove(fr_id);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean usrAvailable() {
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        Server.activeMatches.forEach((key, value) -> {
            if (key.equals(clientIDList.get(1)) && value.equals(clientIDList.get(0))) {
                result.set(true);
            }
        });
        return result.get();
    }

    @Override
    public void run() {

    }
}
