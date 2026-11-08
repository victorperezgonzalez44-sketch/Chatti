package ies.dam.client.chatticlient.controllers.startup.popup;

import ies.dam.client.chatticlient.ClientApplication;
import ies.dam.client.chatticlient.Scenes;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class EmailController {

    @FXML
    public TextField emailField;
    @FXML
    public Label warningLabel;

    @FXML
    private void initialize(){
        try {
            ClientApplication.dataOutputStream.writeByte(Scenes.MAIL_DIALOGUE.getSignal());
            ClientApplication.dataOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void sendMail(ActionEvent actionEvent) {
        String email = emailField.getText();
        try {
            ClientApplication.dataOutputStream.writeByte(51);
            ClientApplication.dataOutputStream.flush();
            ClientApplication.dataOutputStream.writeUTF(email);
            ClientApplication.dataOutputStream.flush();

            byte response = ClientApplication.dataInputStream.readByte();
            if (response == -1){
                warningLabel.setVisible(true);
                warningLabel.setText("Invalid email address");
            } else if (response == 1){
                // Load the change auth pop up.
                ClientApplication.closePopup();
                ClientApplication.loadPopup(Scenes.MAIL_CONFIRMATION);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void cancel(ActionEvent actionEvent) {
        try {
            ClientApplication.dataOutputStream.writeByte(-2);
            ClientApplication.dataOutputStream.flush();
            ClientApplication.closePopup();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
