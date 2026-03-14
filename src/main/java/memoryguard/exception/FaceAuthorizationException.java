package memoryguard.exception;

public class FaceAuthorizationException extends RuntimeException {

    private final double confidence;

    public FaceAuthorizationException(String message, double confidence) {
        super(message);
        this.confidence = confidence;
    }

    public double getConfidence() {
        return confidence;
    }
}

