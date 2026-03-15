package memoryguard.service;

import java.net.InetAddress;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import memoryguard.model.LogAtividade;
import memoryguard.repository.LogAtividadeRepository;

@Service
public class AuditService {

    private final LogAtividadeRepository logAtividadeRepository;

    public AuditService(LogAtividadeRepository logAtividadeRepository) {
        this.logAtividadeRepository = logAtividadeRepository;
    }

    public void success(String descricao, HttpServletRequest request) {
        save(descricao, "SUCCESS", request);
    }

    public void fail(String descricao, HttpServletRequest request) {
        save(descricao, "FAIL", request);
    }

    private void save(String descricao, String status, HttpServletRequest request) {
        if (descricao == null || descricao.isBlank()) {
            return;
        }
        LogAtividade log = LogAtividade.builder()
                .descricao(descricao)
                .dataEvento(LocalDateTime.now())
                .status(status == null || status.isBlank() ? "UNKNOWN" : status)
                .ip(resolveIp(request))
                .nomeMaquina(resolveHostname())
                .build();
        logAtividadeRepository.save(log);
    }

    private static String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int idx = forwardedFor.indexOf(',');
            return (idx > 0 ? forwardedFor.substring(0, idx) : forwardedFor).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String remote = request.getRemoteAddr();
        return (remote == null || remote.isBlank()) ? "unknown" : remote;
    }

    private static String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
