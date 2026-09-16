package dz.vecopharm.vecoassets.security;

import dz.vecopharm.vecoassets.entity.Permission;
import dz.vecopharm.vecoassets.entity.Role;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.entity.UserStatus;
import dz.vecopharm.vecoassets.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Used only during login (by {@code DaoAuthenticationProvider}, wired
 * automatically once this bean and a {@code PasswordEncoder} bean exist).
 * Every request after that is authenticated from the JWT's own claims by
 * {@link JwtAuthenticationFilter} - this class never runs per-request.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(buildAuthorities(user))
                .disabled(user.getStatus() != UserStatus.ACTIVE)
                .build();
    }

    /** ROLE_&lt;code&gt; for each role, plus each permission code attached to that role, flattened. */
    private Set<GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getCode()));
            }
        }
        return authorities;
    }
}
