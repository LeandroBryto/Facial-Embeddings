package memoryguard.security;

import memoryguard.exception.EncryptionServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class Encryption {
    private static final byte VERSION_1 = 1;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec masterKey;
    private final byte[] aadBytes;

    public Encryption(
            @Value("${memoryguard.crypto.master-key-base64:}") String masterKeyBase64,
            @Value("${memoryguard.crypto.aad:memoryguard}") String aad
    ) {
        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            throw new IllegalStateException("Propriedade obrigatória ausente: memoryguard.crypto.master-key-base64");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(masterKeyBase64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("memoryguard.crypto.master-key-base64 deve estar em Base64 válido", ex);
        }

        if (keyBytes.length != 32) {
            throw new IllegalStateException("memoryguard.crypto.master-key-base64 deve decodificar para 32 bytes (AES-256)");
        }

        this.masterKey = new SecretKeySpec(keyBytes, "AES");
        this.aadBytes = aad == null ? null : aad.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] encrypt(String data) {
        if (data == null) {
            throw new IllegalArgumentException("data não pode ser null");
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            if (aadBytes != null && aadBytes.length > 0) {
                cipher.updateAAD(aadBytes);
            }

            byte[] ciphertext = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            ByteBuffer out = ByteBuffer.allocate(1 + IV_LENGTH_BYTES + ciphertext.length);
            out.put(VERSION_1);
            out.put(iv);
            out.put(ciphertext);
            return out.array();
        } catch (Exception ex) {
            throw new EncryptionServiceException("Falha ao criptografar", ex);
        }
    }

    public String decrypt(byte[] encryptedData) {
        if (encryptedData == null) {
            throw new IllegalArgumentException("encryptedData não pode ser null");
        }

        if (encryptedData.length < 1 + IV_LENGTH_BYTES + 16) {
            throw new EncryptionServiceException("Payload criptografado inválido");
        }

        try {
            ByteBuffer in = ByteBuffer.wrap(encryptedData);
            byte version = in.get();
            if (version != VERSION_1) {
                throw new EncryptionServiceException("Versão de payload não suportada: " + version);
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            in.get(iv);

            byte[] ciphertext = new byte[in.remaining()];
            in.get(ciphertext);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            if (aadBytes != null && aadBytes.length > 0) {
                cipher.updateAAD(aadBytes);
            }

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (EncryptionServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new EncryptionServiceException("Falha ao descriptografar", ex);
        }
    }

    public String encryptToBase64(String data) {
        return Base64.getEncoder().encodeToString(encrypt(data));
    }

    public String decryptFromBase64(String encryptedBase64) {
        if (encryptedBase64 == null) {
            throw new IllegalArgumentException("encryptedBase64 não pode ser null");
        }
        return decrypt(Base64.getDecoder().decode(encryptedBase64));
    }
}
