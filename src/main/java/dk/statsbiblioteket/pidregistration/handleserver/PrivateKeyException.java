package dk.statsbiblioteket.pidregistration.handleserver;

/**
 * Exception signifying something went wrong loading the handle private key.
 */
public class PrivateKeyException extends RuntimeException {
    public PrivateKeyException(String message) {
        super(message);
    }

    public PrivateKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
