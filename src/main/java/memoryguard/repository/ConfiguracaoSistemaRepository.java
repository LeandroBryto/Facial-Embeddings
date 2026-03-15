package memoryguard.repository;


import memoryguard.model.ConfiguracaoSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracaoSistemaRepository extends JpaRepository<ConfiguracaoSistema, Long> {
    Optional<ConfiguracaoSistema> findByChave(String chave);
}
