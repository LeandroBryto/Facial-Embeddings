package memoryguard.vision;

import memoryguard.exception.VisionException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class FaceDetector {

    private final String cascadePath;
    private final String cascadeResource;
    private volatile CascadeClassifier classifier;

    public FaceDetector(
            @Value("${memoryguard.vision.cascade-path:}") String cascadePath,
            @Value("${memoryguard.vision.cascade-resource:opencv/haarcascade_frontalface_alt.xml}") String cascadeResource
    ) {
        this.cascadePath = cascadePath == null ? "" : cascadePath.trim();
        this.cascadeResource = cascadeResource;
    }

    public Rect detectFace(Mat frame) {
        if (frame == null || frame.empty()) {
            throw new VisionException("Frame inválido para detecção facial");
        }

        CascadeClassifier c = getOrCreateClassifier();
        MatOfRect faces = new MatOfRect();
        c.detectMultiScale(frame, faces);
        Rect[] rects = faces.toArray();
        if (rects.length == 0) {
            return null;
        }

        Rect best = rects[0];
        for (int i = 1; i < rects.length; i++) {
            Rect r = rects[i];
            if (r.area() > best.area()) {
                best = r;
            }
        }
        return best;
    }

    private CascadeClassifier getOrCreateClassifier() {
        CascadeClassifier local = classifier;
        if (local != null) {
            return local;
        }

        synchronized (this) {
            if (classifier != null) {
                return classifier;
            }

            OpenCvNativeLoader.load();

            String pathToUse = cascadePath;
            if (pathToUse.isBlank()) {
                pathToUse = extractCascadeFromResource(cascadeResource).toAbsolutePath().toString();
            }

            CascadeClassifier loaded = new CascadeClassifier(pathToUse);
            if (loaded.empty()) {
                throw new VisionException("Falha ao carregar CascadeClassifier: " + pathToUse);
            }

            classifier = loaded;
            return loaded;
        }
    }

    private Path extractCascadeFromResource(String resource) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                throw new VisionException("Cascade não encontrado no classpath: " + resource + ". Configure memoryguard.vision.cascade-path apontando para o .xml do OpenCV.");
            }

            Path tmp = Files.createTempFile("memoryguard-cascade-", ".xml");
            tmp.toFile().deleteOnExit();
            Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
            return tmp;
        } catch (Exception ex) {
            throw new VisionException("Falha ao preparar cascade de detecção facial", ex);
        }
    }
}

