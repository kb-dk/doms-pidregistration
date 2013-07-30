package dk.statsbiblioteket.pidregistration;

public class UnknownCollectionException extends RuntimeException {
    public UnknownCollectionException() {
        super();
    }

    public UnknownCollectionException(String message) {
        super(message);
    }

    public UnknownCollectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownCollectionException(Throwable cause) {
        super(cause);
    }
}
