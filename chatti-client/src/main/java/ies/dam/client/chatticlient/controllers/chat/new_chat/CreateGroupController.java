package ies.dam.client.chatticlient.controllers.chat.new_chat;

import com.google.gson.Gson;
import ies.dam.client.chatticlient.ClientApplication;
import ies.dam.client.chatticlient.Scenes;
import ies.dam.info_adapter.model.InfoModel;
import ies.dam.info_adapter.three_columns.ThreeIconButtonCellFactory;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;

public class CreateGroupController {
    @FXML
    private TextField groupNameField;
    @FXML
    private TextField groupDescriptionField;
    @FXML
    private Circle groupImage;
    @FXML
    private ListView userRemoveListView;
    @FXML
    private TextField searchField;
    @FXML
    private ListView addUsersListView;

    protected ObservableList<InfoModel> usersOutOfGroupList = FXCollections.observableArrayList(); //List of users that haven't been added to the group
    protected ObservableList<InfoModel> usersInGroupList = FXCollections.observableArrayList(); //List of users that have been added to the group, and can be removed from it prior to creating it

    private Image image;
    private File chosenImage;

    public void initialize() {
        Gson gson = new Gson();
        Platform.runLater(() -> {
            image = new Image(Objects.requireNonNull(
                    ClientApplication.class.getResource("imgs/default.png")).toExternalForm()
            );
            groupImage.setFill(new ImagePattern(image));
        });
        try {
            //Defaults the group image
            chosenImage = new File(Objects.requireNonNull(ClientApplication.class.getResource("imgs/default.png")).toURI());
            System.out.println(chosenImage.getName());

            //Warns the server that you've entered this scene
            ClientApplication.dataOutputStream.writeByte(Scenes.CREATE_GROUP.getSignal());
            ClientApplication.dataOutputStream.flush();

            //Setups both ListViews so they can have custom cells
            addUsersListView.setCellFactory(new ThreeIconButtonCellFactory());
            userRemoveListView.setCellFactory(new ThreeIconButtonCellFactory());

            //Logic copied from Chattable.java, might need library changes to adapt this better for displaying and handling
            Map<String, String> acc_user_map = gson.fromJson(ClientApplication.dataInputStream.readUTF(), Map.class);
            Map<String, String> pfp_map = gson.fromJson(ClientApplication.dataInputStream.readUTF(), Map.class);
            ArrayList<InfoModel> infoModels = new ArrayList();

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
                infoModels.add(new InfoModel(image, String.format("%s@%s", name, identifier_name), null, String.valueOf(ClientApplication.class.getResource("imgs/TickUser.png"))));
            });

            for(InfoModel info : infoModels) {
                EventHandler<ActionEvent> addToGroup = new EventHandler<ActionEvent>() { //Logic to add a user to a group before it's creation
                                @Override
                                public void handle(ActionEvent event) {
                                    //Logic to add this item to another listview and remove it from this one
                                    addToGroupList(info);
                                }
                            };
                EventHandler<ActionEvent> removeFromGroup = new EventHandler<ActionEvent>() { //Logic to remove a user from a group before it's creation
                                @Override
                                public void handle(ActionEvent event) {
                                    //Logic to add this item to another listview and remove it from this one
                                    removeFromGroup(info);
                                }
                            };
                info.setEvent(addToGroup);
                info.setSecondEvent(removeFromGroup);
            }
            /*This section takes the data of the data of the "usersOutOfGroupList" ObservableList, and puts it in a FilteredList, who reacts to the data in the searchField
              due to a listener in the searchField*/
            FilteredList<InfoModel> filteredList = new FilteredList<>(usersOutOfGroupList, s->true);
            searchField.textProperty().addListener(observable ->{
                String filter = searchField.getText();
                if(filter == null || filter.isEmpty()){
                    filteredList.setPredicate(s -> true);
                } else{
                    filteredList.setPredicate(s -> s.getName().toLowerCase().contains(filter.toLowerCase()));
                }
            });
            addUsersListView.setItems(filteredList);
            userRemoveListView.setItems(usersOutOfGroupList);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void selectImage(ActionEvent actionEvent) { //Allows to select a local image to set as the group's image
        try {
            FileChooser pfpChooser = new FileChooser();
            pfpChooser.setTitle("Open Resource File");
            pfpChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
            chosenImage = pfpChooser.showOpenDialog(ClientApplication.stage);
            if (chosenImage != null) {
                image = new Image(new FileInputStream(chosenImage));
                groupImage.setFill(new ImagePattern(image));
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void createGroup(ActionEvent actionEvent) {
        Gson gson = new Gson();
        if(groupNameField.getText().isEmpty()) { //Checks if the name is empty. If it is, the group cannot be created
           return;
        }
        try {
            //This turns the data into a Map of Strings
            Map<String, String> map = Map.of(
                    "group_name", groupNameField.getText(),
                    "group_desc", groupDescriptionField.getText(),
                    "group_img", chosenImage.getName().split("\\.")[1]
            );
            String jsonMap = gson.toJson(map);
            //Logic to send the jsonMap to the server
            ClientApplication.dataOutputStream.writeUTF(jsonMap);
            ClientApplication.dataOutputStream.flush();

            //Logic to send the image to the server as a byte array
            byte[] imageBytes = FileUtils.readFileToByteArray(chosenImage);
            ClientApplication.dataOutputStream.write(imageBytes);
            ClientApplication.dataOutputStream.flush();

            //Logic to send the users that have been added to the group. Not sure if this works properly because code with me is so fucking inconsistent, so it's commented so it doesn't cause any problems
            //I bet a kiss in the that this actually doesn't end up running. A map will most likely be the way, even if that's still wrong
            //TODO: Quick reminder to whoever does this segment to also add the dude creating the group to the group, because if not, this is gonna become a shithole of being annoying to others by adding them to random groups
            /*Type type = new TypeToken<List<InfoModel>>() {}.getType();
            String json = gson.toJson(usersInGroupList, type);
            byte[] usersBytes = json.getBytes();
            ClientApplication.dataOutputStream.write(imageBytes);
            ClientApplication.dataOutputStream.flush();*/

            //Add logic to receive from the server if the group was successfully created
            byte code = ClientApplication.dataInputStream.readByte();
            System.out.println(code);
            switch(code){
                case -1 -> { //Hopefully this doesn't happen, but it's possible if something goes wrong in the server
                    System.out.println("Error al crear grupo");
                }
                case 0 -> {  //Group successfully created, what a happy day
                    writeExitByte();
                    ClientApplication.loadScene(Scenes.GROUP_CHAT_VIEW);
                }
            }
            //Once the group is created, it makes sense that it returns back to the group view
            writeExitByte();
            ClientApplication.loadScene(Scenes.GROUP_CHAT_VIEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    //TODO: Just like with AddContactController, I expect this to freeze the app. Might break because the server receives an unknown byte as this scene isn't implemented in the server yet
    public void goBack(ActionEvent actionEvent) {
        writeExitByte();
        ClientApplication.loadScene(Scenes.GROUP_CHAT_VIEW);
    }

    public void clearBar(ActionEvent actionEvent) { //Clears the searchBar
        searchField.setText("");
    }
    
    private void writeExitByte(){ //Tells the server that the client is leaving the scene
        try {
            ClientApplication.dataOutputStream.writeByte(-2);
            ClientApplication.dataOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addToGroupList(InfoModel info) {
        //Removes the user from their current list
        usersOutOfGroupList.remove(info);
        //Flips their InfoModel's events
        EventHandler<ActionEvent> newMainEvent = info.getSecondEvent();
        EventHandler<ActionEvent> newSecondEvent = info.getEvent();
        info.setEvent(newMainEvent);
        info.setSecondEvent(newSecondEvent);
        //Adds the user to the other list and orders the list
        usersInGroupList.add(info);
        usersInGroupList.sort(Comparator.comparing(InfoModel::getName));
    }
    public void removeFromGroup(InfoModel info) {
        //Removes the user from their current list
        usersInGroupList.remove(info);
        //Flips their InfoModel's events
        EventHandler<ActionEvent> newMainEvent = info.getSecondEvent();
        EventHandler<ActionEvent> newSecondEvent = info.getEvent();
        info.setEvent(newMainEvent);
        info.setSecondEvent(newSecondEvent);
        //Adds the user to the other list and orders the list
        usersOutOfGroupList.add(info);
        usersOutOfGroupList.sort(Comparator.comparing(InfoModel::getName));
    }
}
