package com.example.cycleborrowingsystem;

import com.example.cycleborrowingsystem.models.User;

/**
 * Lightweight session holder used by controllers to access the current authenticated user.
 * Minimal implementation to satisfy references across the app.
 */
public class Session {
    private static User currentUser;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }
}
