package memoryguard.controller;

import jakarta.servlet.http.HttpServletRequest;
import memoryguard.model.ConfiguracaoSistema;
import memoryguard.repository.ConfiguracaoSistemaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/config")
public class AdminConfigController {

    private final ConfiguracaoSistemaRepository configuracaoSistemaRepository;
    private final String adminKey;

    public AdminConfigController(
            ConfiguracaoSistemaRepository configuracaoSistemaRepository,
            @Value("${memoryguard.admin.key:}") String adminKey
    ) {
        this.configuracaoSistemaRepository = configuracaoSistemaRepository;
        this.adminKey = adminKey == null ? "" : adminKey;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest request) {
        ResponseEntity<?> guard = guard(request);
        if (guard != null) {
            return guard;
        }
        List<ConfiguracaoSistema> all = configuracaoSistemaRepository.findAll().stream()
                .filter(c -> !isSensitiveKey(c.getChave()))
                .toList();
        return ResponseEntity.ok(all);
    }

    @GetMapping("/{chave}")
    public ResponseEntity<?> get(@PathVariable("chave") String chave, HttpServletRequest request) {
        ResponseEntity<?> guard = guard(request);
        if (guard != null) {
            return guard;
        }
        if (isSensitiveKey(chave)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "FORBIDDEN", "message", "Chave protegida"));
        }
        ConfiguracaoSistema cfg = configuracaoSistemaRepository.findByChave(chave).orElse(null);
        if (cfg == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cfg);
    }

    @PutMapping("/{chave}")
    public ResponseEntity<?> upsert(
            @PathVariable("chave") String chave,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        ResponseEntity<?> guard = guard(request);
        if (guard != null) {
            return guard;
        }
        if (isSensitiveKey(chave)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "FORBIDDEN", "message", "Chave protegida"));
        }
        String valor = body == null ? null : String.valueOf(body.getOrDefault("valor", "")).trim();
        String descricao = body == null ? null : String.valueOf(body.getOrDefault("descricao", "")).trim();
        if (valor == null || valor.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "BAD_REQUEST", "message", "valor é obrigatório"));
        }
        ConfiguracaoSistema cfg = configuracaoSistemaRepository.findByChave(chave)
                .orElseGet(() -> ConfiguracaoSistema.builder().chave(chave).build());
        cfg.setValor(valor);
        cfg.setDescricao(descricao);
        cfg.setDataAtualizacao(LocalDateTime.now());
        ConfiguracaoSistema saved = configuracaoSistemaRepository.save(cfg);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{chave}")
    public ResponseEntity<?> delete(@PathVariable("chave") String chave, HttpServletRequest request) {
        ResponseEntity<?> guard = guard(request);
        if (guard != null) {
            return guard;
        }
        if (isSensitiveKey(chave)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "FORBIDDEN", "message", "Chave protegida"));
        }
        ConfiguracaoSistema cfg = configuracaoSistemaRepository.findByChave(chave).orElse(null);
        if (cfg == null) {
            return ResponseEntity.notFound().build();
        }
        configuracaoSistemaRepository.delete(cfg);
        return ResponseEntity.noContent().build();
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

    private static boolean isSensitiveKey(String chave) {
        if (chave == null) {
            return false;
        }
        String k = chave.toLowerCase();
        return k.contains("master-key") || k.contains("password") || k.contains("secret") || k.contains("token");
    }
}
