package memoryguard.repository;

import memoryguard.model.LogAtividade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogAtividadeRepository extends JpaRepository<LogAtividade, Long> {
    List<LogAtividade> findTop50ByOrderByDataEventoDesc();

    long countByStatus(String status);

    long countByDataEventoAfter(LocalDateTime after);

    long countByStatusAndDataEventoAfter(String status, LocalDateTime after);
}
