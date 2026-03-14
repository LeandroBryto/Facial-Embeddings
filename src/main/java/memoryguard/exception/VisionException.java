package memoryguard.exception;

public class VisionException extends RuntimeException {
    public VisionException(String message) {
        super(message);
    }

    public VisionException(String message, Throwable cause) {
        super(message, cause);
    }
}

