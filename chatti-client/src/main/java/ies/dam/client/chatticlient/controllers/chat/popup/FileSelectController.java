package ies.dam.client.chatticlient.controllers.chat.popup;

import ies.dam.client.chatticlient.ClientApplication;
import ies.dam.client.chatticlient.Scenes;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileSelectController {
    @FXML
    private Button fileButton;

    private File selectedFile;

    public void selectFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));

        Stage stage = (Stage) fileButton.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if(selectedFile != null) {
            fileButton.setText(selectedFile.getName());
        } else{
            fileButton.setText("Sin archivo");
        }
    }
    //TODO: Logic copied from a previous project. In it, it used to crash at the last file, corrupting it if there was more than 1 file sent in the same socket
    public void sendFile(ActionEvent actionEvent) {
        //Add logic to send the file to the server
        try {
            if(selectedFile != null) {
                int bytes = 0;
                FileInputStream fis = new FileInputStream(selectedFile);
                //Warn the server that a file is going to be sent
                ClientApplication.dataOutputStream.writeByte(-1); //Defaulted to -1 due to not knowing which byte will be used
                ClientApplication.dataOutputStream.flush();
                //Send the file's name
                ClientApplication.dataOutputStream.writeUTF(selectedFile.getName());
                ClientApplication.dataOutputStream.flush();
                //Divide the file in chunks
                byte[] buffer = new byte[4 * 1024]; //4 KB sized buffer
                while((bytes = fis.read(buffer)) != -1) {
                    ClientApplication.dataOutputStream.write(buffer, 0, bytes); //Send each block
                }
                ClientApplication.dataOutputStream.flush();
                fis.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientApplication.loadPopup(Scenes.FILE_SENT);
    }

    public void cancel(ActionEvent actionEvent) {
        ClientApplication.closePopup();
    }
}
