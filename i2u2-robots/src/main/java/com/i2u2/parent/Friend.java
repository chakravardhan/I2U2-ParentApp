package com.i2u2.parent;

/**
 * Created by kailash on 8/6/16.
 */
public class Friend {
    private String email;
    private String botName;

    public Friend() {
    }

    public Friend(String email, String botName) {
        this.email = email;
        this.botName = botName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }
}
