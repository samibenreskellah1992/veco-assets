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
 * Mouvement d'une immobilisation (affectation, transfert, maintenance,
 * reforme, ...). Workflow Demande -> Validation -> Execution -> Historisation
 * (prompt maitre section 18) : {@link #status} porte l'etape, {@link Asset}
 * n'est mis a jour que lorsque le mouvement passe a EXECUTE.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "asset_movements")
public class AssetMovement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_site_id")
    private Site fromSite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_site_id")
    private Site toSite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_location_id")
    private Location fromLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_location_id")
    private Location toLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id")
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id")
    private User toUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private User validatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementStatus status = MovementStatus.DEMANDE;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "executed_at")
    private Instant executedAt;
}
