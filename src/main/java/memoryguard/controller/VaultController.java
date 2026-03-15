package memoryguard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import memoryguard.dto.ErrorResponse;
import memoryguard.dto.VaultCredentialRequest;
import memoryguard.service.AuditService;
import memoryguard.service.AuthSessionService;
import memoryguard.service.VaultService;

@RestController
@RequestMapping("/vault")
public class VaultController {

    private final VaultService vaultService;
    private final AuditService auditService;
    private final AuthSessionService authSessionService;

    public VaultController(VaultService vaultService, AuditService auditService, AuthSessionService authSessionService) {
        this.vaultService = vaultService;
        this.auditService = auditService;
        this.authSessionService = authSessionService;
    }

    @PostMapping("/credential")
    public ResponseEntity<?> storeCredential(
            @RequestBody VaultCredentialRequest request,
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            HttpServletRequest httpRequest
    ) {
        if (request == null
                || request.getUserId() == null
                || request.getSystem() == null
                || request.getLogin() == null
                || request.getPassword() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!authSessionService.validate(request.getUserId(), request.getSystem(), token)) {
            auditService.fail("VAULT_SAVE_DENIED userId=" + request.getUserId() + " system=" + request.getSystem(), httpRequest);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of("TOKEN_INVALID", "Token inválido ou expirado"));
        }

        var saved = vaultService.storeCredential(
                request.getUserId(),
                request.getSystem(),
                request.getLogin(),
                request.getPassword(),
                request.getUrl()
        );

        auditService.success("VAULT_SAVE userId=" + request.getUserId() + " system=" + request.getSystem(), httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of(
                "status", "SAVED",
                "credentialId", saved.getIdCofre(),
                "userId", request.getUserId(),
                "system", request.getSystem()
        ));
    }
}

