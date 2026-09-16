package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Stockage reel des fichiers de pieces jointes (prompt maitre section 47) :
 * le fichier lui-meme est ecrit sur disque en dehors de PostgreSQL, {@code
 * attachments.storage_path} n'en garde que le chemin relatif. Chemin
 * configurable ({@code app.attachments.storage-dir}, volume Docker dans un
 * deploiement reel) plutot que code en dur, et nom de fichier genere en UUID
 * pour eviter toute collision ou traversee de repertoire a partir du nom
 * original fourni par le client.
 */
@Service
public class AttachmentStorageService {

    private final Path storageDir;

    public AttachmentStorageService(@Value("${app.attachments.storage-dir:./data/attachments}") String storageDir) {
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de creer le repertoire de stockage des pieces jointes : " + this.storageDir, ex);
        }
    }

    public record StoredFile(String storagePath, long sizeBytes) {
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Aucun fichier fourni");
        }
        String extension = extensionOf(file.getOriginalFilename());
        String generatedName = UUID.randomUUID() + extension;
        Path target = storageDir.resolve(generatedName).normalize();
        if (!target.startsWith(storageDir)) {
            // Ne devrait jamais arriver (nom genere par nous, pas par le
            // client), mais defense en profondeur contre toute traversee
            // de repertoire.
            throw new BusinessRuleException("Nom de fichier invalide");
        }
        try {
            file.transferTo(target);
        } catch (IOException ex) {
            throw new IllegalStateException("Echec de l'enregistrement de la piece jointe", ex);
        }
        return new StoredFile(generatedName, file.getSize());
    }

    public byte[] read(String storagePath) {
        Path target = storageDir.resolve(storagePath).normalize();
        if (!target.startsWith(storageDir)) {
            throw new BusinessRuleException("Chemin de piece jointe invalide");
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            throw new BusinessRuleException("Fichier de piece jointe introuvable sur le disque");
        }
    }

    private static String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        String ext = originalFilename.substring(dot).toLowerCase();
        // Borne large mais volontairement stricte : uniquement lettres/chiffres
        // apres le point, jamais de caracteres qui pourraient echapper le
        // repertoire de stockage (deja garde par ailleurs par normalize()+startsWith).
        return ext.matches("\\.[a-z0-9]{1,10}") ? ext : "";
    }
}
