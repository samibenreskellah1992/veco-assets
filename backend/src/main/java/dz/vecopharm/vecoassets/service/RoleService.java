package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.dto.RoleDto;
import dz.vecopharm.vecoassets.mapper.RoleMapper;
import dz.vecopharm.vecoassets.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Lecture seule : les 6 roles V1 (prompt maitre section 27) sont fixes, pas de CRUD role en V1. */
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public RoleService(RoleRepository roleRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    @Transactional(readOnly = true)
    public List<RoleDto> findAll() {
        return roleRepository.findAll().stream().map(roleMapper::toDto).toList();
    }
}
