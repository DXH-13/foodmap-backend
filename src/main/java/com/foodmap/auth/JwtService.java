package com.foodmap.auth;

import com.foodmap.config.props.FoodmapProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Phát hành và kiểm tra access token.
 *
 * <p>Refresh token <b>không</b> nằm ở đây — nó là chuỗi ngẫu nhiên có trạng thái,
 * lưu hash trong CSDL để thu hồi được (FR-AUTH-04). JWT không thu hồi được nên
 * chỉ dùng cho access token, TTL ngắn.
 */
@Service
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMAIL_VERIFIED = "emailVerified";

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtService(FoodmapProperties properties) {
        FoodmapProperties.Jwt jwt = properties.jwt();
        this.key = Keys.hmacShaKeyFor(jwt.secret().getBytes(StandardCharsets.UTF_8));
        this.issuer = jwt.issuer();
        this.accessTokenTtl = Duration.ofMinutes(jwt.accessTokenTtlMinutes());
    }

    public String issueAccessToken(UUID userId, String role, boolean emailVerified) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_EMAIL_VERIFIED, emailVerified)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    /**
     * @return danh tính trong token, hoặc rỗng nếu token sai chữ ký, hết hạn hoặc dị dạng.
     *         Không ném exception — người gọi tự quyết định coi đó là 401 hay là khách vãng lai.
     */
    public Optional<AuthPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(new AuthPrincipal(
                    UUID.fromString(claims.getSubject()),
                    claims.get(CLAIM_ROLE, String.class),
                    Boolean.TRUE.equals(claims.get(CLAIM_EMAIL_VERIFIED, Boolean.class))));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
