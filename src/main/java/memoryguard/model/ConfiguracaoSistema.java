package memoryguard.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TBL_CONFIGURACAO_SISTEMA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CONFIGURACAO")
    private Long idConfiguracao;

    @Column(name = "TX_CHAVE", nullable = false, unique = true)
    private String chave;

    @Column(name = "TX_VALOR", nullable = false)
    private String valor;

    @Column(name = "TX_DESCRICAO")
    private String descricao;

    @Column(name = "DT_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;
}
