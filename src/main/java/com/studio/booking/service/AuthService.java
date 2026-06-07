package com.studio.booking.service;

import com.studio.booking.dao.AdminDao;
import com.studio.booking.dao.impl.AdminDaoImpl;

/**
 * Сервис аутентификации
 */
public class AuthService {

    private final AdminDao adminDao = new AdminDaoImpl();

    /**
     * Проверка пароля администратора.
     *
     * @return true, если пароль верен.
     */
    public boolean login(String password) {
        if (password == null || password.isBlank()) {
            return false;
        }
        return adminDao.checkPassword(password);
    }
}
