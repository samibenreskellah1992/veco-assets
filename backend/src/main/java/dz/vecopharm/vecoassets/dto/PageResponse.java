package dz.vecopharm.vecoassets.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Enveloppe de pagination generique, independante de Spring Data
 * ({@code org.springframework.data.domain.Page} ne se serialise pas de
 * facon stable/documentable en JSON) - premiere utilisation en Phase 5
 * (liste des immobilisations), reutilisable pour toute liste paginee
 * future (inventaires, mouvements...).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
