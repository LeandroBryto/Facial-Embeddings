package memoryguard.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import memoryguard.dto.ErrorResponse;
import memoryguard.service.AuditService;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final AuditService auditService;

    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        auditService.fail(buildErrorEvent("NOT_FOUND", ex, request), request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getError(), ex.getMessage()));
    }

    @ExceptionHandler(FaceAuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleFaceUnauthorized(FaceAuthorizationException ex, HttpServletRequest request) {
        auditService.fail(buildErrorEvent("FACE_UNAUTHORIZED", ex, request) + " confidence=" + ex.getConfidence(), request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("FACE_UNAUTHORIZED", ex.getMessage(), ex.getConfidence()));
    }

    @ExceptionHandler({VisionException.class, RobotServiceException.class, EncryptionServiceException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        auditService.fail(buildErrorEvent("BAD_REQUEST", ex, request), request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("REQUEST_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        auditService.fail(buildErrorEvent("INTERNAL_ERROR", ex, request), request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Erro inesperado"));
    }

    private static String buildErrorEvent(String category, Exception ex, HttpServletRequest request) {
        String method = request == null ? "?" : request.getMethod();
        String uri = request == null ? "?" : request.getRequestURI();
        String exName = ex == null ? "Exception" : ex.getClass().getSimpleName();
        String tag = category == null || category.isBlank() ? "ERROR" : category;
        return tag + " " + method + " " + uri + " ex=" + exName;
    }
}
