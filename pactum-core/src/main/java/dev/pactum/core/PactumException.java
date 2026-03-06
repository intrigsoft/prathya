package dev.pactum.core;

/**
 * Checked exception for pactum parse and validation errors.
 */
public class PactumException extends Exception {

    public PactumException(String message) {
        super(message);
    }

    public PactumException(String message, Throwable cause) {
        super(message, cause);
    }
}
