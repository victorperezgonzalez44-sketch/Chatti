package ies.dam.client.chatticlient.controllers.chat.popup;

import ies.dam.client.chatticlient.ClientApplication;
import javafx.event.ActionEvent;
import java.io.IOException;

public class LeaveGroupController {

    public void leaveGroup(ActionEvent actionEvent) {
        //Add logic to leave the group
        try {
            //This basically just warns the server that you're leaving the group. I don't even think that more is needed here
            ClientApplication.dataOutputStream.writeByte(-1); //Change. Defaulted to -1 due to not knowing which byte will be used
            ClientApplication.dataOutputStream.flush();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
        ClientApplication.closePopup();
    }
    public void closePopup() {
        ClientApplication.closePopup();
    }
}
