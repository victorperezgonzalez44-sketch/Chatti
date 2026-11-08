package ies.dam.servidor.models;

import java.io.Serializable;

public class Test implements Serializable {
    private String id;

    public Test(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
