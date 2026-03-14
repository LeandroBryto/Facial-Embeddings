package memoryguard.controller;

import memoryguard.dto.*;
import memoryguard.exception.FaceAuthorizationException;
import memoryguard.model.SessaoBiometrica;
import memoryguard.repository.PerfilUsuarioRepository;
import memoryguard.repository.SessaoBiometricaRepository;
import memoryguard.service.FaceRecognitionService;
import memoryguard.service.RobotService;
import memoryguard.service.VaultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final FaceRecognitionService faceRecognitionService;
    private final SessaoBiometricaRepository sessaoBiometricaRepository;
    private final VaultService vaultService;
    private final RobotService robotService;
    private final PerfilUsuarioRepository perfilUsuarioRepository;

    public AuthController(
            FaceRecognitionService faceRecognitionService,
            SessaoBiometricaRepository sessaoBiometricaRepository,
            VaultService vaultService,
            RobotService robotService,
            PerfilUsuarioRepository perfilUsuarioRepository
    ) {
        this.faceRecognitionService = faceRecognitionService;
        this.sessaoBiometricaRepository = sessaoBiometricaRepository;
        this.vaultService = vaultService;
        this.robotService = robotService;
        this.perfilUsuarioRepository = perfilUsuarioRepository;
    }

    @PostMapping("/face")
    public ResponseEntity<FaceAuthResponse> authenticateFace() {
        FaceRecognitionService.AuthenticationResult result = faceRecognitionService.authenticateFromWebcam();
        if (!result.authorized()) {
            throw new FaceAuthorizationException("Rosto não autorizado", result.confidence());
        }

        SessaoBiometrica sessao = SessaoBiometrica.builder()
                .perfilUsuario(result.user())
                .inicioSessao(LocalDateTime.now())
                .ultimaFaceDetectada(LocalDateTime.now())
                .statusSessao("ACTIVE")
                .pontuacaoConfianca(result.confidence())
                .build();
        sessaoBiometricaRepository.save(sessao);

        return ResponseEntity.ok(new FaceAuthResponse("AUTHORIZED", result.user().getIdUsuario(), result.user().getNome(), result.confidence()));
    }

    @PostMapping("/face/enroll")
    public ResponseEntity<FaceAuthResponse> enrollFace(@RequestBody FaceEnrollRequest request) {
        if (request == null || request.getUserId() == null || request.getImageBase64() == null) {
            return ResponseEntity.badRequest().build();
        }
        var perfil = faceRecognitionService.enrollFromImageBase64(request.getUserId(), request.getImageBase64());
        return ResponseEntity.ok(new FaceAuthResponse("ENROLLED", perfil.getIdUsuario(), perfil.getNome(), 1.0));
    }

    @PostMapping("/face/image")
    public ResponseEntity<FaceAuthResponse> authenticateFaceFromImage(@RequestBody FaceImageAuthRequest request) {
        if (request == null || request.getImageBase64() == null) {
            return ResponseEntity.badRequest().build();
        }
        FaceRecognitionService.AuthenticationResult result = faceRecognitionService.authenticateFromImageBase64(request.getImageBase64());
        if (!result.authorized()) {
            throw new FaceAuthorizationException("Rosto não autorizado", result.confidence());
        }
        return ResponseEntity.ok(new FaceAuthResponse("AUTHORIZED", result.user().getIdUsuario(), result.user().getNome(), result.confidence()));
    }

    @PostMapping("/face/verify/{userId}")
    public ResponseEntity<FaceAuthResponse> verifyFaceForUser(
            @PathVariable("userId") Long userId,
            @RequestBody FaceImageAuthRequest request
    ) {
        if (request == null || request.getImageBase64() == null) {
            return ResponseEntity.badRequest().build();
        }
        FaceRecognitionService.AuthenticationResult result = faceRecognitionService.authenticateUserFromImageBase64(userId, request.getImageBase64());
        if (!result.authorized()) {
            throw new FaceAuthorizationException("Rosto não autorizado", result.confidence());
        }
        SessaoBiometrica sessao = SessaoBiometrica.builder()
                .perfilUsuario(result.user())
                .inicioSessao(LocalDateTime.now())
                .ultimaFaceDetectada(LocalDateTime.now())
                .statusSessao("ACTIVE")
                .pontuacaoConfianca(result.confidence())
                .build();
        sessaoBiometricaRepository.save(sessao);
        return ResponseEntity.ok(new FaceAuthResponse("AUTHORIZED", result.user().getIdUsuario(), result.user().getNome(), result.confidence()));
    }

    @PostMapping("/login/{system}")
    public ResponseEntity<?> loginAutomacao(
            @PathVariable("system") String system,
            @RequestBody LoginAutomationRequest request
    ) {
        if (request == null || request.getUserId() == null) {
            return ResponseEntity.badRequest().build();
        }

        VaultService.DecryptedCredential credencial = vaultService.loadCredential(request.getUserId(), system);
        robotService.openBrowser(credencial.getUrl());
        robotService.performLogin(credencial.getLogin(), credencial.getPassword());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/face/enroll/{userId}/image")
    public ResponseEntity<FaceImageResponse> getEnrolledFaceImage(@PathVariable("userId") Long userId) {
        var perfil = perfilUsuarioRepository.findById(userId).orElse(null);
        if (perfil == null || perfil.getFaceImage() == null || perfil.getFaceImage().length == 0) {
            return ResponseEntity.notFound().build();
        }
        String base64 = java.util.Base64.getEncoder().encodeToString(perfil.getFaceImage());
        return ResponseEntity.ok(new FaceImageResponse(userId, perfil.getFaceImage().length, base64));
    }
}

