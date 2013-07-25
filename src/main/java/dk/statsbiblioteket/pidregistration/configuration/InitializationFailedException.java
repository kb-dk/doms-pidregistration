package dk.statsbiblioteket.pidregistration.configuration;

/**
 * Exception signifying initialization failure.
 */
public class InitializationFailedException extends RuntimeException {
    public InitializationFailedException(String message) {
        super(message);
    }

    public InitializationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
