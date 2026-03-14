package memoryguard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceImageResponse {
    private Long userId;
    private Integer imageSizeBytes;
    private String imageBase64;
}

