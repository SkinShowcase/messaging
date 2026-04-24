package com.skinsshowcase.messaging.exception;

/**
 * Пользователь не найден (например при поиске чата по имени).
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
