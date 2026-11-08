package ies.dam.client.chatticlient.controllers.chat;

import com.google.gson.Gson;
import ies.dam.client.chatticlient.ClientApplication;
import ies.dam.client.chatticlient.Scenes;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import org.apache.commons.io.FileUtils;
import org.controlsfx.control.action.Action;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RandomChatController extends Chattable{
    //TODO: The chat has become inconsistent with the connections, having problems to pair people or despairing them mid conversation
    @FXML
    public Circle profilePicture;
    @FXML
    public Label username;
    @FXML
    public Button stateButton;

    private EventHandler<ActionEvent> startEvent;
    private EventHandler<ActionEvent> stopEvent;

    @Override
    public void initialize(){
        try {
            //Warns the server that the client is in this scene
            ClientApplication.dataOutputStream.writeByte(Scenes.RANDOM_CHAT.getSignal());
            ClientApplication.dataOutputStream.flush();

            //Initializes the events
            startEvent = new EventHandler<ActionEvent>() { //Event related to starting to chat with random people
                @Override
                public void handle(ActionEvent event) {
                    startRandom();
                    stateButton.setText("Detener");
                }
            };
            stopEvent = new EventHandler<ActionEvent>() { //Event related to pausing the chat with random people
                @Override
                public void handle(ActionEvent event) {
                    stopRandom();
                    stateButton.setText("Continuar");
                }
            };
            //Sets the initial event to startEvent
            stateButton.setOnAction(startEvent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        new Thread(this::handleMessages).start(); //Handles receiving messages. Method is located in Chattable.java
    }

    public void addRandom() {  //Logic to send the other a contact request
        try {
            //Warns the server that a request is going to be sent
            ClientApplication.dataOutputStream.writeByte(106);
            ClientApplication.dataOutputStream.flush();

        } catch (IOException e){
            ClientApplication.showAlert(Alert.AlertType.ERROR, "Error de conexión",
                    "No tienes conexión con el servidor",
                    "Si el problema continua, prueba lo siguiente: " +
                            "\n-Espere a que la aplicación recarge la ventana" +
                            "\n-Compruebe su conexión a internet. " +
                            "\n-Reinicie la aplicación. " +
                            "\nSi el problema continua tras intentar lo anterior," +
                            "\nes posible que el servidor no se encuentre activo");
            throw new RuntimeException(e);
        }
    }
    public void skipRandom() {
        //Logic to skip to the next random
        try {
            ClientApplication.dataOutputStream.writeByte(105);
            ClientApplication.dataOutputStream.flush();
            chatBox.getChildren().clear();
        } catch (IOException e) {
            ClientApplication.showAlert(Alert.AlertType.ERROR, "Error de conexión",
                    "No tienes conexión con el servidor",
                    "Si el problema continua, prueba lo siguiente: " +
                            "\n-Espere a que la aplicación recarge la ventana" +
                            "\n-Compruebe su conexión a internet. " +
                            "\n-Reinicie la aplicación. " +
                            "\nSi el problema continua tras intentar lo anterior," +
                            "\nes posible que el servidor no se encuentre activo");
            throw new RuntimeException(e);
        }
    }

    public void startRandom() {
        //Logic to start a random conversation
        try {
            ClientApplication.dataOutputStream.writeByte(107);
            ClientApplication.dataOutputStream.flush();
            username.setText("Anónimo");
            profilePicture.setFill(new ImagePattern(new Image(String.valueOf(ClientApplication.class.getResource("imgs/Ongoing.png")))));
            stateButton.setOnAction(stopEvent);
            chatBox.getChildren().clear();
        } catch (IOException e) {
            ClientApplication.showAlert(Alert.AlertType.ERROR, "Error de conexión",
                    "No tienes conexión con el servidor",
                    "Si el problema continua, prueba lo siguiente: " +
                            "\n-Espere a que la aplicación recarge la ventana" +
                            "\n-Compruebe su conexión a internet. " +
                            "\n-Reinicie la aplicación. " +
                            "\nSi el problema continua tras intentar lo anterior," +
                            "\nes posible que el servidor no se encuentre activo");
            throw new RuntimeException(e);
        }
    }

    public void stopRandom() {
        //Logic to stop the conversations
        try {
            ClientApplication.dataOutputStream.writeByte(108);
            ClientApplication.dataOutputStream.flush();
            username.setText("Pausado");
            profilePicture.setFill(new ImagePattern(new Image(String.valueOf(ClientApplication.class.getResource("imgs/Paused.png")))));
            stateButton.setOnAction(startEvent);
        } catch (IOException e) {
            ClientApplication.showAlert(Alert.AlertType.ERROR, "Error de conexión",
                    "No tienes conexión con el servidor",
                    "Si el problema continua, prueba lo siguiente: " +
                            "\n-Espere a que la aplicación recarge la ventana" +
                            "\n-Compruebe su conexión a internet. " +
                            "\n-Reinicie la aplicación. " +
                            "\nSi el problema continua tras intentar lo anterior," +
                            "\nes posible que el servidor no se encuentre activo");
            throw new RuntimeException(e);
        }
    }

    public void goBack(){
        writeExitByte();
        ClientApplication.loadScene(Scenes.CHAT_VIEW);
    }

    @FXML
    public void sendMessage(ActionEvent event) {
        try {
            if (!messageField.getText().isEmpty()) {
                //Warns the server that a message is going to be sent
                ClientApplication.dataOutputStream.writeByte(99);
                ClientApplication.dataOutputStream.flush();
                //Sends the message
                ClientApplication.dataOutputStream.writeUTF(super.messageField.getText());
                ClientApplication.dataOutputStream.flush();
                //Adds the message to the chat area
                appendToChatBoxAnon(super.messageField.getText());
                messageField.setText("");
            }
        } catch(IOException e){
            ClientApplication.showAlert(Alert.AlertType.ERROR, "Error de conexión",
                    "No tienes conexión con el servidor",
                    "Si el problema continua, prueba lo siguiente: " +
                            "\n-Espere a que la aplicación recarge la ventana" +
                            "\n-Compruebe su conexión a internet. " +
                            "\n-Reinicie la aplicación. " +
                            "\nSi el problema continua tras intentar lo anterior," +
                            "\nes posible que el servidor no se encuentre activo");
            throw new RuntimeException(e);
        }
    }
    @FXML
    public void enterMessage(KeyEvent event) {
        if(event.getCode() == KeyCode.ENTER){
            sendMessage(null);
        }
    }

    public void appendToChatBoxAnon(String message){ //This method is only for your own messages
        try {
            //Creating the label with the message
            Label label = new Label(String.format("Tú: %s", message));
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
        }catch (InterruptedException e){
            throw new RuntimeException(e);
        }
    }
}
