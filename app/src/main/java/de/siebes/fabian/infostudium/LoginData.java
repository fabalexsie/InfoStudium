package de.siebes.fabian.infostudium;

import androidx.annotation.NonNull;

public class LoginData {
    private long id;
    private String name;
    private String benutzer;
    private String passwort;

    LoginData() {
        this(-1, "", "", "");
    }

    LoginData(int id, String name, String strBenutzer, String strPasswort) {
        this.id = id;
        this.name = name;
        benutzer = strBenutzer;
        passwort = strPasswort;
    }

    public boolean isEmpty() {
        return getBenutzer().isEmpty() || getPasswort().isEmpty();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBenutzer() {
        return benutzer;
    }

    public void setBenutzer(String benutzer) {
        this.benutzer = benutzer;
    }

    public String getPasswort() {
        return passwort;
    }

    public void setPasswort(String passwort) {
        this.passwort = passwort;
    }

    @NonNull
    @Override
    public String toString() {
        return getName() + " (" + getBenutzer() + ")";
    }
}
