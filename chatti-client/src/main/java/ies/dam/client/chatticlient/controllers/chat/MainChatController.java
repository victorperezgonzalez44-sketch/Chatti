package ies.dam.client.chatticlient.controllers.chat;

import com.google.gson.Gson;
import ies.dam.client.chatticlient.ClientApplication;
import ies.dam.client.chatticlient.Scenes;
import ies.dam.client.chatticlient.controllers.chat.popup.DeleteContactController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import org.xml.sax.HandlerBase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainChatController extends Chattable{
    @FXML
    public Circle profilePicture;
    @FXML
    public Label username;
    @FXML
    public Label accountName;

    @Override
    public void initialize() {
        try {
            //Warns the server that you're in this scene
            ClientApplication.dataOutputStream.writeByte(Scenes.CHAT_VIEW.getSignal());
            ClientApplication.dataOutputStream.flush();
            //This receives the friend list and fills the convoList with the friend list, along with adding a listener for whenever an item is selected
            super.initialize();
            super.convoList
                    .getSelectionModel()
                    .selectedIndexProperty()
                    .addListener((observable, oldValue, newValue) -> {
                        //This sends which convo has been selected
                        try {
                            //Warning the server about the change
                            ClientApplication.dataOutputStream.writeByte(97);
                            ClientApplication.dataOutputStream.flush();
                            //Clearing the screen
                            chatBox.getChildren().clear();
                            //Telling the server which convo has been selected
                            String acc_name = super.observableList.get(newValue.intValue()).getThirdColumnText();
                            ClientApplication.dataOutputStream.writeUTF(acc_name);
                            ClientApplication.dataOutputStream.flush();
                            System.out.println("Sent acc name: " + acc_name);
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
                        //This section sets the data of the display that shows who you're talking to
                        username.setText(super.observableList.get(newValue.intValue()).getName());
                        accountName.setText("@"+super.observableList.get(newValue.intValue()).getThirdColumnText());
                        profilePicture.setFill(new ImagePattern(super.observableList.get(newValue.intValue()).getImage()));
            });
            new Thread(this::handleMessages).start(); //Handles receiving messages. Method is located in chattable
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void deleteUser(ActionEvent actionEvent) {
        //Do not write exit byte on this one, instead modify the chat handler to handle deleting a contact
        ClientApplication.loadPopup(Scenes.DELETE_CONTACT);
    }

    @FXML
    public void communitySwitch(ActionEvent actionEvent) {//Most likely won't be used
        writeExitByte();
        // Load the community view
    }

    @FXML
    public void settingSwitch(ActionEvent actionEvent) {
        writeExitByte();
        // Load the user settings view
    }

    @FXML
    public void newChat(ActionEvent actionEvent) { //Sends you to the scene to add contacts
        writeExitByte();
        ClientApplication.loadScene(Scenes.ADD_CONTACT);
    }

    @FXML
    public void randomChat(ActionEvent actionEvent) { //Sends you to the scene where you chat with someone random
        writeExitByte();
        ClientApplication.loadScene(Scenes.RANDOM_CHAT);
    }

    @FXML
    public void embedFile(ActionEvent actionEvent) {
        //Missing method to detect which convo it belongs to
        ClientApplication.loadPopup(Scenes.FILE_SELECT);
    }

    @FXML
    public void sendMessage(ActionEvent actionEvent) {
        //Logic to send a message
        try {
            if(!messageField.getText().isEmpty()){
                //Warns that a message is going to be sent
                ClientApplication.dataOutputStream.writeByte(99);
                ClientApplication.dataOutputStream.flush();
                //Sends the message
                ClientApplication.dataOutputStream.writeUTF(super.messageField.getText());
                ClientApplication.dataOutputStream.flush();
                //Sends any embed files?
                Map<Byte[], String> emb_files = new HashMap<>();
                String message = new Gson().toJson(emb_files, Map.class);
                ClientApplication.dataOutputStream.writeUTF(message);
                ClientApplication.dataOutputStream.flush();
                //Adds the message to the chat area
                appendToChatBox(ClientApplication.getUsername(), super.messageField.getText());
                messageField.setText("");
            }
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

    @FXML
    public void enterMessage(KeyEvent event) {
        if(event.getCode() == KeyCode.ENTER){
            sendMessage(null);
        }
    }

    public void deleteChat() {
        observableList.remove(convoList.getSelectionModel().getSelectedItem());
        chatBox.getChildren().clear();
        convoList.getSelectionModel().getSelectedItems().clear();
        username.setText("Usuario");
        accountName.setText("Cuenta");
        profilePicture.setFill(Paint.valueOf("B0B0B0")); //If css were to be changed, this should also be changed
    }

    @FXML
    public void groupSwitch(MouseEvent mouseEvent) { //Switches you to the group scene
        writeExitByte();
        ClientApplication.loadScene(Scenes.GROUP_CHAT_VIEW);
    }
}
