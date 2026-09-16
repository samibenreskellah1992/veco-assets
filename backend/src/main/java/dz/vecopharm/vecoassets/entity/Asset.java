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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Immobilisation. Suppression logique uniquement ({@link #deleted}) - voir
 * prompt maitre section 26. Toute modification consequente (localisation,
 * affectation, etat, statut) doit s'accompagner d'une ecriture dans
 * {@link AssetMovement} et/ou {@link AssetStatusHistory} par le service
 * layer ; cette entite ne porte que l'etat courant.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assets")
public class Asset extends BaseEntity {

    // --- Identification ---------------------------------------------------
    @Column(name = "asset_code", nullable = false, unique = true, length = 30)
    private String assetCode;

    // --- Designation ---------------------------------------------------
    @Column(nullable = false, length = 255)
    private String designation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private AssetCategory category;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String model;

    @Column(name = "serial_number", length = 150)
    private String serialNumber;

    // --- Localisation courante ---------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id")
    private Floor floor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    // --- Affectation courante ---------------------------------------------------
    @Column(length = 150)
    private String direction;

    @Column(length = 150)
    private String department;

    @Column(length = 150)
    private String service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_user_id")
    private User currentUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private User responsibleUser;

    // --- Acquisition ---------------------------------------------------
    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    @Column(length = 150)
    private String supplier;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "acquisition_value", precision = 14, scale = 2)
    private BigDecimal acquisitionValue;

    @Column(name = "commissioning_date")
    private LocalDate commissioningDate;

    @Column(name = "warranty_until")
    private LocalDate warrantyUntil;

    // --- Etat / statut ---------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetCondition condition = AssetCondition.BON;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetStatus status = AssetStatus.EN_STOCK;

    // --- Complementaire ---------------------------------------------------
    @Column(columnDefinition = "text")
    private String comment;

    @Column(nullable = false)
    private boolean labeled = false;

    @Column(name = "last_inventory_at")
    private Instant lastInventoryAt;

    // --- Suppression logique ---------------------------------------------------
    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
