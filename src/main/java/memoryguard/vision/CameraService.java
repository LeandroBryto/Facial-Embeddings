package memoryguard.vision;

import memoryguard.exception.VisionException;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CameraService implements AutoCloseable {

    private final int cameraIndex;
    private VideoCapture capture;

    public CameraService(@Value("${memoryguard.vision.camera-index:0}") int cameraIndex) {
        this.cameraIndex = cameraIndex;
    }

    public synchronized Mat captureFrame() {
        OpenCvNativeLoader.load();

        if (capture == null) {
            capture = new VideoCapture(cameraIndex);
        }

        if (!capture.isOpened()) {
            if (!capture.open(cameraIndex)) {
                throw new VisionException("Não foi possível abrir a webcam (index=" + cameraIndex + ")");
            }
        }

        Mat frame = new Mat();
        boolean ok = capture.read(frame);
        if (!ok || frame.empty()) {
            throw new VisionException("Falha ao capturar frame da webcam");
        }
        return frame;
    }

    @Override
    public synchronized void close() {
        if (capture != null) {
            capture.release();
            capture = null;
        }
    }
}

