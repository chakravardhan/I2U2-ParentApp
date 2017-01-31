package com.i2u2.parent;

import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * Created by kailash on 8/6/16.
 */
public class User {
    private String name;
    private String uid;
    private String profileURL;
    private HashMap<String, String> friends;
    private HashMap<String, String> quickblox;
    private String type_of_user;

    public String getType_of_user() {
        return type_of_user;
    }

    public void setType_of_user(String type_of_user) {
        this.type_of_user = type_of_user;
    }

    public User() {
    }

    public User(String name, String uid, String profileURL, HashMap<String, String> friends, HashMap<String, String> quickblox, String type_of_user) {
        this.name = name;
        this.uid = uid;
        this.profileURL = profileURL;
        this.friends = friends;
        this.quickblox = quickblox;
        this.type_of_user = type_of_user;
    }

    public HashMap<String, String> getFriends() {
        return friends;
    }

    public void setFriends(HashMap<String, String> friends) {
        this.friends = friends;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getProfileURL() {
        return profileURL;
    }

    public void setProfileURL(String profileURL) {
        this.profileURL = profileURL;
    }

    public HashMap<String, String> getQuickblox() {
        return quickblox;
    }

    public void setQuickblox(HashMap<String, String> quickblox) {
        this.quickblox = quickblox;
    }
}
