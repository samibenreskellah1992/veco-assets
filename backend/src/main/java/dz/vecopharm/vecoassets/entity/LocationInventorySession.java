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
 * Session de scan d'inventaire pour UN local (Checkpoint 3 de l'evolution
 * "locaux scannables", 2026-09). Volontairement plus legere qu'une
 * {@link InventoryCampaign} (Phase 7) : pas de dates de debut/fin, pas de
 * responsable de campagne, perimetre reduit a un seul local. Reserve aux
 * "campagnes par local" completes, un checkpoint ulterieur eventuel (voir
 * claude/veco-assets-phase1-status.md), la notion de campagne a part
 * entiere sur plusieurs locaux/plusieurs jours.
 *
 * <p>Les scans et anomalies produits pendant une session referencent cette
 * entite via {@code InventoryScan.locationSession}/{@code
 * InventoryAnomaly.locationSession} (V17) au lieu de {@code campaign},
 * traversant ainsi gratuitement tous les ecrans/API Phase 7 existants
 * (page Anomalies, onglet "Inventaire" d'une immobilisation) sans
 * duplication - prompt maitre section 26.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "location_inventory_sessions")
public class LocationInventorySession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by")
    private User openedBy;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LocationSessionStatus status = LocationSessionStatus.EN_COURS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private User validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;
}
