package dz.vecopharm.vecoassets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Feuille de la hierarchie de localisation (ex: "Bureau 204").
 *
 * <p>Checkpoint 1 de l'evolution "locaux scannables" (2026-09, prompt
 * detaille de Sami section 26 : reutiliser/enrichir l'existant plutot que
 * dupliquer) : {@code qrCode}, {@code status}, {@code description},
 * {@code responsibleUser} et {@code lastInventoryAt} font de ce meme
 * local un point d'entree scannable pour l'inventaire (comparaison
 * attendu/scanne par local, phases suivantes) sans toucher au CRUD
 * referentiel deja en place ({@code active}/activate/deactivate
 * inchanges).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "locations")
public class Location extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    /** Statut operationnel du local (distinct du booleen {@link #active} ci-dessus - voir {@link LocationStatus}). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LocationStatus status = LocationStatus.ACTIF;

    /** Code unique genere a la creation (LocationCodeGenerator), jamais modifie ensuite - identifiant du QR imprimable. */
    @Column(name = "qr_code", unique = true, length = 50)
    private String qrCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private User responsibleUser;

    @Column(name = "last_inventory_at")
    private Instant lastInventoryAt;
}
