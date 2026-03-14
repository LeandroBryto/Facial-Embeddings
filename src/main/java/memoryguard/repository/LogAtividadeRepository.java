package memoryguard.repository;

import memoryguard.model.LogAtividade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogAtividadeRepository extends JpaRepository<LogAtividade, Long> {
}
