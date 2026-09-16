package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.AssetCategoryDto;
import dz.vecopharm.vecoassets.dto.AssetCategoryRequest;
import dz.vecopharm.vecoassets.service.AssetCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/asset-categories")
public class AssetCategoryController {

    private final AssetCategoryService categoryService;

    public AssetCategoryController(AssetCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<AssetCategoryDto> findAll(@RequestParam(name = "parentId", required = false) UUID parentId) {
        return categoryService.findAll(parentId);
    }

    @GetMapping("/{id}")
    public AssetCategoryDto findById(@PathVariable UUID id) {
        return categoryService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public ResponseEntity<AssetCategoryDto> create(@Valid @RequestBody AssetCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public AssetCategoryDto update(@PathVariable UUID id, @Valid @RequestBody AssetCategoryRequest request) {
        return categoryService.update(id, request);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public AssetCategoryDto activate(@PathVariable UUID id) {
        return categoryService.setActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public AssetCategoryDto deactivate(@PathVariable UUID id) {
        return categoryService.setActive(id, false);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
