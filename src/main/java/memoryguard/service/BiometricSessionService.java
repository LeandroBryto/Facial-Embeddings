package memoryguard.service;

import memoryguard.model.ConfiguracaoSistema;
import memoryguard.model.SessaoBiometrica;
import memoryguard.repository.ConfiguracaoSistemaRepository;
import memoryguard.repository.SessaoBiometricaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BiometricSessionService {

    private final SessaoBiometricaRepository sessaoBiometricaRepository;
    private final ConfiguracaoSistemaRepository configuracaoSistemaRepository;
    private final int defaultTimeoutMinutes;

    public BiometricSessionService(
            SessaoBiometricaRepository sessaoBiometricaRepository,
            ConfiguracaoSistemaRepository configuracaoSistemaRepository,
            @Value("${memoryguard.session.timeout-minutes:5}") int defaultTimeoutMinutes
    ) {
        this.sessaoBiometricaRepository = sessaoBiometricaRepository;
        this.configuracaoSistemaRepository = configuracaoSistemaRepository;
        this.defaultTimeoutMinutes = defaultTimeoutMinutes <= 0 ? 5 : defaultTimeoutMinutes;
    }

    public void endActiveSessions(Long userId) {
        if (userId == null) {
            return;
        }
        List<SessaoBiometrica> active = sessaoBiometricaRepository.findByPerfilUsuario_IdUsuarioAndStatusSessao(userId, "ACTIVE");
        if (active.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (SessaoBiometrica s : active) {
            s.setStatusSessao("ENDED");
            s.setUltimaFaceDetectada(now);
        }
        sessaoBiometricaRepository.saveAll(active);
    }

    public List<SessaoBiometrica> listRecent(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return sessaoBiometricaRepository.findTop20ByPerfilUsuario_IdUsuarioOrderByInicioSessaoDesc(userId);
    }

    @Scheduled(fixedDelayString = "${memoryguard.session.expire-delay-ms:60000}")
    public void expireInactiveSessions() {
        int timeout = resolveTimeoutMinutes();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeout);
        List<SessaoBiometrica> expired = sessaoBiometricaRepository.findByStatusSessaoAndUltimaFaceDetectadaBefore("ACTIVE", cutoff);
        if (expired.isEmpty()) {
            return;
        }
        for (SessaoBiometrica s : expired) {
            s.setStatusSessao("EXPIRED");
        }
        sessaoBiometricaRepository.saveAll(expired);
    }

    private int resolveTimeoutMinutes() {
        ConfiguracaoSistema cfg = configuracaoSistemaRepository.findByChave("session.timeout.minutes").orElse(null);
        if (cfg == null || cfg.getValor() == null || cfg.getValor().isBlank()) {
            return defaultTimeoutMinutes;
        }
        try {
            int v = Integer.parseInt(cfg.getValor().trim());
            return v <= 0 ? defaultTimeoutMinutes : v;
        } catch (Exception e) {
            return defaultTimeoutMinutes;
        }
    }
}
