package ies.dam.client.chatticlient.models.controllers;

public enum Visibility {
    VISIBLE("Visible"),
    INVISIBLE("Invisible");
    private final String lowerCaseName;

    Visibility(String lowerCaseName){
        this.lowerCaseName = lowerCaseName;
    }

    public String getLowerCaseName() {
        return lowerCaseName;
    }
}
