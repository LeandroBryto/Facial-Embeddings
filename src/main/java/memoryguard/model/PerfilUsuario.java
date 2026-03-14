package memoryguard.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "TBL_PERFIL_USUARIO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_USUARIO")
    private Long idUsuario;

    @Column(name = "NM_USUARIO", nullable = false)
    private String nome;

    @Column(name = "TX_TEMPLATE_FACIAL", columnDefinition = "TEXT")
    private String templateFacial;

    @Lob
    @Column(name = "BL_FACE_TEMPLATE", columnDefinition = "LONGBLOB")
    private byte[] faceTemplate;

    @Lob
    @Column(name = "BL_FACE_IMAGE", columnDefinition = "LONGBLOB")
    private byte[] faceImage;

    @Column(name = "DT_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name = "DT_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @Column(name = "ST_STATUS")
    private String status;

    @OneToMany(mappedBy = "perfilUsuario", cascade = CascadeType.ALL)
    private List<CofreSenhas> cofres;
}
