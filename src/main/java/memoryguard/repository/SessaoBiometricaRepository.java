package memoryguard.repository;

import java.time.LocalDateTime;
import java.util.List;

import memoryguard.model.SessaoBiometrica;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SessaoBiometricaRepository extends JpaRepository<SessaoBiometrica, Long> {
    List<SessaoBiometrica> findByPerfilUsuario_IdUsuarioAndStatusSessao(Long userId, String statusSessao);

    List<SessaoBiometrica> findTop20ByPerfilUsuario_IdUsuarioOrderByInicioSessaoDesc(Long userId);

    List<SessaoBiometrica> findByStatusSessaoAndUltimaFaceDetectadaBefore(String statusSessao, LocalDateTime cutoff);

    long countByStatusSessao(String statusSessao);

    long countByInicioSessaoAfter(LocalDateTime after);

    @Query("select s.perfilUsuario.idUsuario, count(s) from SessaoBiometrica s group by s.perfilUsuario.idUsuario order by count(s) desc")
    List<Object[]> countSessionsByUser(Pageable pageable);
}
