package memoryguard.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TBL_SESSAO_BIOMETRICA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessaoBiometrica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_SESSAO")
    private Long idSessao;

    @ManyToOne
    @JoinColumn(name = "ID_USUARIO", nullable = false)
    private PerfilUsuario perfilUsuario;

    @Column(name = "DT_INICIO_SESSAO", nullable = false)
    private LocalDateTime inicioSessao;

    @Column(name = "DT_ULTIMA_FACE_DETECTADA")
    private LocalDateTime ultimaFaceDetectada;

    @Column(name = "ST_STATUS_SESSAO", nullable = false)
    private String statusSessao;

    @Column(name = "NR_PONTUACAO_CONFIANCA")
    private Double pontuacaoConfianca;
}
