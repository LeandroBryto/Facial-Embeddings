package memoryguard.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    private final String error;

    public ResourceNotFoundException(String message) {
        super(message);
        this.error = "NOT_FOUND";
    }

    public ResourceNotFoundException(String error, String message) {
        super(message);
        this.error = (error == null || error.isBlank()) ? "NOT_FOUND" : error;
    }

    public String getError() {
        return error;
    }
}
