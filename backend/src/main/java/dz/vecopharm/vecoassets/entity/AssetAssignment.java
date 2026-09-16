package dz.vecopharm.vecoassets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Historique des affectations d'une immobilisation. Au plus une ligne par
 * immobilisation a {@code assignedUntil == null} (affectation courante) -
 * contrainte garantie par un index unique partiel en base.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "asset_assignments")
public class AssetAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 150)
    private String direction;

    @Column(length = 150)
    private String department;

    @Column(length = 150)
    private String service;

    @Column(name = "assigned_from", nullable = false)
    private Instant assignedFrom;

    @Column(name = "assigned_until")
    private Instant assignedUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Column(columnDefinition = "text")
    private String comment;
}
