package dk.statsbiblioteket.pidregistration.doms;

/**
 * Exception signifying wrong credentials.
 */
public class BackendInvalidCredsException extends RuntimeException {
    public BackendInvalidCredsException(String message) {
        super(message);
    }

    public BackendInvalidCredsException(String message, Throwable cause) {
        super(message, cause);
    }
}
