package memoryguard.service;

import memoryguard.exception.RobotServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.time.Duration;

@Service
public class RobotService {

    private final Duration stepDelay;
    private final boolean enabled;

    public RobotService(
            @Value("${memoryguard.robot.step-delay-ms:120}") long stepDelayMs,
            @Value("${memoryguard.robot.enabled:true}") boolean enabled
    ) {
        this.stepDelay = Duration.ofMillis(stepDelayMs);
        this.enabled = enabled;
    }

    public void openBrowser(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        if (!enabled) {
            throw new RobotServiceException("Automação desabilitada (memoryguard.robot.enabled=false)");
        }
        if (GraphicsEnvironment.isHeadless()) {
            throw new RobotServiceException("Ambiente headless: execute em um desktop com GUI (spring.main.headless=false)");
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new RobotServiceException("Desktop não suportado: não foi possível abrir navegador");
            }
            Desktop.getDesktop().browse(URI.create(url));
            sleep(stepDelay.multipliedBy(10));
        } catch (RobotServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RobotServiceException("Falha ao abrir navegador", ex);
        }
    }

    public void performLogin(String login, String senha) {
        if (!enabled) {
            throw new RobotServiceException("Automação desabilitada (memoryguard.robot.enabled=false)");
        }
        if (GraphicsEnvironment.isHeadless()) {
            throw new RobotServiceException("Ambiente headless: execute em um desktop com GUI (spring.main.headless=false)");
        }
        try {
            Robot robot = new Robot();
            robot.setAutoWaitForIdle(true);

            paste(robot, login);
            sleep(stepDelay);
            press(robot, KeyEvent.VK_TAB);
            sleep(stepDelay);

            paste(robot, senha);
            sleep(stepDelay);
            press(robot, KeyEvent.VK_ENTER);
        } catch (RobotServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RobotServiceException("Falha na automação de login", ex);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static void paste(Robot robot, String text) {
        if (text == null) {
            text = "";
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        press(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_V);
    }

    private static void press(Robot robot, int keyCode) {
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
    }

    private static void press(Robot robot, int modifier, int key) {
        robot.keyPress(modifier);
        robot.keyPress(key);
        robot.keyRelease(key);
        robot.keyRelease(modifier);
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RobotServiceException("Interrompido durante automação", e);
        }
    }
}

