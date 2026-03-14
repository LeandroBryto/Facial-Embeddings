package memoryguard.repository;


import memoryguard.model.CofreSenhas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CofreSenhasRepository extends JpaRepository<CofreSenhas, Long> {
    Optional<CofreSenhas> findByPerfilUsuarioIdUsuarioAndNomeSistema(Long idUsuario, String nomeSistema);
}
