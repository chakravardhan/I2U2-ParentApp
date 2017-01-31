package com.i2u2.parent.holder;

import com.quickblox.users.model.QBUser;

import java.util.ArrayList;
import java.util.List;

/**
 * QuickBlox team
 */
public class DataHolder {

    public static ArrayList<QBUser> usersList;
    public static final String PASSWORD = "x6Bt0VDy5";

    private static QBUser currentUser;

    public static QBUser getLoggedUser() {
        return currentUser;
    }

    public static void setLoggedUser(QBUser currentUser) {
        DataHolder.currentUser = currentUser;
    }

    public static ArrayList<QBUser> createUsersList(List<QBUser> users) {
        usersList = new ArrayList<>();

        for(QBUser user : users){
            QBUser newUser = new QBUser(user.getLogin(), user.getPassword());
            newUser.setId(user.getId());
            newUser.setFullName(user.getFullName());
            usersList.add(newUser);
        }
        return usersList;
    }

    public static void addQbUser(QBUser qbUser) {
        if (usersList == null)
            usersList = new ArrayList<>();
        if (!usersList.contains(qbUser)) {
            usersList.add(qbUser);
        }
    }

    public static String getUserNameByID(Integer callerID) {
        for (QBUser user : usersList) {
            if (user.getId().equals(callerID)) {
                return user.getFullName();
            }
        }
        return callerID.toString();
    }

    public static int getUserIndexByID(Integer callerID) {
        for (QBUser user : usersList) {
            if (user.getId().equals(callerID)) {
                return usersList.indexOf(user);
            }
        }
        return -1;
    }

    public static List<QBUser> getUsers(){
        return usersList;
    }

    public static int getUserIndexByFullName(String fullName) {
        for (QBUser user : usersList) {
            if (user.getFullName().equals(fullName)) {
                return usersList.indexOf(user);
            }
        }
        return -1;
    }

    public static ArrayList<QBUser> getUsersByIDs(Integer... ids) {
        ArrayList<QBUser> result = new ArrayList<>();
        for (Integer userId : ids) {
            for (QBUser user : usersList) {
                if (userId.equals(user.getId())){
                    result.add(user);
                }
            }
        }
        return result;
    }
}