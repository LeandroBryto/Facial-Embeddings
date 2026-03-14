package memoryguard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceEnrollRequest {
    private Long userId;
    private String imageBase64;
}

