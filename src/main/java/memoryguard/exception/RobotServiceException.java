package memoryguard.exception;

public class RobotServiceException extends RuntimeException {
    public RobotServiceException(String message) {
        super(message);
    }

    public RobotServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

