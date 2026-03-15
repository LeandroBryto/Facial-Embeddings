package memoryguard.controller;

import jakarta.servlet.http.HttpServletRequest;
import memoryguard.model.Dispositivo;
import memoryguard.repository.DispositivoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/devices")
public class AdminDeviceController {

    private final DispositivoRepository dispositivoRepository;
    private final String adminKey;

    public AdminDeviceController(
            DispositivoRepository dispositivoRepository,
            @Value("${memoryguard.admin.key:}") String adminKey
    ) {
        this.dispositivoRepository = dispositivoRepository;
        this.adminKey = adminKey == null ? "" : adminKey;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest request) {
        ResponseEntity<?> guard = guard(request);
        if (guard != null) {
            return guard;
        }
        List<Dispositivo> all = dispositivoRepository.findAll();
        return ResponseEntity.ok(all);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        ResponseEntity<?> guard = guard(request);
        if (guard != null) {
            return guard;
        }
        String nome = body == null ? null : String.valueOf(body.getOrDefault("nomeDispositivo", "")).trim();
        String versaoSO = body == null ? null : String.valueOf(body.getOrDefault("versaoSO", "")).trim();
        String ip = body == null ? null : String.valueOf(body.getOrDefault("ip", "")).trim();
        String mac = body == null ? null : String.valueOf(body.getOrDefault("macAddress", "")).trim();
        if (nome == null || nome.isBlank() || mac == null || mac.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "BAD_REQUEST", "message", "nomeDispositivo e macAddress são obrigatórios"));
        }
        Dispositivo entity = Dispositivo.builder()
                .nomeDispositivo(nome)
                .versaoSO(versaoSO)
                .ip(ip)
                .macAddress(mac)
                .dataRegistro(LocalDateTime.now())
                .build();
        Dispositivo saved = dispositivoRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        ResponseEntity<?> guard = guard(request);
        if (guard != null) {
            return guard;
        }
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!dispositivoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dispositivoRepository.deleteById(id);
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
}
