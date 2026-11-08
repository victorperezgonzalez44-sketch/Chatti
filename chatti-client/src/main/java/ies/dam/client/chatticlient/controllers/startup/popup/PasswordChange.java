package ies.dam.client.chatticlient.controllers.startup.popup;

import ies.dam.client.chatticlient.ClientApplication;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

import java.io.IOException;

public class PasswordChange {
    @FXML
    public PasswordField password;
    @FXML
    public PasswordField confirmPassword;
    @FXML
    public Label warningLabel;

    @FXML
    public void confirm(ActionEvent actionEvent) {
        try {
            if (password.getText().equals(confirmPassword.getText())
                    && !(password.getText().isEmpty() || confirmPassword.getText().isEmpty())) {
                ClientApplication.dataOutputStream.writeByte(53);
                ClientApplication.dataOutputStream.flush();
                ClientApplication.dataOutputStream.writeUTF(ClientApplication.hashedPassword(password.getText()));
                ClientApplication.closePopup();
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
