package memoryguard.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import memoryguard.model.CredencialSessao;

@Repository
public interface CredencialSessaoRepository extends JpaRepository<CredencialSessao, Long> {
    Optional<CredencialSessao> findTopByPerfilUsuario_IdUsuarioAndNomeSistemaAndDataExpiracaoAfterOrderByDataCriacaoDesc(
            Long userId,
            String nomeSistema,
            LocalDateTime now
    );

    long deleteByDataExpiracaoBefore(LocalDateTime now);
}
