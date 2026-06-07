package com.studio.booking.service;

/**
 * Хранит состояние сессии администратора.
 */
public final class Session {

    private static boolean loggedIn;
    private Session() {
    }

    public static boolean isLoggedIn() {
        return loggedIn;
    }

    public static void setLoggedIn(boolean value) {
        loggedIn = value;
    }

    public static void clear() {
        loggedIn = false;
    }
}
