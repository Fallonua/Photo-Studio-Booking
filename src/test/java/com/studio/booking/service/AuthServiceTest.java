package com.studio.booking.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AuthServiceTest {

    // Сервис входа администратора — тестируемый объект
    private final AuthService authService = new AuthService();

    @Test
    void login_returnsFalse_whenPasswordIsNull() {
        // Пароль не передан — вход должен быть отклонён
        assertFalse(authService.login(null));
    }

    @Test
    void login_returnsFalse_whenPasswordIsBlank() {
        // Пароль из одних пробелов — вход должен быть отклонён
        assertFalse(authService.login("   "));
    }
}
