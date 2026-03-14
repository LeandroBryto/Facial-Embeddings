package memoryguard.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TBL_LOG_ATIVIDADE")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAtividade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_LOG")
    private Long idLog;

    @Column(name = "TX_DESCRICAO", nullable = false)
    private String descricao;

    @Column(name = "DT_EVENTO", nullable = false)
    private LocalDateTime dataEvento;

    @Column(name = "ST_STATUS", nullable = false)
    private String status;

    @Column(name = "NR_IP", nullable = false)
    private String ip;

    @Column(name = "NM_MAQUINA", nullable = false)
    private String nomeMaquina;
}
