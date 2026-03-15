package memoryguard.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import memoryguard.dto.ErrorResponse;
import memoryguard.dto.FaceAuthResponse;
import memoryguard.dto.FaceEnrollRequest;
import memoryguard.dto.FaceImageAuthRequest;
import memoryguard.dto.FaceImageResponse;
import memoryguard.dto.LoginAutomationRequest;
import memoryguard.exception.FaceAuthorizationException;
import memoryguard.model.SessaoBiometrica;
import memoryguard.repository.PerfilUsuarioRepository;
import memoryguard.repository.SessaoBiometricaRepository;
import memoryguard.service.AuditService;
import memoryguard.service.AuthSessionService;
import memoryguard.service.BiometricSessionService;
import memoryguard.service.FaceRecognitionService;
import memoryguard.service.RobotService;
import memoryguard.service.VaultService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final FaceRecognitionService faceRecognitionService;
    private final SessaoBiometricaRepository sessaoBiometricaRepository;
    private final VaultService vaultService;
    private final RobotService robotService;
    private final PerfilUsuarioRepository perfilUsuarioRepository;
    private final AuditService auditService;
    private final AuthSessionService authSessionService;
    private final BiometricSessionService biometricSessionService;

    public AuthController(
            FaceRecognitionService faceRecognitionService,
            SessaoBiometricaRepository sessaoBiometricaRepository,
            VaultService vaultService,
            RobotService robotService,
            PerfilUsuarioRepository perfilUsuarioRepository,
            AuditService auditService,
            AuthSessionService authSessionService,
            BiometricSessionService biometricSessionService
    ) {
        this.faceRecognitionService = faceRecognitionService;
        this.sessaoBiometricaRepository = sessaoBiometricaRepository;
        this.vaultService = vaultService;
        this.robotService = robotService;
        this.perfilUsuarioRepository = perfilUsuarioRepository;
        this.auditService = auditService;
        this.authSessionService = authSessionService;
        this.biometricSessionService = biometricSessionService;
    }

    @PostMapping("/face")
    public ResponseEntity<FaceAuthResponse> authenticateFace(HttpServletRequest httpRequest) {
        FaceRecognitionService.AuthenticationResult result = faceRecognitionService.authenticateFromWebcam();
        if (!result.authorized()) {
            throw new FaceAuthorizationException("Rosto não autorizado", result.confidence());
        }

        biometricSessionService.endActiveSessions(result.user().getIdUsuario());
        SessaoBiometrica sessao = SessaoBiometrica.builder()
                .perfilUsuario(result.user())
                .inicioSessao(LocalDateTime.now())
                .ultimaFaceDetectada(LocalDateTime.now())
                .statusSessao("ACTIVE")
                .pontuacaoConfianca(result.confidence())
                .build();
        sessaoBiometricaRepository.save(sessao);

        auditService.success(
                "FACE_AUTH_WEBCAM userId=" + result.user().getIdUsuario() + " confidence=" + result.confidence(),
                httpRequest
        );
        return ResponseEntity.ok(new FaceAuthResponse("AUTHORIZED", result.user().getIdUsuario(), result.user().getNome(), result.confidence()));
    }

    @PostMapping("/face/enroll")
    public ResponseEntity<FaceAuthResponse> enrollFace(@RequestBody FaceEnrollRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getUserId() == null || request.getImageBase64() == null) {
            return ResponseEntity.badRequest().build();
        }
        var perfil = faceRecognitionService.enrollFromImageBase64(request.getUserId(), request.getImageBase64());
        auditService.success("FACE_ENROLL userId=" + perfil.getIdUsuario(), httpRequest);
        return ResponseEntity.ok(new FaceAuthResponse("ENROLLED", perfil.getIdUsuario(), perfil.getNome(), 1.0));
    }

    @PostMapping("/face/image")
    public ResponseEntity<FaceAuthResponse> authenticateFaceFromImage(@RequestBody FaceImageAuthRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getImageBase64() == null) {
            return ResponseEntity.badRequest().build();
        }
        FaceRecognitionService.AuthenticationResult result = faceRecognitionService.authenticateFromImageBase64(request.getImageBase64());
        if (!result.authorized()) {
            throw new FaceAuthorizationException("Rosto não autorizado", result.confidence());
        }
        auditService.success(
                "FACE_AUTH_IMAGE userId=" + result.user().getIdUsuario() + " confidence=" + result.confidence(),
                httpRequest
        );
        return ResponseEntity.ok(new FaceAuthResponse("AUTHORIZED", result.user().getIdUsuario(), result.user().getNome(), result.confidence()));
    }

    @PostMapping("/face/verify/{userId}")
    public ResponseEntity<FaceAuthResponse> verifyFaceForUser(
            @PathVariable("userId") Long userId,
            @RequestBody FaceImageAuthRequest request,
            @RequestParam(value = "system", required = false) String system,
            HttpServletRequest httpRequest
    ) {
        if (request == null || request.getImageBase64() == null) {
            return ResponseEntity.badRequest().build();
        }
        FaceRecognitionService.AuthenticationResult result = faceRecognitionService.authenticateUserFromImageBase64(userId, request.getImageBase64());
        if (!result.authorized()) {
            throw new FaceAuthorizationException("Rosto não autorizado", result.confidence());
        }
        biometricSessionService.endActiveSessions(result.user().getIdUsuario());
        SessaoBiometrica sessao = SessaoBiometrica.builder()
                .perfilUsuario(result.user())
                .inicioSessao(LocalDateTime.now())
                .ultimaFaceDetectada(LocalDateTime.now())
                .statusSessao("ACTIVE")
                .pontuacaoConfianca(result.confidence())
                .build();
        sessaoBiometricaRepository.save(sessao);
        AuthSessionService.IssuedToken token = authSessionService.issue(userId, system);
        auditService.success(
                "FACE_VERIFY userId=" + result.user().getIdUsuario() + " confidence=" + result.confidence() + " system=" + token.getSystem(),
                httpRequest
        );
        return ResponseEntity.ok()
                .header("X-Auth-Token", token.getToken())
                .header("X-Auth-Expires-At", String.valueOf(token.getExpiresAt()))
                .body(new FaceAuthResponse("AUTHORIZED", result.user().getIdUsuario(), result.user().getNome(), result.confidence()));
    }

    @PostMapping("/login/{system}")
    public ResponseEntity<?> loginAutomacao(
            @PathVariable("system") String system,
            @RequestBody LoginAutomationRequest request,
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            HttpServletRequest httpRequest
    ) {
        if (request == null || request.getUserId() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!authSessionService.validate(request.getUserId(), system, token)) {
            auditService.fail("AUTO_LOGIN_DENIED userId=" + request.getUserId() + " system=" + system, httpRequest);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of("TOKEN_INVALID", "Token inválido ou expirado"));
        }

        VaultService.DecryptedCredential credencial = vaultService.loadCredential(request.getUserId(), system);
        robotService.openBrowser(credencial.getUrl());
        robotService.performLogin(credencial.getLogin(), credencial.getPassword());

        auditService.success("AUTO_LOGIN userId=" + request.getUserId() + " system=" + system, httpRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessions/{userId}")
    public ResponseEntity<?> listSessions(@PathVariable("userId") Long userId) {
        List<SessaoBiometrica> sessions = biometricSessionService.listRecent(userId);
        List<Map<String, Object>> out = sessions.stream()
                .map(s -> Map.<String, Object>ofEntries(
                        Map.entry("idSessao", (Object) s.getIdSessao()),
                        Map.entry("userId", (Object) (s.getPerfilUsuario() == null ? null : s.getPerfilUsuario().getIdUsuario())),
                        Map.entry("inicioSessao", (Object) s.getInicioSessao()),
                        Map.entry("ultimaFaceDetectada", (Object) s.getUltimaFaceDetectada()),
                        Map.entry("statusSessao", (Object) s.getStatusSessao()),
                        Map.entry("pontuacaoConfianca", (Object) s.getPontuacaoConfianca())
                ))
                .toList();
        return ResponseEntity.ok(out);
    }

    @PostMapping("/sessions/{userId}/end")
    public ResponseEntity<?> endSessions(@PathVariable("userId") Long userId, HttpServletRequest httpRequest) {
        biometricSessionService.endActiveSessions(userId);
        auditService.success("SESSION_END userId=" + userId, httpRequest);
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

