package memoryguard.exception;

public class EncryptionServiceException extends RuntimeException {
    public EncryptionServiceException(String message) {
        super(message);
    }

    public EncryptionServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

