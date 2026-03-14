package memoryguard.service;

import memoryguard.exception.VisionException;
import memoryguard.model.PerfilUsuario;
import memoryguard.repository.PerfilUsuarioRepository;
import memoryguard.vision.CameraService;
import memoryguard.vision.FaceDetector;
import memoryguard.vision.OpenCvNativeLoader;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FaceRecognitionService {

    private final CameraService cameraService;
    private final FaceDetector faceDetector;
    private final PerfilUsuarioRepository perfilUsuarioRepository;
    private final double minConfidence;
    private final int templateSize;

    public FaceRecognitionService(
            CameraService cameraService,
            FaceDetector faceDetector,
            PerfilUsuarioRepository perfilUsuarioRepository,
            @Value("${memoryguard.face.min-confidence:0.85}") double minConfidence,
            @Value("${memoryguard.face.template-size:100}") int templateSize
    ) {
        this.cameraService = cameraService;
        this.faceDetector = faceDetector;
        this.perfilUsuarioRepository = perfilUsuarioRepository;
        this.minConfidence = minConfidence;
        this.templateSize = templateSize;
    }

    public Mat captureFrame() {
        return cameraService.captureFrame();
    }

    public Mat detectFace(Mat frame) {
        Rect rect = detectFaceRect(frame);
        if (rect == null) {
            return null;
        }
        return new Mat(frame, rect);
    }

    public Rect detectFaceRect(Mat frame) {
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);
        try {
            return faceDetector.detectFace(gray);
        } catch (VisionException ex) {
            return new Rect(0, 0, gray.cols(), gray.rows());
        }
    }

    public byte[] extractFaceTemplate(Mat faceRoi) {
        if (faceRoi == null || faceRoi.empty()) {
            throw new VisionException("ROI facial inválida para extração de template");
        }

        Mat gray = new Mat();
        Imgproc.cvtColor(faceRoi, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.resize(gray, gray, new Size(templateSize, templateSize));

        Mat floatMat = new Mat();
        gray.convertTo(floatMat, CvType.CV_32F, 1.0 / 255.0);
        float[] vector = new float[(int) (floatMat.total() * floatMat.channels())];
        floatMat.get(0, 0, vector);

        normalizeInPlace(vector);
        return floatVectorToBytes(vector);
    }

    public double compareFaces(byte[] templateA, byte[] templateB) {
        if (templateA == null || templateB == null) {
            return 0.0;
        }
        float[] a = bytesToFloatVector(templateA);
        float[] b = bytesToFloatVector(templateB);
        if (a.length == 0 || a.length != b.length) {
            return 0.0;
        }
        return cosineSimilarity(a, b);
    }

    public AuthenticationResult authenticateFromWebcam() {
        Mat frame = captureFrame();
        Mat face = detectFace(frame);
        if (face == null) {
            return AuthenticationResult.notAuthorized(0.0);
        }

        byte[] probeTemplate = extractFaceTemplate(face);
        List<PerfilUsuario> perfis = perfilUsuarioRepository.findAll();

        PerfilUsuario bestUser = null;
        double best = 0.0;

        for (PerfilUsuario perfil : perfis) {
            byte[] enrolled = perfil.getFaceTemplate();
            if (enrolled == null || enrolled.length == 0) {
                continue;
            }
            double confidence = compareFaces(probeTemplate, enrolled);
            if (confidence > best) {
                best = confidence;
                bestUser = perfil;
            }
        }

        if (bestUser == null || best < minConfidence) {
            return AuthenticationResult.notAuthorized(best);
        }

        return AuthenticationResult.authorized(bestUser, best);
    }

    public AuthenticationResult authenticateFromImageBase64(String imageBase64) {
        if (imageBase64 == null || imageBase64.isBlank()) {
            throw new VisionException("imageBase64 é obrigatório");
        }
        byte[] bytes = Base64.getDecoder().decode(imageBase64);
        return authenticateFromImageBytes(bytes);
    }

    public AuthenticationResult authenticateFromImageBytes(byte[] imageBytes) {
        Mat frame = decodeImage(imageBytes);
        Mat face = detectFace(frame);
        if (face == null) {
            return AuthenticationResult.notAuthorized(0.0);
        }

        byte[] probeTemplate = extractFaceTemplate(face);
        List<PerfilUsuario> perfis = perfilUsuarioRepository.findAll();

        PerfilUsuario bestUser = null;
        double best = 0.0;

        for (PerfilUsuario perfil : perfis) {
            byte[] enrolled = perfil.getFaceTemplate();
            if (enrolled == null || enrolled.length == 0) {
                continue;
            }
            double confidence = compareFaces(probeTemplate, enrolled);
            if (confidence > best) {
                best = confidence;
                bestUser = perfil;
            }
        }

        if (bestUser == null || best < minConfidence) {
            return AuthenticationResult.notAuthorized(best);
        }

        return AuthenticationResult.authorized(bestUser, best);
    }

    public AuthenticationResult authenticateUserFromImageBase64(Long userId, String imageBase64) {
        if (userId == null) {
            throw new VisionException("userId é obrigatório");
        }
        if (imageBase64 == null || imageBase64.isBlank()) {
            throw new VisionException("imageBase64 é obrigatório");
        }
        byte[] bytes = Base64.getDecoder().decode(imageBase64);
        return authenticateUserFromImageBytes(userId, bytes);
    }

    public AuthenticationResult authenticateUserFromImageBytes(Long userId, byte[] imageBytes) {
        PerfilUsuario perfil = perfilUsuarioRepository.findById(userId)
                .orElseThrow(() -> new VisionException("Usuário não encontrado: id=" + userId));

        if (perfil.getFaceTemplate() == null || perfil.getFaceTemplate().length == 0) {
            return AuthenticationResult.notAuthorized(0.0);
        }

        Mat frame = decodeImage(imageBytes);
        Mat face = detectFace(frame);
        if (face == null) {
            return AuthenticationResult.notAuthorized(0.0);
        }

        byte[] probeTemplate = extractFaceTemplate(face);
        double confidence = compareFaces(probeTemplate, perfil.getFaceTemplate());
        if (confidence < minConfidence) {
            return AuthenticationResult.notAuthorized(confidence);
        }
        return AuthenticationResult.authorized(perfil, confidence);
    }

    public PerfilUsuario enrollFromImageBase64(Long userId, String imageBase64) {
        if (userId == null) {
            throw new VisionException("userId é obrigatório");
        }
        if (imageBase64 == null || imageBase64.isBlank()) {
            throw new VisionException("imageBase64 é obrigatório");
        }
        byte[] bytes = Base64.getDecoder().decode(imageBase64);
        return enrollFromImageBytes(userId, bytes);
    }

    public PerfilUsuario enrollFromImageBytes(Long userId, byte[] imageBytes) {
        Mat frame = decodeImage(imageBytes);
        Mat face = detectFace(frame);
        if (face == null) {
            throw new VisionException("Nenhum rosto detectado");
        }

        byte[] template = extractFaceTemplate(face);
        PerfilUsuario perfil = perfilUsuarioRepository.findById(userId)
                .orElseGet(() -> perfilUsuarioRepository.save(PerfilUsuario.builder().nome("Usuário").build()));
        perfil.setFaceTemplate(template);
        perfil.setFaceImage(imageBytes);
        return perfilUsuarioRepository.save(perfil);
    }

    private Mat decodeImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new VisionException("Imagem vazia");
        }
        OpenCvNativeLoader.load();
        Mat mat = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
        if (mat == null || mat.empty()) {
            throw new VisionException("Falha ao decodificar imagem");
        }
        return mat;
    }

    public byte[] floatVectorToBytes(float[] vector) {
        Objects.requireNonNull(vector, "vector");
        ByteBuffer bb = ByteBuffer.allocate(4 + (vector.length * 4)).order(ByteOrder.BIG_ENDIAN);
        bb.putInt(vector.length);
        for (float v : vector) {
            bb.putFloat(v);
        }
        return bb.array();
    }

    public float[] bytesToFloatVector(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return new float[0];
        }
        try {
            ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            int len = bb.getInt();
            if (len < 0 || (4 + (len * 4)) != bytes.length) {
                return new float[0];
            }
            float[] out = new float[len];
            for (int i = 0; i < len; i++) {
                out[i] = bb.getFloat();
            }
            return out;
        } catch (Exception ex) {
            return new float[0];
        }
    }

    private static void normalizeInPlace(float[] v) {
        double sum = 0.0;
        for (float x : v) {
            sum += (double) x * x;
        }
        double norm = Math.sqrt(sum);
        if (norm <= 0.0) {
            return;
        }
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) (v[i] / norm);
        }
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
        }
        if (dot < 0.0) {
            return 0.0;
        }
        if (dot > 1.0) {
            return 1.0;
        }
        return dot;
    }

    public static final class AuthenticationResult {

        private final boolean authorized;
        private final PerfilUsuario user;
        private final double confidence;

        private AuthenticationResult(boolean authorized, PerfilUsuario user, double confidence) {
            this.authorized = authorized;
            this.user = user;
            this.confidence = confidence;
        }

        public boolean authorized() {
            return authorized;
        }

        public PerfilUsuario user() {
            return user;
        }

        public double confidence() {
            return confidence;
        }

        public static AuthenticationResult authorized(PerfilUsuario user, double confidence) {
            return new AuthenticationResult(true, user, confidence);
        }

        public static AuthenticationResult notAuthorized(double confidence) {
            return new AuthenticationResult(false, null, confidence);
        }
    }
}

