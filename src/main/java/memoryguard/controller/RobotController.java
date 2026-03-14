package memoryguard.controller;

import memoryguard.dto.RobotStatusResponse;
import memoryguard.service.RobotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.*;

@RestController
@RequestMapping("/robot")
public class RobotController {

    private final RobotService robotService;

    public RobotController(RobotService robotService) {
        this.robotService = robotService;
    }

    @GetMapping("/status")
    public ResponseEntity<RobotStatusResponse> status() {
        return ResponseEntity.ok(new RobotStatusResponse(
                robotService.isEnabled(),
                GraphicsEnvironment.isHeadless(),
                Desktop.isDesktopSupported()
        ));
    }
}

