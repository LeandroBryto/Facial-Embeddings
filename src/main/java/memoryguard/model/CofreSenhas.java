package memoryguard.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "TBL_COFRE_SENHAS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CofreSenhas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_COFRE")
    private Long idCofre;

    @ManyToOne
    @JoinColumn(name = "ID_USUARIO", nullable = false)
    private PerfilUsuario perfilUsuario;

    @Column(name = "NM_SISTEMA", nullable = false)
    private String nomeSistema;

    @Column(name = "TX_LOGIN", nullable = false)
    private String login;

    @Column(name = "TX_SENHA_CRIPTOGRAFADA", nullable = false)
    private String senhaCriptografada;

    @Column(name = "TX_URL_ACESSO")
    private String urlAcesso;

    @Column(name = "DT_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name = "DT_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;
}
