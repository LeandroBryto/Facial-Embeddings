package memoryguard.controller;

import memoryguard.dto.VaultCredentialRequest;
import memoryguard.service.VaultService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vault")
public class VaultController {

    private final VaultService vaultService;

    public VaultController(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @PostMapping("/credential")
    public ResponseEntity<?> storeCredential(@RequestBody VaultCredentialRequest request) {
        if (request == null
                || request.getUserId() == null
                || request.getSystem() == null
                || request.getLogin() == null
                || request.getPassword() == null) {
            return ResponseEntity.badRequest().build();
        }

        var saved = vaultService.storeCredential(
                request.getUserId(),
                request.getSystem(),
                request.getLogin(),
                request.getPassword(),
                request.getUrl()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of(
                "status", "SAVED",
                "credentialId", saved.getIdCofre(),
                "userId", request.getUserId(),
                "system", request.getSystem()
        ));
    }
}

