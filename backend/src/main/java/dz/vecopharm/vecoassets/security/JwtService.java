package dz.vecopharm.vecoassets.security;

import dz.vecopharm.vecoassets.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;

/**
 * Issues and validates the stateless JWT used for API authentication
 * (prompt maitre section 33). Authorities (ROLE_xxx + permission codes) are
 * embedded in the token at login time so that {@link JwtAuthenticationFilter}
 * never has to hit the database on every request - see
 * {@link dz.vecopharm.vecoassets.security.CustomUserDetailsService} for
 * where those authorities are computed.
 */
@Component
public class JwtService {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_AUTHORITIES = "authorities";

    /**
     * Valeur de secours de {@code application.yml} (jamais un vrai secret,
     * son nom le dit) - documentee en clair dans ce depot, donc quiconque
     * la lit peut forger un JWT avec les autorisations de son choix si elle
     * finit par signer des jetons en dehors d'un poste de developpement.
     */
    private static final String DEFAULT_SECRET = "change-me-in-env-never-commit-a-real-secret";

    private final SecretKey signingKey;
    private final long expirationMinutes;

    /**
     * Phase 10 (revue securite - gestion des secrets) : si {@code
     * app.jwt.secret} (variable d'environnement {@code JWT_SECRET}) est
     * encore la valeur de secours ci-dessus ET que les profils dev/demo ne
     * sont pas actifs, on refuse de demarrer plutot que de signer des
     * jetons de production avec un secret public. Les profils dev/demo (et
     * test, qui fournit toujours son propre secret - voir
     * application-test.yml) restent inchanges.
     */
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes,
            Environment environment
    ) {
        if (DEFAULT_SECRET.equals(secret) && !environment.matchesProfiles("dev", "demo")) {
            throw new IllegalStateException(
                    "app.jwt.secret (variable d'environnement JWT_SECRET) est toujours la valeur de "
                            + "developpement par defaut. Definissez un secret fort et unique avant de demarrer "
                            + "hors des profils dev/demo - cette valeur par defaut est documentee dans le depot, "
                            + "la conserver en dehors du developpement permettrait de forger n'importe quel jeton.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(User user, Collection<String> authorities) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_USER_ID, user.getId().toString())
                .claim(CLAIM_NAME, user.getFullName())
                .claim(CLAIM_AUTHORITIES, authorities)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }

    /** @throws io.jsonwebtoken.JwtException if the token is malformed, expired, or has an invalid signature. */
    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
    }
}
