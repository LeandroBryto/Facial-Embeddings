package memoryguard.controller;

import jakarta.servlet.http.HttpServletRequest;
import memoryguard.model.LogAtividade;
import memoryguard.repository.LogAtividadeRepository;
import memoryguard.repository.SessaoBiometricaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private final LogAtividadeRepository logAtividadeRepository;
    private final SessaoBiometricaRepository sessaoBiometricaRepository;
    private final String adminKey;

    public AdminDashboardController(
            LogAtividadeRepository logAtividadeRepository,
            SessaoBiometricaRepository sessaoBiometricaRepository,
            @Value("${memoryguard.admin.key:}") String adminKey
    ) {
        this.logAtividadeRepository = logAtividadeRepository;
        this.sessaoBiometricaRepository = sessaoBiometricaRepository;
        this.adminKey = adminKey == null ? "" : adminKey;
    }

    @GetMapping
    public ResponseEntity<?> summary(HttpServletRequest request) {
        ResponseEntity<?> guard = guard(request);
        if (guard != null) {
            return guard;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24 = now.minusHours(24);

        long logsTotal = logAtividadeRepository.count();
        long logsLast24 = logAtividadeRepository.countByDataEventoAfter(last24);
        long logsSuccessLast24 = logAtividadeRepository.countByStatusAndDataEventoAfter("SUCCESS", last24);
        long logsFailLast24 = logAtividadeRepository.countByStatusAndDataEventoAfter("FAIL", last24);

        long activeSessions = sessaoBiometricaRepository.countByStatusSessao("ACTIVE");
        long sessionsLast24 = sessaoBiometricaRepository.countByInicioSessaoAfter(last24);

        List<Map<String, Object>> topUsers = sessaoBiometricaRepository.countSessionsByUser(PageRequest.of(0, 5)).stream()
                .map(row -> Map.of("userId", row[0], "sessions", row[1]))
                .toList();

        List<Map<String, Object>> recentLogs = logAtividadeRepository.findTop50ByOrderByDataEventoDesc().stream()
                .map(this::toLogMap)
                .toList();

        return ResponseEntity.ok(Map.of(
                "now", now,
                "logsTotal", logsTotal,
                "logsLast24h", logsLast24,
                "logsSuccessLast24h", logsSuccessLast24,
                "logsFailLast24h", logsFailLast24,
                "activeSessions", activeSessions,
                "sessionsLast24h", sessionsLast24,
                "topUsers", topUsers,
                "recentLogs", recentLogs
        ));
    }

    private Map<String, Object> toLogMap(LogAtividade l) {
        return Map.of(
                "idLog", l.getIdLog(),
                "dataEvento", l.getDataEvento(),
                "status", l.getStatus(),
                "descricao", l.getDescricao(),
                "ip", l.getIp(),
                "nomeMaquina", l.getNomeMaquina()
        );
    }

    private ResponseEntity<?> guard(HttpServletRequest request) {
        if (adminKey.isBlank()) {
            return null;
        }
        String header = request == null ? null : request.getHeader("X-Admin-Key");
        if (header == null || !adminKey.equals(header)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Admin key inválida"));
        }
        return null;
    }
}
