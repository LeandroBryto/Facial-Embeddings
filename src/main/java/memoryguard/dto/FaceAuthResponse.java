package memoryguard.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceAuthResponse {
    private String status;
    private Long userId;
    private String user;
    private double confidence;
}

