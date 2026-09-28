package vn.iotstar.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

import java.text.ParseException;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString("a-secure-test-key-with-at-least-32-bytes".getBytes());
        jwtService = new JwtService(secret, 3_600_000);
        user = new User("phuc@qycandle.vn", "encoded", List.of());
    }

    @Test
    void nimbusGeneratesHs256JwtWithRequiredClaims() throws ParseException {
        String token = jwtService.generateToken(user);
        SignedJWT parsed = SignedJWT.parse(token);
        assertThat(token.split("\\.")).hasSize(3);
        assertThat(parsed.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);
        assertThat(parsed.getHeader().getType().toString()).isEqualTo("JWT");
        assertThat(parsed.getJWTClaimsSet().getSubject()).isEqualTo("phuc@qycandle.vn");
        assertThat(parsed.getJWTClaimsSet().getIssueTime()).isNotNull();
        assertThat(parsed.getJWTClaimsSet().getExpirationTime()).isNotNull();
        assertThat(jwtService.extractUsername(token)).isEqualTo("phuc@qycandle.vn");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void rejectsModifiedAndExpiredTokens() {
        String token = jwtService.generateToken(user);
        char last = token.charAt(token.length() - 1);
        String modified = token.substring(0, token.length() - 1) + (last == 'a' ? 'b' : 'a');
        assertThat(jwtService.isTokenValid(modified, user)).isFalse();

        String secret = Base64.getEncoder().encodeToString("a-secure-test-key-with-at-least-32-bytes".getBytes());
        JwtService expiredService = new JwtService(secret, -1_000);
        assertThat(expiredService.isTokenValid(expiredService.generateToken(user), user)).isFalse();
    }
}
