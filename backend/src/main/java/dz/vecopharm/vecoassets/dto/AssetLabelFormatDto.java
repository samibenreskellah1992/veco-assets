package dz.vecopharm.vecoassets.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AssetLabelFormatDto(
        UUID id,
        String code,
        String name,
        BigDecimal widthMm,
        BigDecimal heightMm,
        boolean showLogo,
        boolean showShortDesignation,
        boolean showQrCode,
        boolean showBarcode,
        boolean active
) {
}
