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
 * Trace chaque generation d'etiquette pour un local (Checkpoint 2 de
 * l'evolution "locaux scannables", 2026-09) - mirroir exact de
 * {@link AssetLabel} pour l'etiquetage d'immobilisation (Phase 6), memes
 * garanties : jamais de generation silencieuse, {@code format} reutilise
 * {@link AssetLabelFormat} tel quel (voir migration V16).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "location_labels")
public class LocationLabel extends BaseCreatedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "format_id", nullable = false)
    private AssetLabelFormat format;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}
