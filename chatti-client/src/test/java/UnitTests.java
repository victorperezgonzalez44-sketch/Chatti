import ies.dam.client.chatticlient.ClientApplication;
import ies.dam.client.chatticlient.Scenes;
import ies.dam.client.chatticlient.controllers.startup.WelcomeController;
import ies.dam.info_adapter.model.InfoModel;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>Test class</h1>
 * <h3>This class contains test for functions that have tendencies to explode.</h3>
 * <h3>Some tests might require some setup before running them, will be indicated before their method.</h3>
 * <h3>The tests are as followed:</3>
 * <p>-Register and log in</p>
 * <p>-Registering a duplicate</p>
 * <p>-Logging in with wrong credentials</p>
 * <p>-Sending a message</p>
 * <p>-Sending a contact/friend request</p>
 * <p>-Sending a bad contact/friend request</p>
 * <p>-Deleting a contact/friend</p>
 */
class UnitTests extends ApplicationTest {

    private static boolean isPopUpOpen = false; //Soo... If you try to turn the use of this into the use of ClientApplication's isPopUpOpen, the IDE gets angry and throws a compilation error. This is because it is private

    @Override
    public void start(Stage stage) throws IOException {
        //Initializes the stages
        ClientApplication.stage = stage;
        ClientApplication.popUpStage =  new Stage();
        //Makes popUpStage not have the topBar, turning it into a proper popUp
        ClientApplication.popUpStage.initStyle(StageStyle.TRANSPARENT);
        //Loads the initial scene
        ClientApplication.loadScene(Scenes.WELCOME_VIEW);
        //Sets certain actions that the app hs to do when closing it
        stage.setOnCloseRequest(event -> {
            try {
                WelcomeController.switchExit();
                if (ClientApplication.dataOutputStream != null && ClientApplication.dataInputStream != null) { //Closes the dataOutputStream and dataInputStream, warning the server about it
                    //Warns the server
                    ClientApplication.dataOutputStream.writeByte(-1);
                    ClientApplication.dataOutputStream.flush();
                    //Closes the inputStream and outputStream
                    ClientApplication.dataOutputStream.close();
                    ClientApplication.dataInputStream.close();
                }
                if(isPopUpOpen) { //Closes any open popUp
                    ClientApplication.popUpStage.close();
                }
                ClientApplication.stage.close(); //THIS FUCKING LINE DOES ABSOLUTELY NOTHING, IT JUST DOES SOMETHING THAT THE CLOSE BUTTON DOES BY DEFAULT
            } catch (IOException e) {
                System.out.println("Error: Tried to send -1 but server was closed.");
            }
        });
    }

    public <T extends Node> T find(final String query){
        return (T) lookup(query).queryAll().iterator().next();
    }

    /**
     * Registering and doing a log in test. Requires: Any account by the account name of test_registro_e_inicio to be deleted before launching it
     */
    @Test
    @DisplayName("Register an account and login with it")
    void registerAndLoginTest(){
        clickOn("#signup");
        TextField usernameField = find("#usernameField");
        usernameField.setText("SoyUnTest");
        TextField accountField = find("#accountNameField");
        accountField.setText("test_registro_e_inicio");
        PasswordField passwordField = find("#passField");
        passwordField.setText("Test_Password");
        PasswordField confPassField = find("#confirmPassField");
        confPassField.setText("Test_Password");
        clickOn("#createAcc");
        assertNotEquals(Scenes.SIGNUP_VIEW.getName(), ClientApplication.stage.getTitle());
        clickOn("#login");
        TextField userfield = find("#userField");
        userfield.setText("test_registro_e_inicio");
        TextField passField = find("#passField");
        passField.setText("Test_Password");
        clickOn("#login");
        assertEquals(Scenes.CHAT_VIEW.getName(), ClientApplication.stage.getTitle());
    }

    /**
     * Test to check if you're not allowed to create an account with the same account name twice. Requires: any account by the account name of test_duplicado to be deleted before launching it
     */
    @Test
    @DisplayName("Duplicate Registry")
    void duplicateRegister(){
        clickOn("#signup");
        TextField usernameField = find("#usernameField");
        usernameField.setText("SoyUnTest2");
        TextField emailField = find("#mailField");
        emailField.setText("test_email@gmail.com");
        TextField accountField = find("#accountNameField");
        accountField.setText("test_duplicado");
        PasswordField passwordField = find("#passField");
        passwordField.setText("Test_Password");
        PasswordField confPassField = find("#confirmPassField");
        confPassField.setText("Test_Password");
        clickOn("#createAcc");
        assertNotEquals(Scenes.SIGNUP_VIEW.getName(), ClientApplication.stage.getTitle());
        clickOn("#signup");
        TextField usernameField2 = find("#usernameField");
        usernameField2.setText("SoyUnTest2");
        TextField accountField2 = find("#accountNameField");
        accountField2.setText("test_duplicado");
        PasswordField passwordField2 = find("#passField");
        passwordField2.setText("Test_Password");
        PasswordField confPassField2 = find("#confirmPassField");
        confPassField2.setText("Test_Password");
        clickOn("#createAcc");
        assertEquals(Scenes.SIGNUP_VIEW.getName(), ClientApplication.stage.getTitle());
    }

    /**
     * Test to check if you can't log in with the wrong credentials. Doesn't require setup
     */
    @Test
    @DisplayName("Wrong Login")
    void wrongLogin(){
        clickOn("#login");
        TextField userField = find("#userField");
        userField.setText("NON-EXISTENT-USER");
        TextField passField = find("#passField");
        passField.setText("NON-EXISTENT-PASSWORD");
        clickOn("#login");
        assertEquals(Scenes.LOGIN_VIEW.getName(), ClientApplication.stage.getTitle());
    }

    /**
     * Test to send a friend request to an existing user. Requires: 2 accounts to exist, one with account name "test_friend" and other with "test_friend2"
     */
    @Test
    @DisplayName("Send correct request")
    void correctRequest(){
        clickOn("#login");
        TextField userField = find("#userField");
        TextField passField = find ("#passField");
        userField.setText("test_friend");
        passField.setText("123");
        clickOn("#login");
        clickOn("#newchat");
        TextField accNameField = find("#accountNameField");
        accNameField.setText("test_friend2");
        clickOn("#send");
        Label infoLabel = find("#infoLabel");
        assertEquals("Solicitud enviada", infoLabel.getText());
    }

    /**
     * Test to send a friend request to a non-valid user. Requires: an accounts to exist, one with account name "test_friend" and that there's no account with account name "test_friend_nonexistent", or that it already is a contact of the former account
     */
    @Test
    @DisplayName("Send incorrect request")
    void incorrectRequest(){
        clickOn("#login");
        TextField userField = find("#userField");
        TextField passField = find ("#passField");
        userField.setText("test_friend");
        passField.setText("123");
        clickOn("#login");
        clickOn("#newchat");
        TextField accNameField = find("#accountNameField");
        accNameField.setText("test_friend_nonexistent");
        clickOn("#send");
        Label infoLabel = find("#infoLabel");
        assertEquals("Usuario no válido", infoLabel.getText());
    }

    /**
     * Test to check if a user has received a request. Requires: To run "Send Correct Request" test previously
     */
    @Test
    @DisplayName("Request received correctly")
    void requestReceived(){ //Always run after successful correct request
        clickOn("#login");
        TextField userField = find("#userField");
        TextField passField = find ("#passField");
        userField.setText("test_friend2");
        passField.setText("123");
        clickOn("#login");
        clickOn("#newchat");
        ListView<InfoModel> listView = find("#requestListView");
        ObservableList<InfoModel> observableList = listView.getItems();
        assertFalse(observableList.isEmpty());
    }

    /* I am lost on how to click on a specific item's specific button. Will have to test it manually until I find it out
    @Test
    @DisplayName("Request accepted")
    void requestAccepted(){
        clickOn("#login");
        TextField userField = find("#userField");
        TextField passField = find ("#passField");
        userField.setText("test_friend2");
        passField.setText("123");
        clickOn("#login");
        ListView<InfoModel> convoList = find("#convoList");
        int initialSize = convoList.getItems().toArray().length;
        clickOn("#newchat");
        ListView<InfoModel> listView = find("#requestView");

    }*/

    /* I am lost on how to click on a specific item's specific button. Will have to test it manually until I find it out
    @Test
    @DisplayName("Request rejected")
    void requestRejected(){
        clickOn("#login");
        TextField userField = find("#userField");
        TextField passField = find ("#passField");
        userField.setText("test_friend2");
        passField.setText("123");
        clickOn("#login");
        ListView<InfoModel> convoList = find("#convoList");
        int initialSize = convoList.getItems().toArray().length;
        clickOn("#newchat");
        ListView<InfoModel> listView = find("#requestView");

    }*/

    /**
     * Test to send a message to a user (Prioritizes the first user in the contact list). Requires: A user by the account name of "chat_testA" and that this user has someone on the contact list
     */
    @Test
    @DisplayName("Sending Message")
    void sendMessage(){
        clickOn("#login");
        TextField userField = find("#userField");
        TextField passField = find ("#passField");
        userField.setText("chat_testA");
        passField.setText("123");
        clickOn("#login");
        ListView<InfoModel> convoList = find("#convoList");
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                convoList.getSelectionModel().selectFirst();
            }
        });
        VBox chatBox = find("#chatBox");
        int previousAmountOfMessages = chatBox.getChildren().toArray().length;
        TextField messageField = find("#messageField");
        messageField.setText("Test_12345");
        clickOn("#send");
        assertNotEquals(previousAmountOfMessages, chatBox.getChildren().toArray().length);
    }

    /**
     * Test to check if a user is deleted correctly from the contact list (Prioritizes the last user in the contact list). Requires: A user by the account name of "chat_testA" and that this user has someone on the contact list
     */
    @Test
    @DisplayName("Delete Contact")
    void deleteContact(){
        clickOn("#login");
        TextField userField = find("#userField");
        TextField passField = find ("#passField");
        userField.setText("chat_testA");
        passField.setText("123");
        clickOn("#login");
        ListView<InfoModel> convoList = find("#convoList");
        int previousAmount = convoList.getItems().toArray().length;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                convoList.getSelectionModel().selectLast();
            }
        });
        clickOn("#delete");
        clickOn("#confirmDelete");
        ListView<InfoModel> convoList2 = find("#convoList");
        assertNotEquals(previousAmount, convoList2.getItems().toArray().length);
    }
}
