package memoryguard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VaultCredentialRequest {
    private Long userId;
    private String system;
    private String login;
    private String password;
    private String url;
}

