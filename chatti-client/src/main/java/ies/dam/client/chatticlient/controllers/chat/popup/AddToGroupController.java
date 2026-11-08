package ies.dam.client.chatticlient.controllers.chat.popup;

import com.google.gson.Gson;
import ies.dam.client.chatticlient.ClientApplication;
import ies.dam.info_adapter.three_columns.ThreeIconButtonCellFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import ies.dam.info_adapter.model.InfoModel;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class AddToGroupController {
    @FXML
    private TextField SearchField;
    @FXML
    private ListView usersList;

    public void initialize() {
        Gson gson = new Gson();
        try {
            ObservableList<InfoModel> observableList = FXCollections.observableArrayList();
            //Logic to receive the users copied from Chattable.java. Library might need some changes

            //Gets a map with the layout of the users and profile pictures?
            Map<String, String> acc_user_map = gson.fromJson(ClientApplication.dataInputStream.readUTF(), Map.class);
            Map<String, String> pfp_map = gson.fromJson(ClientApplication.dataInputStream.readUTF(), Map.class);
            ArrayList<InfoModel> infoModels = new ArrayList<>();

            //Reads the map with the profile pictures and handles the images
            pfp_map.forEach((key, value) -> {
                String[] bytes_rep = key.substring(1, key.length() - 1).split(", ");
                byte[] bytes = new byte[bytes_rep.length];
                for (int i = 0; i < bytes_rep.length; i++) {
                    bytes[i] = Byte.parseByte(bytes_rep[i]);
                }

                //Creates the custom ListView cell
                Image image = new Image(new ByteArrayInputStream(bytes));
                String identifier_name = value.substring(0, value.lastIndexOf("."));
                String name = acc_user_map.get(identifier_name);
                infoModels.add(new InfoModel(image, String.format("%s@%s", name, identifier_name)));
            });
            for(InfoModel info : infoModels) { //Reads all the objects in the InfoModel array and adds the events to them
                EventHandler<ActionEvent> addUserToGroup = new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        //Logic to add a user to a group
                        try{
                            //Warns the server that there will be someone added to a group
                            ClientApplication.dataOutputStream.writeByte(-1); //CHANGE THIS WITH WHATEVER BYTE IS GOING TO BE USED. DEFAULTED WITH -1 TO AVOID COMPILATION ERRORS
                            ClientApplication.dataOutputStream.flush();
                            //Tells the server who is going to be added (MOST LIKELY IMPORTANT: ALSO ADD THE GROUP ID SO IT CAN RECOGNISE WHICH GROUP TO ADD THE USER. MIGHT CHANGE THE LIBRARY FOR THAT)
                            //(SECOND NOTE: I'VE JUST REALISED THAT THE CLIENT TELLS THE SERVER IN WHICH GROUP IT'S LOCATED WITH THE CONVOLIST'S LISTENER, SO MAYBE WHAT I WROTE ABOVE ISN'T NECESSARY. GONNA KEEP THAT NOTE FOR NOW THO)
                            Map<String, String> map = Map.of(
                                    "account_name", info.getName()
                            );
                            String jsonMap = gson.toJson(map);
                            ClientApplication.dataOutputStream.writeUTF(jsonMap);
                            ClientApplication.dataOutputStream.flush();
                            //Removes the added user from the list
                            observableList.remove(info);
                        }catch (IOException e){
                            throw new RuntimeException(e);
                        }
                    }
                };
                info.setEvent(addUserToGroup);
            }
            observableList.addAll(infoModels);
            //The next section is used to filter and show certain users based if what you're writing appears somewhere in their username
            FilteredList<InfoModel> filteredList = new FilteredList<>(observableList, infoModel -> true);
            SearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                String filter = SearchField.getText();
                if (filter == null || filter.isEmpty()) {
                    filteredList.setPredicate(infoModel -> true);
                } else {
                    filteredList.setPredicate(infoModel -> infoModel.getName().toLowerCase().contains(filter.toLowerCase()));
                }
            });
            //Prepares the userList to have the custom cells and fills it
            usersList.setCellFactory(new ThreeIconButtonCellFactory());
            usersList.setItems(filteredList);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void clearField(ActionEvent actionEvent) { //You won't be surprised, but this method... just clears the searchbar. That's it
        SearchField.clear();
    }
}