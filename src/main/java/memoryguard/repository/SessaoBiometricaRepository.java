package memoryguard.repository;
import memoryguard.model.SessaoBiometrica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessaoBiometricaRepository extends JpaRepository<SessaoBiometrica, Long> {
}
