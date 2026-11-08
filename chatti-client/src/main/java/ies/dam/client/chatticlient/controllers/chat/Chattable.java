package ies.dam.client.chatticlient.controllers.chat;

import com.google.gson.Gson;
import ies.dam.client.chatticlient.ClientApplication;
import ies.dam.client.chatticlient.Scenes;
import ies.dam.info_adapter.model.InfoModel;
import ies.dam.info_adapter.two_columns.TwoUsersCellFactory;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;

public abstract class Chattable {
    @FXML
    protected ListView<InfoModel> convoList;
    @FXML
    protected ToggleSwitch toggleSwitch;
    @FXML
    protected TextField messageField;
    @FXML
    public VBox chatBox;
    @FXML
    public ScrollPane scrollPane;

    protected String message = "";

    protected ObservableList<InfoModel> observableList;

    //Fills the convoList with the friends
    public void initialize(){
        Gson gson = new Gson();
        try {
            //Gets a map with the layout of the users and profile pictures?
            Map<String, String> acc_user_map = gson.fromJson(ClientApplication.dataInputStream.readUTF(), Map.class);
            System.out.println(acc_user_map.toString());
            byte[] jsonBytes = new byte[ClientApplication.dataInputStream.readInt()];
            ClientApplication.dataInputStream.readFully(jsonBytes);
            Map <String, String> pfp_map = gson.fromJson(new String(jsonBytes, StandardCharsets.UTF_8), Map.class);
            System.out.println(pfp_map.toString());
            ArrayList<InfoModel> infoModels = new ArrayList<>();

            //Reads the map with the profile pictures and handles the images
            pfp_map.forEach((key, value) -> {
                String[] bytes_rep = value.substring(1, value.length() - 1).split(", ");
                byte[] bytes = new byte[bytes_rep.length];
                for (int i = 0; i < bytes_rep.length; i++) {
                    bytes[i] = Byte.parseByte(bytes_rep[i]);
                }

                //Creates the custom ListView cell
                Image image = new Image(new ByteArrayInputStream(bytes));
                String identifier_name = key.substring(0, key.lastIndexOf("."));
                String name = acc_user_map.get(identifier_name);
                infoModels.add(new InfoModel(image, name, identifier_name));
            });
            //Reads and fills the convoList with the friends
            convoList.setCellFactory(new TwoUsersCellFactory());
            observableList = FXCollections.observableArrayList();
            observableList.addAll(infoModels);
            convoList.setItems(observableList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(ActionEvent event) {
        //Insertar lógica para enviar un mensaje (SI ESTO AL FINAL NO LO USAMOS, MEJOR BORRARLO)
    }

    //Es enviar mensaje al pulsar enter, no introducir el mensaje
    public void enterMessage(KeyEvent event) {
        if(event.getCode() == KeyCode.ENTER){
            message = messageField.getText();

            //Insertar lógica para enviar un mensaje
        }
    }

    public void embedFile(ActionEvent event) {
        //INSERTAR LÓGICA PARA DISTINGUIR A DONDE MANDARLO
        ClientApplication.loadPopup(Scenes.FILE_SELECT);
    }

    public void settingSwitch(ActionEvent event) {
        // Initializing the settings view
    }

    protected void writeExitByte(){ //Warns the server that the client is leaving the scene
        try {
            ClientApplication.dataOutputStream.writeByte(-2);
            ClientApplication.dataOutputStream.flush();
        } catch (IOException e) {
            ClientApplication.showAlert(Alert.AlertType.ERROR, "Error de conexión",
                    "No tienes conexión con el servidor",
                    "Si el problema continua, prueba lo siguiente: " +
                            "\n-Espere a que la aplicación recarge la ventana" +
                            "\n-Compruebe su conexión a internet." +
                            "\n-Reinicie la aplicación." +
                            "\nSi el problema continua tras intentar lo anterior," +
                            "\nes posible que el servidor no se encuentre activo");
            throw new RuntimeException(e);
        }
    }

    protected void appendToChatBox(String username, String message){
        //Adds the messages to the chatBox
        try {
            if (username.equals(ClientApplication.getUsername())) { //Your own messages
                //Creating the label with the message
                Label label = new Label(String.format("%s: %s", username, message));
                label.getStyleClass().add("OwnChatBubble");
                label.setWrapText(true);
                //Creating the area in which the message is added
                HBox hbox = new HBox();
                hbox.setAlignment(Pos.CENTER_RIGHT);
                hbox.getChildren().add(label);
                //Adding the message to the chatBox
                chatBox.getChildren().add(hbox);
                //Waiting a bit and then scrolling to the bottom
                Thread.sleep(20);
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        scrollPane.setVvalue(1D);
                    }
                });
            } else { //Other people's messages
                //Creating the label with the message
                Label label = new Label(String.format("%s: %s", username, message));
                label.getStyleClass().add("OtherChatBubble");
                //Creating the area in which the message is added
                HBox hbox = new HBox();
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.getChildren().add(label);
                //Adding the message to the chatBox
                chatBox.getChildren().add(hbox);
                //Waiting a bit and then scrolling to the bottom
                Thread.sleep(20);
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        scrollPane.setVvalue(1D);
                    }
                });
            }
        }catch (InterruptedException e){
            throw new RuntimeException(e);
        }
    }

    protected void handleMessages(){ //Method made to handle receiving messages. Launched as a thread
        try {
            while (true) {
                byte signal = ClientApplication.dataInputStream.readByte();
                switch (signal) {
                    case 101 -> { // Manages adding already existing messages to the chat section
                        String msg_json = ClientApplication.dataInputStream.readUTF();
                        Queue<String> queue = new Gson().fromJson(msg_json, Queue.class);
                        Platform.runLater(() -> {
                            while (!queue.isEmpty()) {
                                String[] pair = queue.poll().split("/");
                                appendToChatBox(pair[0], pair[1]);
                            }
                        });
                    }
                    case 100 -> { // Manages adding new messages to the chat section
                        String peer_message = ClientApplication.dataInputStream.readUTF();
                        String[] packet = peer_message.split("/");
                        Platform.runLater(() -> appendToChatBox(packet[1], packet[0]));
                    }
                    case -10 -> { //The thread is closed
                        System.out.println("Thread terminated!");
                        return;
                    }
                    case -1 -> { //This should be for nonexistent users or ones that you already have in your contact list
                        System.out.println("Invalid friend request.");
                    }
                    case 0 -> { //This should only be for existing users that you do not have direct contact with
                        System.out.println("Valid friend request.");
                    }
                    default -> System.out.println("Unknown signal: " + signal);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
