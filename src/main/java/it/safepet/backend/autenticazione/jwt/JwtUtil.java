package it.safepet.backend.autenticazione.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {
    private static final String SECRET = "thisIsASecretKeyThatIsLongEnoughForHmacSha256";
    private static final long EXPIRATION_MS = 3600_000; // 1h

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

    // Generazione token con informazioni extra
    public String generateToken(Long id, String email, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .addClaims(Map.of(
                        "id", id,
                        "email", email,
                        "role", role.name()
                ))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Estrae i claims
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Recupera dati singoli
    public Long getId(String token) {
        return extractAllClaims(token).get("id", Long.class);
    }

    public String getEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    public Role getRole(String token) {
        String roleStr = extractAllClaims(token).get("role", String.class);
        return Role.valueOf(roleStr); // converte stringa in enum
    }

    public boolean isExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
