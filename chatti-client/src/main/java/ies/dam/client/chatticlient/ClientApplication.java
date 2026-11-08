package ies.dam.client.chatticlient;

import ies.dam.client.chatticlient.controllers.startup.WelcomeController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Map;

public class ClientApplication extends Application {
    public static Stage stage;  //Normal stage
    public static Stage popUpStage; //Stage for Popups
    private static String serverAddress; //Auto-descriptive. Unused
    private static int serverPort; //Auto-descriptive. Unused
    public static Socket socket; //Client's socket for user related purposes
    private static Map<String, String> settingsTemp; //Map for changed settings. Not implemented yet
    private static Map<String, String> settingsOriginal; //Map for original settings. Not implemented yet
    public static DataInputStream dataInputStream; //The inputStream of the socket that connects to the server
    public static DataOutputStream dataOutputStream; //The outputStream of the socket that connects to the server
    private static String username; //The username of the person using the client
    private static String hashedPassword; //The hashed password of the user, used to re-login automatically after regaining connection with the server
    private static boolean isPopUpOpen = false; //Boolean that indicates if a popUp is open. Used so that when closing the main window, any open popUp will also close along with it
    public static boolean connected = false; //Boolean that checks if the client is currently connected to the server
    public static boolean hasConnected = false; //Boolean that determines if the client has ever connected to a server during the app's current launch
    public static Scenes currentScene; //Tracks the current scene
    public static Boolean heartBeatStarted = false;

    @Override
    public void start(Stage stage) throws IOException {
        //Initializes the stages
        ClientApplication.stage = stage;
        ClientApplication.popUpStage =  new Stage();
        //Makes popUpStage not have the topBar, turning it into a proper popUp
        ClientApplication.popUpStage.initStyle(StageStyle.TRANSPARENT);
        //Loads the initial scene
        loadScene(Scenes.WELCOME_VIEW);
        //Sets certain actions that the app hs to do when closing it
        stage.setOnCloseRequest(event -> {
            try {
                WelcomeController.switchExit();
                if (dataOutputStream != null && dataInputStream != null) { //Closes the dataOutputStream and dataInputStream, warning the server about it
                    //Warns the server
                    dataOutputStream.writeByte(-1);
                    dataOutputStream.flush();
                    //Closes the inputStream and outputStream
                    dataOutputStream.close();
                    dataInputStream.close();
                }
                if(isPopUpOpen) { //Closes any open popUp
                    popUpStage.close();
                }
                stage.close();
                System.exit(0); //Kind of a failsafe in case any thread remains open
            } catch (IOException e) {
                System.out.println("Error: Tried to send -1 but server was closed.");
            }
        });
    }

    public static void main(String[] args) {
        launch();
    }

    public static void loadScene(Scenes enumScene){ //Loads scenes
        try {
            //Prepares the scene for the stage, and sets the properties of the stage
            FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("fxmls/".concat(enumScene.getFxmlName())));
            Scene scene = new Scene(fxmlLoader.load());
            currentScene = enumScene;
            stage.setTitle(enumScene.getName());
            stage.setMinHeight(enumScene.getHeight());
            stage.setHeight(enumScene.getHeight());
            stage.setMinWidth(enumScene.getWidth());
            stage.setWidth(enumScene.getWidth());
            // Uncomment if needed. (Not really needed)
            /*
            boolean nonResizableScene = enumScene == LoadableScenes.CONFIRM_FRAME || enumScene == Scenes.LOGIN_FRAME;
            stage.setResizable(!nonResizableScene);
             */
            //Shows the window
            stage.setScene(scene);
            stage.show();

        } catch (IOException e){
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    showAlert(Alert.AlertType.ERROR, "Error de conexión",
                            "No tienes conexión con el servidor",
                            "Si el problema continua, prueba lo siguiente: " +
                                    "\n-Espere un poco antes de intentarlo de nuevo. " +
                                    "\n-Reinicie la aplicación. " +
                                    "\n-Compruebe su conexión a internet. " +
                                    "\nSi el problema continua tras intentar lo anterior," +
                                    "\nes posible que el servidor no se encuentre activo");
                }
            });
            throw new RuntimeException(e);
        }
    }

    public static void closePopup(){ //Closes a popUp and indicates it to the app itself
        isPopUpOpen= false;
        popUpStage.close();
    }

    public static void loadPopup(Scenes enumScene){ //Opens or changes a popUp
        try {
            isPopUpOpen = true; //Indicates that a popUp is open
            //Prepares the scene for the stage
            FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("fxmls/".concat(enumScene.getFxmlName())));
            Scene scene = new Scene(fxmlLoader.load());
            //The two line below allow for that top line in the app's window to disappear, which means that it cannot be moved and needs an external way of closing that isn't that line's x button
            scene.setFill(Color.TRANSPARENT);
            //Adds the properties to the stage
            popUpStage.setTitle(enumScene.getName());
            popUpStage.setMinHeight(enumScene.getHeight());
            popUpStage.setHeight(enumScene.getHeight());
            popUpStage.setMinWidth(enumScene.getWidth());
            popUpStage.setWidth(enumScene.getWidth());
            // Uncomment if needed (Not really needed)
            /*
            boolean nonResizableScene = enumScene == LoadableScenes.CONFIRM_FRAME || enumScene == Scenes.LOGIN_FRAME;
            stage.setResizable(!nonResizableScene);
             */

            popUpStage.setScene(scene);
            popUpStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String hashedPassword(String password) { //Hashes the password
        String newHashedPassword;
        try{
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(password.getBytes("UTF-8"));
            final StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                final String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1){
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            newHashedPassword = hexString.toString();
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
        return newHashedPassword;
    }

    public static String newHashedPassword(byte[] salt, String password) {
        try {
            /*
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
             */
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : hash) {
                stringBuilder.append(String.format("%02x", b));
            }
            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e){
            throw new RuntimeException(e);
        }
    }

    /*
    Getter and Setter for the username property. Might not look useful, but it is
     */
    public static String getUsername(){
        return username;
    }

    public static void setUsername(String username){
        ClientApplication.username = username;
    }

    /*
    Getter and Setter for the hashed password property. Might not look useful, but it is
     */
    public static String getHashedPassword() {
        return hashedPassword;
    }

    public static void setHashedPassword(String hashedPassword) {
        ClientApplication.hashedPassword = hashedPassword;
    }

    /**
     * Method that opens a popup warning to the user when called
     * @param alertType Type of alert that it is (INFORMATION, ERROR, WARNING, NONE, CONFIRMATION)
     * @param title Title that the alert will have
     * @param header Header of the alert
     * @param message Main message of the alert. If it is too long, a "\n" is required for the full message to show
     */
    public static void showAlert(Alert.AlertType alertType, String title, String header, String message){
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Getter for the heartbeat system to close any popup that was previously open when regaining connection
     * @return Returns true if a popUp was open when regained the connection
     */
    public static boolean getIsPopUpOpen() {
        return isPopUpOpen;
    }
}