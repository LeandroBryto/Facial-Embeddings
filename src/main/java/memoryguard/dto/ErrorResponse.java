package memoryguard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String error;
    private String message;
    private Double confidence;

    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, null);
    }

    public static ErrorResponse of(String error, String message, Double confidence) {
        return new ErrorResponse(error, message, confidence);
    }
}

