package memoryguard.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TBL_CREDENCIAL_SESSAO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredencialSessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CREDENCIAL")
    private Long idCredencial;

    @ManyToOne
    @JoinColumn(name = "ID_USUARIO", nullable = false)
    private PerfilUsuario perfilUsuario;

    @Column(name = "NM_SISTEMA", nullable = false)
    private String nomeSistema;

    @Column(name = "TX_SENHA_TEMPORARIA", nullable = false)
    private String senhaTemporaria;

    @Column(name = "DT_CRIACAO", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "DT_EXPIRACAO", nullable = false)
    private LocalDateTime dataExpiracao;
}
