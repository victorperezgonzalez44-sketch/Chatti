package ies.dam.client.chatticlient.controllers.chat.new_chat;

import com.google.gson.Gson;
import ies.dam.client.chatticlient.ClientApplication;
import ies.dam.client.chatticlient.Scenes;
import ies.dam.info_adapter.four_columns.FourDoubleIconButtonCellFactory;
import ies.dam.info_adapter.model.InfoModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

public class AddContactController {
    @FXML
    private TextField accountNameField;
    @FXML
    private Label infoLabel;
    @FXML
    private ListView requestListView;

    protected ObservableList<InfoModel> requestList;

    public void initialize() {
        Gson gson = new Gson();
        try {
            infoLabel.setVisible(false); //This label works as a warning and thus should not be visible at the start
            //Warns the server that the client is in the current scene
            ClientApplication.dataOutputStream.writeByte(Scenes.ADD_CONTACT.getSignal());
            ClientApplication.dataOutputStream.flush();

            //Prepares the listview so that it can hold custom cells, and an array with object that'll later fill the listview
            requestListView.setCellFactory(new FourDoubleIconButtonCellFactory());
            ArrayList<InfoModel> infoModels = new ArrayList<>();

            //Logic to obtain the requests
            Map<String, String> acc_user_map = gson.fromJson(ClientApplication.dataInputStream.readUTF(), Map.class);
            System.out.println(acc_user_map.toString());
            byte[] jsonBytes = new byte[ClientApplication.dataInputStream.readInt()];
            ClientApplication.dataInputStream.readFully(jsonBytes);
            Map <String, String> pfp_map = gson.fromJson(new String(jsonBytes, StandardCharsets.UTF_8), Map.class);
            System.out.println(pfp_map.toString());

            //Reads the map with the profile pictures and handles the images
            pfp_map.forEach((key, value) -> {
                String[] bytes_rep = value.substring(1, value.length() - 1).split(", ");
                byte[] bytes = new byte[bytes_rep.length];
                for (int i = 0; i < bytes_rep.length; i++) {
                    bytes[i] = Byte.parseByte(bytes_rep[i]);
                }

                //Creates the custom ListView cell
                Image image = new Image(new ByteArrayInputStream(bytes));
                String identifier_name = key.substring(0, key.lastIndexOf("."));
                String name = acc_user_map.get(identifier_name);
                InfoModel infoModel = new InfoModel(image, name, null, new Image(String.valueOf(ClientApplication.class.getResource("imgs/TickUser.png"))), null, new Image(String.valueOf(ClientApplication.class.getResource("imgs/CrossUser.png"))));
                infoModel.setThirdColumnText(identifier_name);
                infoModels.add(infoModel);
            });

            //Applying the events to the object
            for(InfoModel info : infoModels) {
                EventHandler<ActionEvent> accept = new EventHandler<ActionEvent>() { //Logic to accept a request
                    @Override
                    public void handle(ActionEvent event) {
                        try {
                            //Warns the server that a request is going to be accepted
                            ClientApplication.dataOutputStream.writeByte(109);
                            ClientApplication.dataOutputStream.flush();
                            //Tells the server which request was accepted
                            ClientApplication.dataOutputStream.writeUTF(info.getThirdColumnText());
                            ClientApplication.dataOutputStream.flush();
                            //Removing the user from the list
                            requestList.remove(info);
                        }catch (IOException e){
                            ClientApplication.showAlert(Alert.AlertType.ERROR, "Error de conexión",
                                    "No tienes conexión con el servidor",
                                    "Si el problema continua, prueba lo siguiente: " +
                                            "\n-Reinicie la aplicación. " +
                                            "\n-Compruebe su conexión a internet. " +
                                            "\nSi el problema continua tras intentar lo anterior," +
                                            "\nes posible que el servidor no se encuentre activo");
                            throw new RuntimeException(e);
                        }
                    }
                };
                EventHandler<ActionEvent> reject = new EventHandler<ActionEvent>() { //Logic to reject a request
                    @Override
                    public void handle(ActionEvent event) {
                        try {
                            //Warns the server that a request is going to be rejected
                            ClientApplication.dataOutputStream.writeByte(110);
                            ClientApplication.dataOutputStream.flush();
                            //Tells the server which request was rejected
                            ClientApplication.dataOutputStream.writeUTF(info.getThirdColumnText());
                            ClientApplication.dataOutputStream.flush();

                            //Removing the user from the list
                            requestList.remove(info);
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
                };
                //Adds the events to allow the cells to accept and reject the requests
                info.setEvent(accept);
                info.setSecondEvent(reject);
            }
            //Adds the requests to the requestList and then to the ListView
            requestList = FXCollections.observableArrayList();
            requestList.addAll(infoModels);
            requestListView.setItems(requestList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void sendRequest(ActionEvent event) {
        Gson gson = new Gson();
        try {
            if (accountNameField.getText().isEmpty()) { //Checks that the user has written an accountName before sending the request
                infoLabel.setVisible(true);
                infoLabel.setText("Intruzca el nombre de un usuario");
            } else {
                //Warns the server that a request is going to be sent
                ClientApplication.dataOutputStream.writeByte(111);
                ClientApplication.dataOutputStream.flush();
                //Logic to send the request.
                ClientApplication.dataOutputStream.writeUTF(accountNameField.getText());
                ClientApplication.dataOutputStream.flush();

                //Checks if the request was valid
                Byte success = ClientApplication.dataInputStream.readByte();
                switch (success) {
                    case -1 -> { //This should be for nonexistent users or ones that you already have in your contact list
                        infoLabel.setVisible(true);
                        infoLabel.setText("Usuario no válido");
                    }
                    case 0 -> { //This should only be for existing users that you do not have direct contact with
                        infoLabel.setVisible(true);
                        infoLabel.setText("Solicitud enviada");
                        accountNameField.setText("");
                    } case 1 -> { //This should only be for when you input the name of a user that has sent you a request that hasn't been accepted/rejected yet
                        infoLabel.setVisible(true);
                        infoLabel.setText("La solicitud pendiente de "+ accountNameField.getText()+" ha sido aceptada");
                        for (InfoModel info : requestList) {
                            if (info.getThirdColumnText().equals(accountNameField.getText())) {
                                requestList.remove(info);
                            }
                        }
                        accountNameField.setText("");
                    }
                }
            }
        } catch (IOException e) {
            ClientApplication.showAlert(Alert.AlertType.ERROR, "Error de conexión",
                    "No tienes conexión con el servidor",
                    "Si el problema continua, prueba lo siguiente: " +
                            "\n-Reinicie la aplicación. " +
                            "\n-Compruebe su conexión a internet. " +
                            "\nSi el problema continua tras intentar lo anterior," +
                            "\nes posible que el servidor no se encuentre activo");
            throw new RuntimeException(e);
        }
    }
    @FXML
    public void goBack(ActionEvent event) {
        writeExitByte();
        ClientApplication.loadScene(Scenes.CHAT_VIEW);
    }

    private void writeExitByte(){ //Warns the server that the client is leaving the scene
        try {
            ClientApplication.dataOutputStream.writeByte(-2);
            ClientApplication.dataOutputStream.flush();
        } catch (IOException e) {
            ClientApplication.showAlert(Alert.AlertType.ERROR, "Error de conexión",
                    "No tienes conexión con el servidor",
                    "Si el problema continua, prueba lo siguiente: " +
                            "\n-Reinicie la aplicación. " +
                            "\n-Compruebe su conexión a internet. " +
                            "\nSi el problema continua tras intentar lo anterior," +
                            "\nes posible que el servidor no se encuentre activo");
            throw new RuntimeException(e);
        }
    }
}