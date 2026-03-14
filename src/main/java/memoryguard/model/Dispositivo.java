package memoryguard.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TBL_DISPOSITIVO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DISPOSITIVO")
    private Long idDispositivo;

    @Column(name = "NM_DISPOSITIVO", nullable = false)
    private String nomeDispositivo;

    @Column(name = "TX_VERSAO_SO")
    private String versaoSO;

    @Column(name = "NR_IP")
    private String ip;

    @Column(name = "TX_MAC_ADDRESS", nullable = false, unique = true)
    private String macAddress;

    @Column(name = "DT_REGISTRO")
    private LocalDateTime dataRegistro;
}
