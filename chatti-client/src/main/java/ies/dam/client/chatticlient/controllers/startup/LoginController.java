package ies.dam.client.chatticlient.controllers.startup;

import ies.dam.client.chatticlient.ClientApplication;
import ies.dam.client.chatticlient.Scenes;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class LoginController {

    @FXML
    public TextField userField;
    @FXML
    public PasswordField passField;
    @FXML
    public Label warningText;

    public void initialize() {
        warningText.setVisible(false); //Makes the warning invisible at the start
        try { //Warns the server that the client is in this scene
            ClientApplication.dataOutputStream.writeByte(Scenes.LOGIN_VIEW.getSignal());
            ClientApplication.dataOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void login(ActionEvent actionEvent) {
        try {
            if (!userField.getText().isEmpty() && !passField.getText().isEmpty()) { //Doesn't allow you to log in if you have empty fields
                //Sends the login info to the server
                ClientApplication.dataOutputStream.write(100);
                ClientApplication.dataOutputStream.flush();

                ClientApplication.dataOutputStream.writeUTF(
                        String.format("%s/%s",
                                userField.getText(),
                                passField.getText()
                        )
                );
                ClientApplication.dataOutputStream.flush();

                int signal = ClientApplication.dataInputStream.readByte(); //Gets the response of the server to see if the login info is correct
                switch (signal) {
                    case 0 -> { //Wrong login info
                        warningText.setVisible(true);
                    }
                    case 1 -> { //Right login info
                        ClientApplication.setUsername(userField.getText());
                        ClientApplication.setHashedPassword(passField.getText());
                        ClientApplication.loadScene(Scenes.CHAT_VIEW);
                    }
                }
            }
        } catch (IOException e) {
            warningText.setText("Something went wrong.");
            warningText.setVisible(true);
            e.printStackTrace();
        }
    }

    private void writeExitByte(){
        try {
            ClientApplication.dataOutputStream.writeByte(-2);
            ClientApplication.dataOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void cancelLogin(ActionEvent actionEvent) {
        writeExitByte();
        ClientApplication.loadScene(Scenes.WELCOME_VIEW);
    }

    @FXML
    public void mailDialogue(ActionEvent actionEvent) {
        ClientApplication.loadPopup(Scenes.MAIL_DIALOGUE);
    }
}
