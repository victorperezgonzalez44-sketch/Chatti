package ies.dam.client.chatticlient.controllers.chat.popup;

import ies.dam.client.chatticlient.ClientApplication;
import javafx.event.ActionEvent;

public class FileSentController {
    public void closePopup(ActionEvent actionEvent) {
        ClientApplication.closePopup();
    }
}