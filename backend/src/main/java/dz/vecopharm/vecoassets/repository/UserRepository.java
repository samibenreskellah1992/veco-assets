package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMatricule(String matricule);

    /**
     * Eagerly fetches roles and their permissions in one query (both are
     * mapped as {@code Set}, so this double fetch-join does not trigger
     * Hibernate's MultipleBagFetchException). Used at authentication time
     * and by {@code /api/auth/me} so the flattened authority set never
     * needs a lazy-loading round trip outside the original transaction.
     */
    @Query("select distinct u from User u "
            + "left join fetch u.roles r "
            + "left join fetch r.permissions "
            + "where u.email = :email")
    Optional<User> findByEmailWithRolesAndPermissions(@Param("email") String email);
}
