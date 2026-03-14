package memoryguard.repository;

import memoryguard.model.CredencialSessao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CredencialSessaoRepository extends JpaRepository<CredencialSessao, Long> {
}
