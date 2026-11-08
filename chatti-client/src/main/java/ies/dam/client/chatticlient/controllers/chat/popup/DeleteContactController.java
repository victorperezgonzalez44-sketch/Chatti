package ies.dam.client.chatticlient.controllers.chat.popup;

import ies.dam.client.chatticlient.ClientApplication;
import ies.dam.client.chatticlient.Scenes;
import ies.dam.client.chatticlient.controllers.chat.MainChatController;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;

import java.io.IOException;

public class DeleteContactController {
    public void deleteContact(ActionEvent actionEvent) {
        try { //This actually shouldn't need anything else in theory as you previously indicated in which convo you are, meaning that you've warned who you're going to delete now
            ClientApplication.dataOutputStream.writeByte(112);
            ClientApplication.dataOutputStream.flush();
            //This segment just think the server into thinking that you left the chat view so it can reload
            ClientApplication.dataOutputStream.writeByte(-2);
            ClientApplication.dataOutputStream.flush();
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
        //Reloads the view
        ClientApplication.loadScene(Scenes.CHAT_VIEW);
        ClientApplication.closePopup();
    }

    public void closePopup(ActionEvent actionEvent) {

        ClientApplication.closePopup();
    }
}
