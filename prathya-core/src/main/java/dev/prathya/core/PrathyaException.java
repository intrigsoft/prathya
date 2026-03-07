package dev.prathya.core;

/**
 * Checked exception for prathya parse and validation errors.
 */
public class PrathyaException extends Exception {

    public PrathyaException(String message) {
        super(message);
    }

    public PrathyaException(String message, Throwable cause) {
        super(message, cause);
    }
}
