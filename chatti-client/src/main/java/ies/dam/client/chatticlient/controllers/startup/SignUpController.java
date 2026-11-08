package ies.dam.client.chatticlient.controllers.startup;

import com.google.gson.Gson;
import ies.dam.client.chatticlient.ClientApplication;
import ies.dam.client.chatticlient.Scenes;

import java.io.*;
import java.lang.ref.Cleaner;
import java.net.URISyntaxException;
import ies.dam.status_component.StatusComponent;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.apache.commons.io.FileUtils;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class SignUpController {
    @FXML
    public TextField accountNameField;
    @FXML
    public Label accNameWarning;
    @FXML
    public Circle profilePic;
    @FXML
    public PasswordField passField;
    @FXML
    public TextField usernameField;
    @FXML
    public TextField mailField;
    @FXML
    public PasswordField confirmPassField;
    @FXML
    public TextArea descriptionArea;
    @FXML
    public StatusComponent statusComponent;
    @FXML
    public Button createAcc;

    private Image image;
    private byte[] defaultImageBytes;
    private File chosenImage;

    public void initialize() {
        try {
            try (InputStream in = ClientApplication.class.getResourceAsStream("imgs/default.png")) {
                defaultImageBytes = in.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Platform.runLater(() -> { //Defaults the profile picture and the name warning
                image = new Image(new ByteArrayInputStream(defaultImageBytes));
                profilePic.setFill(new ImagePattern(image));
                accNameWarning.setVisible(false);
            });
            // TODO: Deprecated
            System.out.printf("Faulty URI: %s%n", Objects.requireNonNull(ClientApplication.class.getResource("imgs/default.png")).toExternalForm());
            chosenImage = new File(Objects.requireNonNull(ClientApplication.class.getResource("imgs/default.png")).toExternalForm());
            System.out.println(chosenImage.getName());

            //Sends a warning to the server that the client is in the SignUp scene
            ClientApplication.dataOutputStream.writeByte(Scenes.SIGNUP_VIEW.getSignal());
            ClientApplication.dataOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method opens a file selection window.
     * */
    @FXML
    public void changePfp(ActionEvent actionEvent) {
        try {
            FileChooser pfpChooser = new FileChooser();
            pfpChooser.setTitle("Open Resource File");
            pfpChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
            chosenImage = pfpChooser.showOpenDialog(ClientApplication.stage);
            if (chosenImage != null) {
                FileInputStream fileInputStream = new FileInputStream(chosenImage);
                image = new Image(new FileInputStream(chosenImage));
                defaultImageBytes = fileInputStream.readAllBytes();
                profilePic.setFill(new ImagePattern(image));
                fileInputStream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void cancelCreate(ActionEvent actionEvent) {
        writeExitByte();
        ClientApplication.loadScene(Scenes.WELCOME_VIEW);
    }

    private void writeExitByte(){
        try {
            ClientApplication.dataOutputStream.writeByte(-2);
            ClientApplication.dataOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void createAcc(ActionEvent actionEvent) {
        accNameWarning.setVisible(false);
        if (uncheckedFields()) {
            return;
        }
        try {
            //Section that gets the info in the text based fields and sends it to the server
            ClientApplication.dataOutputStream.writeByte(101);
            ClientApplication.dataOutputStream.flush();

            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            ClientApplication.dataOutputStream.write(salt);
            ClientApplication.dataOutputStream.flush();

            Gson gson = new Gson();
            Map<String, String> map = Map.of(
                    "_name", accountNameField.getText(),
                    "us_name", usernameField.getText(),
                    "email", mailField.getText(),
                    "pass_field", ClientApplication.newHashedPassword(salt, passField.getText()),
                    "desc", descriptionArea.getText(),
                    ".ext", chosenImage.getName().split("\\.")[1]
            );
            String jsonMap = gson.toJson(map);
            ClientApplication.dataOutputStream.writeUTF(jsonMap);
            ClientApplication.dataOutputStream.flush();

            //Section that sends the image to the server
            byte[] bytes = defaultImageBytes;
            System.out.println("Bytes sent: ");
            System.out.println(Arrays.toString(bytes));
            ClientApplication.dataOutputStream.writeInt(bytes.length);
            ClientApplication.dataOutputStream.flush();
            ClientApplication.dataOutputStream.write(bytes);
            ClientApplication.dataOutputStream.flush();

            //Section that manages if the account cannot be created because one with the same key properties already exists
            byte code = ClientApplication.dataInputStream.readByte();
            System.out.printf("Received byte from server: %s.%n", code);
            switch(code) { //Received from the server and indicates if the account already existed or not
                case -1 -> { //Account already existed
                    accNameWarning.setVisible(true);
                    ClientApplication.showAlert(Alert.AlertType.INFORMATION,
                            "Cuenta ya existente",
                            "Esta cuenta ya existe",
                            "Por favor, use otro nombre de cuenta");
                    System.out.println("DUPLICATE REGISTRY");
                }
                case 0 -> { //Account created
                    writeExitByte();
                    ClientApplication.loadScene(Scenes.WELCOME_VIEW);
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

    /**
     * Method that manages warnings for empty fields
     * @return True if there are empty fields, false if there aren't
     */
    private boolean uncheckedFields() {
        if (passField.getText().isEmpty() && confirmPassField.getText().isEmpty()) {
            accNameWarning.setText("No hay contraseña.");
            accNameWarning.setVisible(true);
            return true;
        } else if (!passField.getText().equals(confirmPassField.getText())) {
            accNameWarning.setText("Las contraseñas no son iguales.");
            accNameWarning.setVisible(true);
            return true;
        } else if (accountNameField.getText().isEmpty()) {
            accNameWarning.setText("Ingrese un nombre para su cuenta.");
            accNameWarning.setVisible(true);
            return true;
        } else if (usernameField.getText().isEmpty()) {
            accNameWarning.setText("Ingrese un nombre de usuario.");
            accNameWarning.setVisible(true);
            return true;
        } else if (mailField.getText().isEmpty() || mailField.getText().contains("@.*\\.com")) {
            accNameWarning.setText("Ingrese un email válido.");
            accNameWarning.setVisible(true);
            return true;
        }
        return false;
    }
}
