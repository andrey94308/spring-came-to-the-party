package org.mato.command.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;
    private final Clock clock;

    public JwtService(@Value("${auth.jwt-secret}") String secret,
                      @Value("${auth.jwt-ttl-seconds}") long ttlSeconds,
                      Clock clock) {
        // jjwt requires HS256 keys to be at least 256 bits. Pad if needed.
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(raw, 0, padded, 0, raw.length);
            raw = padded;
        }
        this.key = Keys.hmacShaKeyFor(raw);
        this.ttlSeconds = ttlSeconds;
        this.clock = clock;
    }

    public String issue(String username) {
        Date now = Date.from(clock.instant());
        Date exp = Date.from(clock.instant().plusSeconds(ttlSeconds));
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public String parseUsername(String compact) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(compact).getPayload().getSubject();
    }
}
