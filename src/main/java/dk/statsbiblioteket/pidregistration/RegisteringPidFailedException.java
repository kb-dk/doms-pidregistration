package dk.statsbiblioteket.pidregistration;

/**
 * In case trying to resolve a handle failed abnormally
 */
public class RegisteringPidFailedException extends RuntimeException {
    public RegisteringPidFailedException(String message) {
        super(message);
    }

    public RegisteringPidFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
