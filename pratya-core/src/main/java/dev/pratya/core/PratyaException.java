package dev.pratya.core;

/**
 * Checked exception for pratya parse and validation errors.
 */
public class PratyaException extends Exception {

    public PratyaException(String message) {
        super(message);
    }

    public PratyaException(String message, Throwable cause) {
        super(message, cause);
    }
}
