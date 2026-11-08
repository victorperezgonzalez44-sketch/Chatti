module ies.dam.client.chatticlient {
    requires ies.dam.status_component;
    requires com.google.gson;
    requires org.apache.commons.io;
    requires org.controlsfx.controls;
    //requires org.apache.commons.lang3;
    requires ies.dam.info_adapter;
    requires javafx.fxml;
    requires java.xml;


    opens ies.dam.client.chatticlient to javafx.fxml;
    exports ies.dam.client.chatticlient;
    opens ies.dam.client.chatticlient.controllers.startup to javafx.fxml;
    exports ies.dam.client.chatticlient.controllers.startup;
    opens ies.dam.client.chatticlient.controllers.chat to javafx.fxml;
    exports ies.dam.client.chatticlient.controllers.chat to javafx.fxml;
    opens ies.dam.client.chatticlient.controllers.chat.new_chat to javafx.fxml;
    exports ies.dam.client.chatticlient.controllers.chat.new_chat;
    opens ies.dam.client.chatticlient.controllers.chat.popup to javafx.fxml;
    exports ies.dam.client.chatticlient.controllers.chat.popup;
    exports ies.dam.client.chatticlient.controllers.startup.popup;
    opens ies.dam.client.chatticlient.controllers.startup.popup to javafx.fxml;
}