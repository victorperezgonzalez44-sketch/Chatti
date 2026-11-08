package ies.dam.servidor;

import com.google.gson.Gson;
import com.mongodb.MongoWriteException;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import ies.dam.servidor.models.Convo;
import ies.dam.servidor.models.EmailUtil;
import ies.dam.servidor.models.Test;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Server class for Chatti Application. It makes uses of various models
 * included in ies.dam.servidor.models.

 * @version 1.0
 * */
public class Server {
    private static ServerSocket serverSocket;
    public static Map<String, Socket> clientMap = new HashMap<>();
    public static Map<String, String> activeMatches = new ConcurrentHashMap<>();
    private static final Set<String> randomList = ConcurrentHashMap.newKeySet();

    private static MongoClient mongoClient;
    public static MongoCollection<Document> users;
    public static MongoCollection<Document> convos;
    public static MongoCollection<Document> messages;
    private static Logger logger;
    private static final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {
        System.out.print("Load default database? (Y)es or (N)o: ");
        Scanner scanner = new Scanner(System.in);
        String response = scanner.nextLine().toUpperCase();
        String conSTring = response.startsWith("Y") ? "mongodb+srv://victorperezgonzalez44:1234@clusterchatti.alymu.mongodb.net/?retryWrites=true&w=majority" : "";
        if (response.startsWith("N")){
            System.out.println("""
                    You will need to configure your IP in the access list for
                    an atlas database, then input your connection string with your
                    given credentials.
                    More information here: https://www.mongodb.com/docs/atlas/security/ip-access-list
                    """);
            System.out.print("Please input your connection string: ");
            conSTring = scanner.nextLine();
            if (!conSTring.contains("mongodb+srv://")){
                System.out.println("Connection string invalid. Ending server boot-up.");
                return;
            } else if (conSTring.matches("mongodb\\+srv://[A-Za-z0-9]*:<[A-Za-z0-9]*>.*")){
                System.out.println("""
                        Please remove the leading and trailing arrows from the password field and
                        input the password. Ending server boot-up.""");
                return;
            }
            System.out.println("Connection string valid.");
        }
        //Prepares the connection
        System.out.println("Initializing connection...");
        logger = LoggerFactory.getLogger("ChattiLogger");
        mongoClient = MongoClients.create(conSTring);
        //Receives the database
        System.out.println("Connecting to database...");
        MongoDatabase mongoDatabase = mongoClient.getDatabase("chatti");
        //Separates the collections of the database
        users = mongoDatabase.getCollection("users");
        convos = mongoDatabase.getCollection("convos");
        messages = mongoDatabase.getCollection("messages");

        System.out.println("Server started");
        startServer();
    }

    /**
     * Server procedure that listens for connections in socket.
     * */
    private static void startServer() {
        try {
            serverSocket = new ServerSocket(1234);
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("New client connected");
                new Thread(() -> serveClient(client)).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Server procedure in charge of reading and serving petitions from a client.
     * It holds the client streams and reads signals sent by the client (petitions).
     * @param client Client to be served
     */
    private static void serveClient(Socket client){
        boolean connected = true;
        String _id = "";
        try {
            //Creating input and output streams
            DataInputStream clientInputStream = new DataInputStream(client.getInputStream());
            DataOutputStream clientOutputStream = new DataOutputStream(client.getOutputStream());
            while (connected) {
                // Then waits for client requests (???)

                // Then reads and serves
                byte code = clientInputStream.readByte();
                System.out.println("Received byte: " + code);
                switch (code) {
                    case -69 ->{ //Renzo specifically added for this byte
                        System.out.println("Received heartbeat (byte -69)");
                        clientOutputStream.writeUTF("Latido");
                    }
                    case -1 -> { //Client disconnects
                        System.out.println("Client disconnected");
                        closeSocket(client, _id);
                        connected = false;
                    }
                    case 0 -> { //Client is trying to log in into an account
                        _id = handleLogin(clientInputStream, clientOutputStream, client);
                        switch (_id){
                            case "UN_EXIT" -> {
                                System.out.println("Client disconnected (IN CASE 0)");
                                closeSocket(client, _id);
                                connected = false;
                            }
                            case "EXIT" -> {
                                System.out.println("Client exits view and doesn't create a new socket!");
                            }
                        }
                        System.out.println("Succesfully logged in");
                    }
                    case 1 -> { //Client is trying to register a new account
                        byte result;
                        result = handleNewRegistry(clientInputStream, clientOutputStream);
                        if (result == -1) { //Client disconnects
                            System.out.println("Client disconnected (IN CASE 1)");
                            closeSocket(client, _id);
                            connected = false;
                        } else if (result == RecSignals.EXIT_VIEW.getSignal()){ //Client cancels registration
                            System.out.println("Client exits view and doesn't create a new socket!");
                        }
                    }
                    case 2 -> { //Client is in a one to one chat
                        //Filling the convoList
                        AtomicReference<ArrayList<String>> list = new AtomicReference<>();
                        AtomicReference<String> acc_name = new AtomicReference<>();
                        populateLists(_id, list, acc_name, "u_friend_list_ids");

                        // This is something that the user should receive everytime they load the chat-view.fxml
                        Gson gson = new Gson();

                        //Sending the convoList
                        String friendList = gson.toJson(getUserFList(list), Map.class);
                        clientOutputStream.writeUTF(friendList);
                        clientOutputStream.flush();

                        //Obtaining their pfps and sending them
                        String pfpList = gson.toJson(getUsersPfp(list), Map.class);
                        byte[] jsonBytes = pfpList.getBytes(StandardCharsets.UTF_8);
                        clientOutputStream.writeInt(jsonBytes.length);
                        clientOutputStream.flush();
                        clientOutputStream.write(jsonBytes);
                        clientOutputStream.flush();

                        //Receives a byte from the client that indicates the instructions
                        byte receivedByte = clientInputStream.readByte();
                        System.out.println("Read byte : " + receivedByte);
                        System.out.printf("Is received byte equal to exit byte?: %s%n", receivedByte == RecSignals.EXIT_VIEW.getSignal());
                        while (receivedByte != RecSignals.EXIT_VIEW.getSignal()){ //Loops while it doesn't receive the exit byte (-2)
                            if (receivedByte == RecSignals.USER_SWITCH.getSignal()) { //This triggers when the client changes convo
                                String friend_acc_name = clientInputStream.readUTF(); //Gets the name of the friend
                                ArrayList<String> names = new ArrayList<>(List.of(acc_name.get(), friend_acc_name));
                                Collections.sort(names);
                                String friend_user_id = //gets the id of the friend
                                        users.find(Filters.eq("acc_name", friend_acc_name))
                                                .first().getObjectId("_id").toString();
                                String fName = String.format("cache/%s@%s", names.getFirst(), names.getLast());
                                System.out.printf("Name: %s // ID: %s\n", friend_acc_name, friend_user_id);
                                Convo convo;
                                if (Files.exists(Path.of(fName))) {
                                    try (FileInputStream fIs = new FileInputStream(fName)) {
                                        ObjectInputStream oIs = new ObjectInputStream(fIs);
                                        Test test = (Test) oIs.readObject();
                                        convo = new Convo(convos, messages, test.getId());
                                    } catch (ClassNotFoundException e) {
                                        throw new RuntimeException(e);
                                    }
                                } else {
                                    List<String> clientIDList = new ArrayList<>(List.of(_id, friend_user_id));
                                    convo = new Convo(convos, messages, clientIDList);
                                }
                                //Sends the data related to the messages to the client
                                clientOutputStream.writeByte(SenSignals.SEND_MSG_DATA.getSignal());
                                clientOutputStream.flush();
                                clientOutputStream.writeUTF(gson.toJson(convo.retrieveMessages(), Queue.class));
                                clientOutputStream.flush();
                                receivedByte = convo.expectMessage(client, _id);
                            } else if (receivedByte == -1) {
                                System.out.println("Client disconnected (IN SWITCH CASE 2)");
                                closeSocket(client, _id);
                                connected = false;
                                break;
                            } else if (receivedByte == RecSignals.DELETE_CONTACT.getSignal()) {
                                System.out.println("User deleted.");
                                break;
                            }
                        }
                        terminateThread(clientOutputStream);
                    } case 3 -> {
                        // Group serving logic
                    } case 4 -> {// Random chat logic
                        /* TODO: So this has a pretty specific bug that makes it so that you'll never connect to another Random Chat without resetting the view. I'll list the steps:
                         *  1-Get two users in a random chat
                         *  2-Have user A stop the chatting
                         *  3-After getting a "El otro usuario ah salido del chat" message, send a message with user B, user A should somehow also receive that message despite not chatting
                         *  4-Stop chatting with user B
                         *  5-Press skip button with both users
                         *  6-Press the resume button with both users
                         *  7-Magic
                         */
                        //Creates the list for the people waiting in the random chat
                        List<String> rand_ids = new ArrayList<>();
                        Optional<String> partner = Optional.empty();
                        //Receives the byte of the client that indicates the action
                        int receivedByte = clientInputStream.readByte();
                        while (receivedByte != RecSignals.EXIT_VIEW.getSignal()){ //Loops as long as the client doesn't exit the view
                            if (receivedByte == RecSignals.NEXT_RAND.getSignal() || receivedByte == RecSignals.START_RAND.getSignal()){ //Triggers whenever the client skips to the next random or starts the random chat
                                // TODO: Evaluate what's different between skip and start
                                partner = findPartner(_id, rand_ids, clientInputStream);
                                byte tempByte = checkContingency(partner.get());
                                // This can't be methodic because there's breaks, we can only copy it.
                                if (tempByte == (byte) -1){ //The client disconnected
                                    System.out.println("Client disconnected (IN CASE 4)");
                                    closeSocket(client, _id);
                                    connected = false;
                                    break;
                                } else if (tempByte == RecSignals.EXIT_VIEW.getSignal()){ //If the byte is -2, finish the loop early
                                    receivedByte = RecSignals.EXIT_VIEW.getSignal();
                                    continue;
                                } else if (tempByte == RecSignals.STOP_RAND.getSignal()){ //If the byte is 108, finish the current loop cycle early
                                    receivedByte = RecSignals.STOP_RAND.getSignal();
                                    continue;
                                }
                                //Starts the conversation between two random users
                                System.out.printf("Conversation started between user %s and %s.%n", _id, partner.get());
                                receivedByte = startConvo(client, rand_ids, _id);
                                //Ends the conversation between two random users
                                conversationEnd(_id, partner.get());
                                System.out.printf("Conversation ended between user %s and %s.%n", _id, partner.get());
                            } else if (receivedByte == RecSignals.STOP_RAND.getSignal()){ //Triggers if the client stopped talking with random people (Not exiting the view)
                                System.out.println("Stopping.");
                                //Ends the conversation between two random users
                                removeFromStructures(_id);
                                conversationEnd(_id, partner.get());
                                receivedByte = clientInputStream.readByte();
                            }
                        }
                        terminateThread(clientOutputStream);
                    } case 8 ->{ //Skipped to case 8 due to priority in development (Add contacts)
                        //Filling the requestList
                        AtomicReference<ArrayList<String>> list = new AtomicReference<>();
                        AtomicReference<String> acc_name = new AtomicReference<>();
                        populateLists(_id, list, acc_name, "fr_requests");

                        Gson gson = new Gson();

                        //Sending the requestList
                        String requestList = gson.toJson(getUserFList(list), Map.class);
                        clientOutputStream.writeUTF(requestList);
                        clientOutputStream.flush();

                        //Obtaining their pfps and sending them
                        String pfpList = gson.toJson(getUsersPfp(list), Map.class);
                        byte[] jsonBytes = pfpList.getBytes(StandardCharsets.UTF_8);
                        clientOutputStream.writeInt(jsonBytes.length);
                        clientOutputStream.flush();
                        clientOutputStream.write(jsonBytes);
                        clientOutputStream.flush();

                        byte receivedByte = 0xf; // random byte because it needs to update in loop
                        while(receivedByte != RecSignals.EXIT_VIEW.getSignal()){
                                receivedByte = clientInputStream.readByte();
                            System.out.println("Byte received: " + receivedByte);
                            if(receivedByte == RecSignals.ACCEPT_REQUEST.getSignal()){ //Client accepted a request
                                //Gets the info
                                System.out.println("Accepted request. Byte: 109");
                                String senderName = clientInputStream.readUTF();
                                String senderId = users.find(Filters.eq("acc_name", senderName)).first().getObjectId("_id").toString();
                                //Removes from the request list
                                users.findOneAndUpdate(Filters.eq("_id", new ObjectId(_id)), Updates.pull("fr_requests", senderId));
                                users.findOneAndUpdate(Filters.eq("_id", new ObjectId(_id)), Updates.push("u_friend_list_ids", senderId));
                                users.findOneAndUpdate(Filters.eq("_id", new ObjectId(senderId)), Updates.push("u_friend_list_ids", _id));
                                System.out.println("User id " + _id + " accepted " + senderId + "(" + senderName + ")'s request");

                            } else if (receivedByte == RecSignals.REJECT_REQUEST.getSignal()){//Client rejected a request
                                //Gets the info
                                System.out.println("Rejected request. Byte: 110");
                                String senderName = clientInputStream.readUTF();
                                String senderId = users.find(Filters.eq("acc_name", senderName)).first().getObjectId("_id").toString();
                                //Removes the request
                                users.findOneAndUpdate(Filters.eq("_id", new ObjectId(_id)), Updates.pull("fr_requests", senderId));
                                System.out.println("User id " + new ObjectId(_id) + " rejected " + senderId + "(" + senderName + ")'s request");


                            } else if (receivedByte == RecSignals.SEND_REQUEST.getSignal()){//Client sent a request
                                //Gets the info
                                System.out.println("Sent request. Byte: 111");
                                String receiverName = clientInputStream.readUTF();
                                System.out.println(receiverName);
                                String receiverID = "";
                                if(users.find(Filters.eq("acc_name", receiverName)).first() == null){
                                    receiverID = "Non-existent-user1234567";
                                    System.out.println("User id " + new ObjectId(_id) + " sent a request to a nonexistent user or already friend, returning byte -1");
                                    clientOutputStream.writeByte(-1);
                                    clientOutputStream.flush();
                                }else {
                                    receiverID = users.find(Filters.eq("acc_name", receiverName)).first().getObjectId("_id").toString();
                                    //Adds the request to the receiver's requests
                                    sendFrReq(clientOutputStream, _id, receiverID);
                                }


                            }
                        }
                    }
                    default -> System.err.println("Unknown byte was sent");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Refactored snippet. Handles friend request sending. Sends a byte to the user depending on whether
     * the request was automatically accepted or not.
     * @param clientOutputStream User output stream.
     * @param clientID User id.
     * @param receiverID Receiver id.
     * */
    public static void sendFrReq(DataOutputStream clientOutputStream, String clientID, String receiverID){
        try {
            System.out.printf("User %s sending request to %s.%n", clientID, receiverID);
            if (users.find(Filters.eq("_id", new ObjectId(receiverID))).first() != null //Checks that the user exists
                    && !users.find(Filters.eq("_id", new ObjectId(clientID))).first().getList("u_friend_list_ids", Object.class).contains(receiverID) //Checks that the user isn't in the friend list
            ) {
                if(!users.find(Filters.eq("_id", new ObjectId(clientID))).first().getList("fr_requests", Object.class).contains(receiverID)){ //If there wasn't a previous request, send it
                    users.findOneAndUpdate(Filters.eq("_id", new ObjectId(receiverID)), Updates.push("fr_requests", clientID));
                    System.out.println("User id " + new ObjectId(clientID) + " sent " + receiverID + " a contact request, returning byte 0");
                    clientOutputStream.writeByte(0);
                    clientOutputStream.flush();
                } else{ //If there was, accept it automatically
                    users.findOneAndUpdate(Filters.eq("_id", new ObjectId(clientID)), Updates.pull("fr_requests", receiverID));
                    users.findOneAndUpdate(Filters.eq("_id", new ObjectId(clientID)), Updates.push("u_friend_list_ids", receiverID));
                    users.findOneAndUpdate(Filters.eq("_id", new ObjectId(receiverID)), Updates.push("u_friend_list_ids", clientID));
                    System.out.println("User id " + clientID + " accepted " + receiverID + "'s request");
                    //Signal to warn the client that the request was accepted
                    clientOutputStream.writeByte(1);
                    clientOutputStream.flush();
                }
            } else { //If it doesn't exist or is already a friend, send a message to the client
                System.out.println("User id " + new ObjectId(clientID) + " sent a request to a nonexistent user or already friend, returning byte -1");
                clientOutputStream.writeByte(-1);
                clientOutputStream.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Really just makes sure the socket is closed and removed from the clientMap.
     * @param client Socket to be closed.
     * @param _id ID for clientMap.
     * */
    private static void closeSocket(Socket client, String _id) throws IOException {
        Socket socket = clientMap.remove(_id);
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        client.close();
        System.out.printf("Removed socket %s.%n", client.getRemoteSocketAddress());
    }

    /**
     * Removes the client from the random list when it stops
     * @param _id Client user id.
     * */
    private static void removeFromStructures(String _id) {
        synchronized (lock) {
            randomList.remove(_id);
            activeMatches.remove(_id);
        }
    }

    /**
     * Used to translate a string to a byte for the correct function of the random chat.
     * @param contingency String to be translated into a byte.
     * */
    private static byte checkContingency(String contingency){
        switch (contingency) {
            case "EXIT" -> {
                return -1;
            }
            case "EXIT_VIEW" -> {
                return -2;
            }
            case "STOP_RAND" -> {
                return 108;
            }
            default -> {
                return (byte) -10;
            }
        }
    }

    /**
     * Ending the conversation of a random chat. It removes active matches on both ends if needed.
     * @param _id User id, one end of the conversation.
     * @param partner_id Partner id, other end of the conversation.
     * */
    private static void conversationEnd(String _id, String partner_id){
        activeMatches.remove(_id);
        /*
        * TODO: It's not feasible to remove this snippet, it's needed to secure conversations have ended on both ends.
        *  The only solution is solving the sync issues in findPartner. Shouldn't give problems after that's fixed.
        * */
        activeMatches.forEach((k, v)-> {
            if (k.equals(partner_id) && v.equals(_id)) {
                System.out.printf("Deleting pair: {%s, %s}%n", partner_id, _id);
                activeMatches.remove(partner_id);
            }
        });
    }

    /**
     * Function meant to pair users with a random user in the randomList.
     * @param _id User id
     * @param clientInputStream User input stream. The user may or not do operations while finding a partner.
     * @param rand_ids ServeClient local list to populate.
     * */
    private static Optional<String> findPartner(String _id, List<String> rand_ids, DataInputStream clientInputStream) { //Binds two users into a conversation in the random chat
        synchronized (lock) { //Lock :3
            randomList.add(_id);
        }
        rand_ids.clear();
        rand_ids.add(_id);

        Optional<String> partner = Optional.empty();
        // Since we can't return a byte (because we're returning optionals) we can just return a verbose signal.
        String contingency = "";
        try {
            while (partner.isEmpty()) {
                System.out.println("Trying to find partner...");
                randomList.forEach(System.out::println);
                if(clientInputStream.available() > 0){
                    // We could receive a message here, but it won't do anything.
                    byte receivedByte = clientInputStream.readByte();
                    System.out.println("Received byte: " + receivedByte);
                    if (receivedByte == -1) { //The client disconnected
                        randomList.remove(_id);
                        contingency = "EXIT";
                        break;
                    } else if (receivedByte == RecSignals.EXIT_VIEW.getSignal()) { //The client left the scene
                        randomList.remove(_id);
                        contingency = "EXIT_VIEW";
                        break;
                    } else if (receivedByte == RecSignals.STOP_RAND.getSignal()) { //The client stops the random view (Not exiting the scene)
                        randomList.remove(_id);
                        contingency = "STOP_RAND";
                        break;
                    }
                }
                //Sets the partner for the clients during the random chat
                /*
                * TODO: The issue is not necessarily on removing clients. If multiple clients start
                *  at the same time, regardless of whether the conversation is over or not, the matchmaking
                *  suffers from synchronization issues that lead up to a mix in the streams (user a matches
                *  with user b and user c matches with user b).
                *  Synchronizing partner finding, which is just the snippet below, doesn't seem to fix the problem.
                * */
                synchronized (lock) {
                    if(!activeMatches.containsKey(_id)){

                        partner = randomList.stream()
                                .filter(id -> !id.equals(_id) && !activeMatches.containsKey(id))
                                .findAny();
                        System.out.println(partner);
                        if (!partner.isEmpty()) {
                            // Register match
                            activeMatches.put(_id, partner.get());
                            activeMatches.put(partner.get(), _id);

                            // Remove from randomList AFTER match is set
                            randomList.remove(_id);
                            randomList.remove(partner.get());
                        } else{
                            System.out.println("Partenr not found yet");
                        }
                    } else if (partner.isEmpty()){
                        partner = Optional.ofNullable(activeMatches.get(_id));
                        System.out.println("User " + partner.get() + " got paired with " + _id + ". Making a pair in the other direction to finish the pairing");
                        System.out.println(partner);
                    } else{
                        System.err.println("THIS ERROR SHOULDN'T BE SHOWN. Somehow the user has both a partner and is in the active list yet somehow hasn't left the loop");
                    }
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        String partnerId;

        if (partner.isPresent()){ //Checks if a partner has been matched to the client
            System.out.println("Partner found!");
            partnerId = partner.get();
        } else {
            return Optional.of(contingency);
        }
        rand_ids.add(partnerId);

        synchronized (lock) {
            // Register match
            activeMatches.put(_id, partnerId);
            activeMatches.put(partnerId, _id);

            // Remove from randomList AFTER match is set
            randomList.remove(_id);
            randomList.remove(partnerId);
        }
        System.out.printf("User %s newly paired with user %s.%n", _id, partnerId);
        return partner;
    }

    /**
     * Refactored snippet. Meant to start a conversation between random users.
     * @param rand_ids Local serveClient list, meant to initialize the conversation clientIDList.
     * @param client User socket.
     * @param _id User id.
     * */
    private static byte startConvo(Socket client, List<String> rand_ids, String _id) {
        byte receivedByte;
        Convo convo = new Convo(rand_ids);
        receivedByte = convo.expectAnonMsgs(client, _id);
        return receivedByte;
    }

    /**
     * Refactored snippet. When the user is in a conversation, it's expecting bytes from the server in a
     * separate thread. To make sure that thread doesn't stay alive, and ends up consuming bytes that it
     * shouldn't, we make sure to terminate that thread at the end.
     * @param clientOutputStream User's output stream.
     * */
    private static void terminateThread(DataOutputStream clientOutputStream) throws IOException {
        clientOutputStream.writeByte(SenSignals.TERMINATE.getSignal());
        clientOutputStream.flush();
    }

    /**
     * Method that populates an atomic list and fetches the name of our current user.
     * @param _id User ID, for easy of querying.
     * @param list List to populate.
     * @param acc_name Client name.
     * */
    private static void populateLists(String _id, AtomicReference<ArrayList<String>> list, AtomicReference<String> acc_name, String field) {
        MongoCursor<Document> query_of_list =
                users.find(
                        Filters.eq("_id", new ObjectId(_id))
                ).projection(
                        Projections.include("acc_name", field)
                ).iterator();
        query_of_list.forEachRemaining(document -> {
            list.set(document.get(field, ArrayList.class));
            acc_name.set(document.get("acc_name", String.class));
        });
    }

    /**
     * Server function that returns a map with the user's friends, intended functionality for serving
     * conversations, as the client will need to load a set of users in the chat view.
     * @param list ID list.
     * @return A map of the user's friends, with their account names and usernames respectively.
     * */
    private static Map<String, String> getUserFList(AtomicReference<ArrayList<String>> list){
        AtomicReference<Map<String, String>> friend_info = new AtomicReference<>();
        friend_info.set(new HashMap<>());
        list.get().forEach((id )-> {
            MongoCursor<Document> query_users =
                    users.find(
                            Filters.eq("_id", new ObjectId(id))
                    ).projection(
                            Projections.include("acc_name", "user_name", "profile_pic")
                    ).iterator();
            query_users.forEachRemaining((friend_document)-> {
                friend_info.get().put(
                        friend_document.get("acc_name", String.class),
                        friend_document.get("user_name", String.class)
                );
            }
            );
            query_users.close();
        });
        return friend_info.get();
    }

    /**
     * Client shouldn't store the profile pictures, instead it should load them in buffer
     * everytime a view loads. We're going to include the buffer information mapped into
     * the name of the file, which contains the account name and extension of the file.
     * @param list ID list.
     * @return A Map containing the picture buffer and the file name of the picture.
     * */
    private static Map<String, String> getUsersPfp(AtomicReference<ArrayList<String>> list){
        Map <String, String> pfp_info = new HashMap<>();
        list.get().forEach((id)->{
            MongoCursor<Document> query_users =
                    users.find(
                            Filters.eq("_id", new ObjectId(id))
                    ).projection(
                            Projections.include("acc_name", "user_name", "profile_pic")
                    ).iterator();
            query_users.forEachRemaining((document) -> {
                String filePath = document.get("profile_pic", String.class);
                String[] str_bfr = getPfpStrBfr(filePath);
                pfp_info.put(filePath.substring(filePath.lastIndexOf("\\") + 1), Arrays.toString(str_bfr));
            });
        });
        return pfp_info;
    }

    /**
     * Refactored snippet. Meant to get the string buffer of a server locally stored image.
     * @param filePath File path of the desired image.
     * */
    private static String[] getPfpStrBfr(String filePath){
        String[] str_bfr;
        try {
            File file = FileUtils.getFile(filePath);
            Byte[] bfr = ArrayUtils.toObject(FileUtils.readFileToByteArray(file));
            str_bfr = new String[bfr.length];
            for (int i = 0; i < bfr.length; i++) {
                str_bfr[i] = bfr[i].toString();
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath);
            throw new RuntimeException(e);
        }
        return str_bfr;
    }

    /**
     * Server procedure in charge of inserting a new user document into the database
     * based on the input given by the user.
     * <p>
     * If the operation was correct, server will send a 0 signal to the client.
     * If it wasn't, server will send a -1.
     * @param dIs Client input stream
     * @param dOs Client output stream
     * */
    private static byte handleNewRegistry(DataInputStream dIs, DataOutputStream dOs){
        try {
            byte action = dIs.readByte();
            switch (action) {
                case -1 -> {
                    // Client exits unexpectedly
                    return -1;
                }
                case -2 -> {
                    // Client exits the view, went back
                    return -2;
                }
                case 101 -> {
                    // Client actually creates an account
                    byte[] salt = new byte[16];
                    dIs.readFully(salt);

                    String json = dIs.readUTF();
                    Map<String, String> map = (Map<String, String>)new Gson().fromJson(json, Map.class);
                    Document document = new Document()
                            .append("u_friend_list_ids", new ArrayList<Integer>())
                            .append("is_notis_en", true)
                            .append("status", "offline")
                            .append("desc", map.get("desc"))
                            .append("user_tags",new ArrayList<String>())
                            .append("fr_requests", new ArrayList<String>())
                            .append("acc_name", map.get("_name"))
                            .append("email", map.get("email"))
                            .append("user_name", map.get("us_name"))
                            .append("acc_pass", map.get("pass_field"));

                    String secret_path = String.format("secret/%s", document.get("acc_name"));
                    createFile(secret_path, salt);
                    byte[] file_bfr = new byte[dIs.readInt()];
                    dIs.readFully(file_bfr);
                    String path = String.format("pfp\\%s.%s", document.get("acc_name"), map.get(".ext"));
                    createFile(path, file_bfr);
                    document.append("profile_pic", path);
                    InsertOneResult result = users.insertOne(document);
                    // Registry was correctly inserted, no exception thrown, writes 0 to the client.
                    dOs.writeByte(0);
                    dOs.flush();
                    return 100;
                } default -> { return -10; }
            }
        } catch (MongoWriteException e) {
            try {
                // Registry was not inserted, exception is thrown. We write -1 to the client.
                dOs.writeByte(-1);
                dOs.flush();
                System.out.println("DUPLICATE REGISTRY");
                e.printStackTrace();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return -10;
    }

    private static void createFile(String path, byte[] file_bfr) throws IOException {
        if (!Files.exists(Path.of(path))){
            // Personally, I prefer initializing it here, else it'll always create a file.
            FileUtils.writeByteArrayToFile(new File(path), file_bfr);
            System.out.println("Bytes received: ");
            System.out.println(Arrays.toString(file_bfr));
        }
    }

    public static String newHashedPassword(byte[] salt, String password) {
        try {
            /*
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
             */
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : hash) {
                stringBuilder.append(String.format("%02x", b));
            }
            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Server procedure to be called once, taking the user credentials and finding
     * such in the database collection called users.
     * <p>
     * Will write 0 if no user was found with the
     * provided credentials, 1 if a user is found.
     * @param dOs Client output stream.
     * @param dIs Client input stream.
     * @param client Client to be inserted in class variable clientMap.
     * @return Whether the login operation was correct or not, false if not.
     * */
    private static synchronized String handleLogin(DataInputStream dIs, DataOutputStream dOs, Socket client){
        String id = "";
        try {
            while (true) {
                byte code = dIs.readByte();
                switch (code) {
                    case -1 -> { //Client disconnects
                        return "UN_EXIT";
                    }
                    case -2 -> { //Client cancels login
                        return "EXIT";
                    }
                    case 100 -> { //Client attempts a login

                        String[] userCreds = dIs.readUTF().split("/");
                        System.out.println(Arrays.asList(userCreds));
                        // MongoClient operates with data...
                        Bson projection = Projections.fields(
                                Projections.include("_id", "acc_name", "acc_pass")
                        );
                        //Creates a filter
                        String acc_name = userCreds[0];
                        File file = FileUtils.getFile(String.format("secret/%s", acc_name));
                        byte[] salt = FileUtils.readFileToByteArray(file);

                        Bson filter = Filters.and(
                                Filters.eq("acc_name", acc_name),
                                Filters.eq("acc_pass", newHashedPassword(salt, userCreds[1]))
                        );

                        MongoCursor<Document> cursor = users.find(filter).projection(projection).iterator();

                        id = "";
                        while (cursor.hasNext()) {
                            id = cursor.next().get("_id").toString();
                        }

                        if (id.isEmpty()) { //User not found
                            System.out.println("Error: user not found");
                            dOs.writeByte(0);
                        } else { //User found
                            // Saves the id and client into the clientMap
                            System.out.printf("User %s logged in\n", id);
                            dOs.writeByte(1);
                            dOs.flush();
                            cursor.close();
                            // clientMap.put(client, id);
                            clientMap.put(id, client);
                            return id;
                        }
                    } case 10 -> { // Scene signal for mail popup
                        boolean result = mailAuth(dIs, dOs);
                        System.out.println( result ? "AUTH_SUCCESS" : "AUTH_FAIL");
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean mailAuth(DataInputStream dIs, DataOutputStream dOs){
        try {
            while (true) {
                byte code = dIs.readByte();
                if (code == RecSignals.EXIT_VIEW.getSignal()){
                    return false;
                } else if (code == -1){
                    return false;
                } else if (code == RecSignals.EMAIL_SENT.getSignal()){
                    String email = dIs.readUTF();
                    Document document = users.find(Filters.eq("email", email)).first();
                    if (document == null) { // No user with matching email
                        dOs.writeByte(-1);
                    } else { // User with matching email found, sending code email
                        dOs.writeByte(1);
                        String id = document.getObjectId("_id").toHexString();
                        // Got to make auth code here, then pass it to both expect and send auth code procedures
                        int auth_code = new Random().nextInt(900000) + 100000;
                        EmailUtil.sendAuthCode(email, auth_code);
                        byte result = expectAuthCode(dIs, dOs, auth_code, id);
                        return result == 100;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte expectAuthCode(DataInputStream dIs, DataOutputStream dOs, int auth_code, String id){
        try {
            while (true) {
                byte code = dIs.readByte();
                if (code == RecSignals.EXIT_VIEW.getSignal()){
                    return code;
                } else if (code == -1){
                    return code;
                } else if (code == RecSignals.CODE_SENT.getSignal()){
                    int user_code = dIs.readInt();
                    if (user_code == auth_code){
                        dOs.writeByte(2);
                        return expectPassword(dIs, dOs, id);
                    } else { // Failed
                        dOs.writeByte(-2);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte expectPassword(DataInputStream dIs, DataOutputStream dOs, String id){
        // Another loop that expects exit, unexpected exit, or the needed operation (password submitting).
        try {
            while (true) {
                byte code = dIs.readByte();
                if (code == RecSignals.EXIT_VIEW.getSignal()){
                    return code;
                } else if (code == -1){
                    return code;
                } else if (code == RecSignals.PASSWORD_SENT.getSignal()){
                    String password = dIs.readUTF();
                    users.findOneAndUpdate(Filters.eq("_id", new ObjectId(id)), Updates.set("acc_pass", password));
                    return 100;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Procedure that deletes a user from their friend list. It is bidirectional, meaning it will take
     * the user ids from both of their documents at the mongo collection, users.
     * @param user1 User who requests the deletion.
     * @param user2 User who is to be deleted from user1's friend list, and vice versa.
     * */
    public static void deleteFriends(String user1, String user2){
        users.findOneAndUpdate(Filters.eq("_id", new ObjectId(user1)), Updates.pull("u_friend_list_ids", user2));
        System.out.println("Se ha borrado al usuario(id): " + user2 + " de los amigos de: " + user1);
        users.findOneAndUpdate(Filters.eq("_id", new ObjectId(user2)), Updates.pull("u_friend_list_ids", user1));
        System.out.println("Se ha borrado al usuario(id): " + user1 + " de los amigos de: " + user2);
    }
}