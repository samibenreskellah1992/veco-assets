package dz.vecopharm.vecoassets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Format d'etiquette administrable (prompt maitre section 13) : dimensions
 * et contenu ne sont jamais codes en dur cote frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "asset_label_formats")
public class AssetLabelFormat extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "width_mm", nullable = false, precision = 6, scale = 2)
    private BigDecimal widthMm;

    @Column(name = "height_mm", nullable = false, precision = 6, scale = 2)
    private BigDecimal heightMm;

    @Column(name = "show_logo", nullable = false)
    private boolean showLogo = true;

    @Column(name = "show_short_designation", nullable = false)
    private boolean showShortDesignation = true;

    @Column(name = "show_qr_code", nullable = false)
    private boolean showQrCode = true;

    @Column(name = "show_barcode", nullable = false)
    private boolean showBarcode = false;

    @Column(nullable = false)
    private boolean active = true;
}
