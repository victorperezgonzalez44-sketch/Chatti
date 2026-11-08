package ies.dam.client.chatticlient;

public enum Scenes {
    WELCOME_VIEW("Chatti - Bienvenido", "startup/welcome-view.fxml", 630, 442),
    LOGIN_VIEW("Chatti - Iniciar Sesión", "startup/login-view.fxml", 220, 310, 0),
    SIGNUP_VIEW("Chatti - Registrarse", "startup/signup-view.fxml", 630, 442, 1),
    CHAT_VIEW("Chatti - Chat", "chat/chat-view.fxml", 630, 412, 2),
    GROUP_CHAT_VIEW("Chatti - Grupos", "chat/groupchat-view.fxml", 630, 412, 3),
    RANDOM_CHAT("Chatti - Chat Aleatorio", "chat/random-view.fxml", 630, 412, 4),
    DELETE_CONTACT("Chatti - Borrar Contacto", "chat/popup/deletecontact-view.fxml", 190, 90), //Deleted signal from here to keep server consistent
    LEAVE_GROUP("Chatti - Salir del grupo", "chat/popup/leavegroup-view.fxml", 200, 90, 5),
    FILE_SELECT("Chatti - Seleccionar Archivo", "chat/popup/fileselect-view.fxml", 430, 200, 6),
    FILE_SENT("Chatti - Archivo Enviado", "chat/popup/filesent-view.fxml", 160, 90),
    ADD_TO_GROUP("Chatti - Añadir miembros al grupo", "chat/popup/addtogroup-view.fxml", 500, 412, 7),
    ADD_CONTACT("Chatti - Solicitudes de contacto", "chat/new_chat/addcontact-view.fxml", 630, 412, 8),
    CREATE_GROUP("Chatti - Crear un grupo nuevo", "chat/new_chat/creategroup-view.fxml", 630, 412, 9),
    MAIL_DIALOGUE("Chatti - Restaurar contraseña", "startup/popup/email-input.fxml", 322, 189, 10),
    MAIL_CONFIRMATION("Chatti - Confirmar código", "startup/popup/email-auth.fxml", 322, 189),
    PASSWORD_CHANGE("Chatti - Cambiar contraseña", "startup/popup/password-change.fxml", 251, 250);
    private final String name;
    private final String fxmlName;
    private final int width;
    private final int height;
    private final int signal;

    Scenes(String name, String fxmlName, int width, int height, int signal) {
        this.name = name;
        this.fxmlName = fxmlName;
        this.width = width;
        this.height = height;
        this.signal = signal;
    }

    Scenes(String name, String fxmlName, int width, int height) {
        this.name = name;
        this.fxmlName = fxmlName;
        this.width = width;
        this.height = height;
        this.signal = -100; //This could be left as null
    }

    public String getName() {
        return name;
    }

    public String getFxmlName() {
        return fxmlName;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getSignal() {
        return signal;
    }
}
