package memoryguard.service;

import java.time.LocalDateTime;

import memoryguard.exception.CredentialNotFoundException;
import memoryguard.security.Encryption;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Data;
import memoryguard.model.CofreSenhas;
import memoryguard.model.PerfilUsuario;
import memoryguard.repository.CofreSenhasRepository;
import memoryguard.repository.PerfilUsuarioRepository;

import java.time.LocalDateTime;

@Service
public class VaultService {

    private final CofreSenhasRepository cofreSenhasRepository;
    private final PerfilUsuarioRepository perfilUsuarioRepository;
    private final Encryption encryption;

    public VaultService(
            CofreSenhasRepository cofreSenhasRepository,
            PerfilUsuarioRepository perfilUsuarioRepository,
            Encryption encryption
    ) {
        this.cofreSenhasRepository = cofreSenhasRepository;
        this.perfilUsuarioRepository = perfilUsuarioRepository;
        this.encryption = encryption;
    }

    public CofreSenhas storeCredential(Long userId, String system, String login, String plainPassword, String url) {
        PerfilUsuario perfil = perfilUsuarioRepository.findById(userId)
                .orElseThrow(() -> new CredentialNotFoundException("Usuário não encontrado: id=" + userId));

        String encrypted = encryption.encryptToBase64(plainPassword);
        CofreSenhas entity = cofreSenhasRepository
                .findByPerfilUsuarioIdUsuarioAndNomeSistema(userId, system)
                .orElse(CofreSenhas.builder()
                        .perfilUsuario(perfil)
                        .nomeSistema(system)
                        .dataCriacao(LocalDateTime.now())
                        .build());

        entity.setLogin(login);
        entity.setSenhaCriptografada(encrypted);
        entity.setUrlAcesso(url);
        entity.setDataAtualizacao(LocalDateTime.now());

        return cofreSenhasRepository.save(entity);
    }

    public DecryptedCredential loadCredential(Long userId, String system) {
        CofreSenhas credencial = cofreSenhasRepository
                .findByPerfilUsuarioIdUsuarioAndNomeSistema(userId, system)
                .orElseThrow(() -> new CredentialNotFoundException("Credencial não encontrada para system=" + system));

        String senha = encryption.decryptFromBase64(credencial.getSenhaCriptografada());
        return new DecryptedCredential(credencial.getLogin(), senha, credencial.getUrlAcesso());
    }

    @Data
    @AllArgsConstructor
    public static class DecryptedCredential {
        private String login;
        private String password;
        private String url;
    }
}

