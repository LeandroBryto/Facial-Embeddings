package memoryguard.service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import memoryguard.exception.ResourceNotFoundException;
import memoryguard.model.CredencialSessao;
import memoryguard.model.PerfilUsuario;
import memoryguard.repository.CredencialSessaoRepository;
import memoryguard.repository.PerfilUsuarioRepository;

@Service
public class AuthSessionService {

    public static final String ANY_SYSTEM = "*";

    private final CredencialSessaoRepository credencialSessaoRepository;
    private final PerfilUsuarioRepository perfilUsuarioRepository;
    private final SecureRandom secureRandom;
    private final int tokenMinutes;

    public AuthSessionService(
            CredencialSessaoRepository credencialSessaoRepository,
            PerfilUsuarioRepository perfilUsuarioRepository,
            @Value("${memoryguard.session.token-minutes:5}") int tokenMinutes
    ) {
        this.credencialSessaoRepository = credencialSessaoRepository;
        this.perfilUsuarioRepository = perfilUsuarioRepository;
        this.secureRandom = new SecureRandom();
        this.tokenMinutes = tokenMinutes <= 0 ? 5 : tokenMinutes;
    }

    public IssuedToken issue(Long userId, String system) {
        if (userId == null) {
            throw new IllegalArgumentException("userId é obrigatório");
        }
        PerfilUsuario perfil = perfilUsuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuário não encontrado: id=" + userId));

        String sys = (system == null || system.isBlank()) ? ANY_SYSTEM : system;
        String rawToken = generateToken();
        String tokenHash = sha256Hex(rawToken);

        LocalDateTime now = LocalDateTime.now();
        CredencialSessao entity = CredencialSessao.builder()
                .perfilUsuario(perfil)
                .nomeSistema(sys)
                .senhaTemporaria(tokenHash)
                .dataCriacao(now)
                .dataExpiracao(now.plusMinutes(tokenMinutes))
                .build();
        credencialSessaoRepository.save(entity);
        return new IssuedToken(rawToken, entity.getDataExpiracao(), sys);
    }

    public boolean validate(Long userId, String system, String rawToken) {
        if (userId == null || rawToken == null || rawToken.isBlank()) {
            return false;
        }
        String sys = (system == null || system.isBlank()) ? ANY_SYSTEM : system;
        LocalDateTime now = LocalDateTime.now();
        credencialSessaoRepository.deleteByDataExpiracaoBefore(now);

        String tokenHash = sha256Hex(rawToken);
        CredencialSessao match = credencialSessaoRepository
                .findTopByPerfilUsuario_IdUsuarioAndNomeSistemaAndDataExpiracaoAfterOrderByDataCriacaoDesc(userId, sys, now)
                .orElseGet(() -> credencialSessaoRepository
                        .findTopByPerfilUsuario_IdUsuarioAndNomeSistemaAndDataExpiracaoAfterOrderByDataCriacaoDesc(userId, ANY_SYSTEM, now)
                        .orElse(null)
                );
        return match != null && tokenHash.equals(match.getSenhaTemporaria());
    }

    private String generateToken() {
        byte[] rnd = new byte[16];
        secureRandom.nextBytes(rnd);
        String seed = UUID.randomUUID() + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar hash do token", e);
        }
    }

    public static final class IssuedToken {
        private final String token;
        private final LocalDateTime expiresAt;
        private final String system;

        public IssuedToken(String token, LocalDateTime expiresAt, String system) {
            this.token = token;
            this.expiresAt = expiresAt;
            this.system = system;
        }

        public String getToken() {
            return token;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public String getSystem() {
            return system;
        }
    }
}
