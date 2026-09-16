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

/**
 * Anomalie detectee pendant un inventaire. {@code asset} est nullable
 * (une anomalie NON_REFERENCEE peut ne correspondre a aucun bien connu).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory_anomalies")
public class InventoryAnomaly extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private InventoryCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id")
    private InventoryScan scan;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", nullable = false, length = 40)
    private AnomalyType anomalyType;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    private User reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnomalyStatus status = AnomalyStatus.NOUVELLE;
}
