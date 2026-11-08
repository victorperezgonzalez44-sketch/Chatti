package ies.dam.client.chatticlient.controllers.startup.popup;

import ies.dam.client.chatticlient.ClientApplication;
import ies.dam.client.chatticlient.Scenes;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class EmailAuthController {

    @FXML
    public TextField codeField;
    @FXML
    public Label warningLabel;

    @FXML
    public void confirm(ActionEvent actionEvent) {
        try {
            int code = Integer.parseInt(codeField.getText());
            // User typed something weird? Might as well not go through here
            ClientApplication.dataOutputStream.writeByte(52);
            ClientApplication.dataOutputStream.flush();
            ClientApplication.dataOutputStream.writeInt(code);
            ClientApplication.dataOutputStream.flush();

            byte response = ClientApplication.dataInputStream.readByte();
            if (response == 2){
                ClientApplication.closePopup();
                ClientApplication.loadPopup(Scenes.PASSWORD_CHANGE);
            } else if (response == -2){
                warningLabel.setVisible(true);
                warningLabel.setText("Code does not match.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NumberFormatException e){
            warningLabel.setVisible(true);
            warningLabel.setText("Input was not a number.");
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
