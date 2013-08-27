package dk.statsbiblioteket.pidregistration.doms;

public class BackendInvalidStateException extends RuntimeException {
    public BackendInvalidStateException() {
    }

    public BackendInvalidStateException(String message) {
        super(message);
    }

    public BackendInvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public BackendInvalidStateException(Throwable cause) {
        super(cause);
    }
}
