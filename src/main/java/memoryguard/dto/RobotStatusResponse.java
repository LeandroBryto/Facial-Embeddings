package memoryguard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RobotStatusResponse {
    private boolean robotEnabled;
    private boolean headless;
    private boolean desktopSupported;
}

