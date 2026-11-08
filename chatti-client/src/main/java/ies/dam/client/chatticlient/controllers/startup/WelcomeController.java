package ies.dam.client.chatticlient.controllers.startup;

import ies.dam.client.chatticlient.ClientApplication;
import ies.dam.client.chatticlient.Scenes;
import ies.dam.status_component.StatusComponent;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class WelcomeController {

    private static boolean exit = false;

    @FXML
    public StatusComponent statusComponent;

    public void initialize(){
        /*
        if(!ClientApplication.heartBeatStarted){
            Thread heartbeat = new Thread(this::heartBeat); //Initializes the makeConnect method as a thread
            heartbeat.setDaemon(true);
            heartbeat.start();
        }
         */
        new Thread(this::makeConnect).start();
    }

    /**
     * This method constantly attempts to connect to the server until it manages to connect
     * TODO:This method should be deleted. I left it in case something breaks and this can be used to temporally fix it
     */
    private void makeConnect(){
        boolean hasConnected = false;
        while (!hasConnected) {
            try {
                if (exit){
                    break;
                }
                if (ClientApplication.socket == null) {
                    //ClientApplication.socket = new Socket("10.12.0.68", 1234); //Creates the connection socket
                    ClientApplication.socket = new Socket("localhost", 1234); //Wrote this line for both in the final version when presenting and for testing myself -Héctor
                    //Sets the inputStream and outputStream of the socket as an app's global inputStream and outputStream
                    ClientApplication.dataInputStream = new DataInputStream(ClientApplication.socket.getInputStream());
                    ClientApplication.dataOutputStream = new DataOutputStream(ClientApplication.socket.getOutputStream());
                }
                hasConnected = true; //Indicates that the socket has connected
                //Displays an indication that the client has connected
                updateConnectionStatus("Connected");
                System.out.println("Connected");
            } catch (IOException e) {
                try {
                    //Displays an indication that the socket is not connected
                    updateConnectionStatus("Not Connected");
                    System.out.println("Not connected");
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    /**
     * Working heartbeat + reconnect method
     */
    public void heartBeat(){
        //Creates some variables that'll be used later
        Socket heartbeat;
        DataOutputStream dataOutputStreamHeartbeat = null;
        DataInputStream dataInputStreamHeartbeat = null;
        ClientApplication.heartBeatStarted = true;
        while (true){ //Loops until the app closes
            try {
                while (!ClientApplication.connected){ //Constantly tries to establish a connection with the server until it does (This one will be used for heartbeats)
                    heartbeat = new Socket("localhost", 1234);
                    //heartbeat = new Socket("10.12.0.68", 1234);
                    dataOutputStreamHeartbeat = new DataOutputStream(heartbeat.getOutputStream());
                    dataInputStreamHeartbeat = new DataInputStream(heartbeat.getInputStream());
                    ClientApplication.connected=true;
                }
                //Client signaling that the client is connected
                System.out.println("Conectado");
                updateConnectionStatus("Connected");
                connect(); //Establishes a connection with the server for user-related purposes
                if(!ClientApplication.hasConnected){ //Stores that the client has at least connected once
                    ClientApplication.hasConnected=true;
                } else { //Triggered only if the client has connected at least once, and this segment is to reconnect the client to the server
                    if (ClientApplication.currentScene.getSignal()>1){ //Triggers if the client is in a scene that isn't the welcome, login or sign up scene
                        //Auto-logins the user back into the app
                        ClientApplication.dataOutputStream.writeByte(0);
                        ClientApplication.dataOutputStream.flush();
                        ClientApplication.dataOutputStream.writeByte(100);
                        ClientApplication.dataOutputStream.flush();
                        ClientApplication.dataOutputStream.writeUTF(String.format("%s/%s",
                                ClientApplication.getUsername(),
                                ClientApplication.getHashedPassword())
                        );
                        ClientApplication.dataOutputStream.flush();
                        int signal = ClientApplication.dataInputStream.readByte();
                        switch (signal){ //Checks that nothing has gone wrong
                            case 0 ->{ //Something has gone wrong
                                System.out.println("Error");
                            } case 1 ->{ //Everything good for now :)
                                System.out.println("Logged in");
                                //Reloads the current scene that the user is in, opens a warning popup and closes any popup that might have been open
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        ClientApplication.loadScene(ClientApplication.currentScene);
                                        if(ClientApplication.getIsPopUpOpen()){
                                            ClientApplication.closePopup();
                                        }
                                        ClientApplication.showAlert(Alert.AlertType.INFORMATION,
                                                "Recuperación de conexión",
                                                "Has recuperado la conexión",
                                                "Ha habido una perdida de conexión temporal.\n" +
                                                        "Debido a esto, la ventana se ha recargado");
                                    }
                                });
                            }
                        }
                    } else if (ClientApplication.currentScene.getSignal() == 0 || ClientApplication.currentScene.getSignal() == 1){ //Triggers only if the user was in the login or sign up scenes
                        //Reloads the scene and warns the user
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                ClientApplication.showAlert(Alert.AlertType.INFORMATION,
                                        "Recuperación de conexión",
                                        "Has recuperado la conexión",
                                        "Ha habido una perdida de conexión temporal.\n" +
                                                "Debido a esto, la ventana se ha recargado");
                            }
                        });
                    }

                }
                while (ClientApplication.connected){ //Loops until the client disconnects, and heartbeats the server
                    dataOutputStreamHeartbeat.writeByte(-69); //Renzo specifically asked for this byte
                    dataOutputStreamHeartbeat.flush();
                    System.out.println(dataInputStreamHeartbeat.readUTF());
                    Thread.sleep(3000);
                }
            } catch (IOException e) { //Triggers when the client loses the connection with the server
                if(ClientApplication.connected){ //Triggers to signal the client connection status
                    System.err.println("Desconectado");
                    updateConnectionStatus("Not connected");
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            ClientApplication.showAlert(Alert.AlertType.INFORMATION,
                                    "Perdida de conexión",
                                    "Has perdido la conexión",
                                    "No podrás realizar operaciones hasta que se recupere.\n" +
                                            "Una vez se recupere se le avisará");
                        }
                    });
                }
                ClientApplication.connected=false;
            } catch (InterruptedException e) { //Something has just somehow interrupted the heartbeat's sleep
                System.err.println("La espera del thread de latidos se ha interrumpido de manera desconocida");
            }
        }
    }

    /**
     * Method that connects the client to the server for user-related purposes
     */
    public static void connect(){
        try {
            ClientApplication.socket = new Socket("localhost", 1234);
            ClientApplication.dataOutputStream = new DataOutputStream(ClientApplication.socket.getOutputStream());
            ClientApplication.dataInputStream = new DataInputStream(ClientApplication.socket.getInputStream());
            System.out.println("Conexión principal creada");
        } catch (IOException e) {
            System.err.println("No conectado");
        }
    }

    private void updateConnectionStatus(String status) {
        Platform.runLater(() -> statusComponent.setStatus(status.equals("Connected")));
    }

    public static void switchExit() {
        exit = true;
    }

    public void login(ActionEvent actionEvent) {
        ClientApplication.loadScene(Scenes.LOGIN_VIEW);
    }

    public void signUp(ActionEvent actionEvent) {
        ClientApplication.loadScene(Scenes.SIGNUP_VIEW);
    }
}