package ies.dam.client.chatticlient.controllers.chat;

import com.google.gson.Gson;
import ies.dam.client.chatticlient.ClientApplication;
import ies.dam.client.chatticlient.Scenes;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public class GroupChatController extends Chattable{
    @FXML
    private Circle profilePicture;
    @FXML
    private Label groupName;

    @Override
    public void initialize() {
        try {
            //Warns the server that you're in this scene
            ClientApplication.dataOutputStream.writeByte(Scenes.GROUP_CHAT_VIEW.getSignal());
            ClientApplication.dataOutputStream.flush();
            //This receives the friend list and fills the convoList with the group list, along with adding a listener for whenever an item is selected
            super.initialize();
            super.convoList
                    .getSelectionModel()
                    .selectedIndexProperty()
                    .addListener((observable, oldValue, newValue) -> {
                        //This sends which convo has been selected
                        try{
                            //Warning the server about the change
                            ClientApplication.dataOutputStream.writeByte(97); //NOT SURE IF THE SERVER WILL HANDLE IT WITH THE SAME BYTE AS MainChatContoller.java
                            ClientApplication.dataOutputStream.flush();
                            //Clearing the chat space
                            chatBox.getChildren().clear();
                            //Telling the server  which group has been selected
                            String group_name = super.observableList.get(newValue.intValue()).getThirdColumnText();
                            ClientApplication.dataOutputStream.writeUTF(group_name);
                            ClientApplication.dataOutputStream.flush();
                            System.out.println("Sent group name: " + group_name);
                        }catch(IOException e){
                            throw new RuntimeException(e);
                        }
                        //This section sets the data of the display that shows the group you're talking in
                        groupName.setText(super.observableList.get(newValue.intValue()).getName());
                        profilePicture.setFill(new ImagePattern(super.observableList.get(newValue.intValue()).getImage()));
                    });
            new Thread(this::handleMessages).start(); //Handles receiving messages. Method is located in Chattable.java
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addUserToGroup(){
        ClientApplication.loadPopup(Scenes.ADD_TO_GROUP);
    }

    public void exitGroup(){
        ClientApplication.loadPopup(Scenes.LEAVE_GROUP);
    }

    public void communitySwitch(){
        writeExitByte();
        // Load the community view
    }

    @FXML
    public void sendMessage(ActionEvent event){
        try {
            if (!messageField.getText().isEmpty()) { //Stops any empty messages from being sent
                //Warns the server that a message is going be sent
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
        }catch(IOException e){
                throw new RuntimeException(e);
        }
    }

    @FXML
    public void enterMessage(KeyEvent event) { //Handles sending the message with the enter key
        if(event.getCode() == KeyCode.ENTER){
            sendMessage(null);
        }
    }
    public void SettingSwitch(){
        writeExitByte();
        // Load the user settings view
    }
    public void newGroup(){ //Sends you to the scene to create a group
        writeExitByte();
        ClientApplication.loadScene(Scenes.CREATE_GROUP);
    }
    public void randomChat(){
        //Most likely add logic to register the suer in the list of people in the random chat section
        writeExitByte();
        ClientApplication.loadScene(Scenes.RANDOM_CHAT);
    }
    public void embedFile(){
        ClientApplication.loadPopup(Scenes.FILE_SELECT);
    }

    public void chatSwitch(MouseEvent mouseEvent) { //Switches you to the chat scene
        writeExitByte();
        ClientApplication.loadScene(Scenes.CHAT_VIEW);
    }
}
