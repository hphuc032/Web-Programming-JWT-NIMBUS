package vn.iotstar.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import vn.iotstar.entity.User;
import vn.iotstar.exception.InvalidJwtException;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {
    private final byte[] secretBytes;
    private final long expirationTime;

    public JwtService(@Value("${security.jwt.secret-key}") String secret,
                      @Value("${security.jwt.expiration-time}") long expirationTime) {
        this.secretBytes = decodeSecret(secret);
        this.expirationTime = expirationTime;
    }

    private byte[] decodeSecret(String secret) {
        if (secret == null || secret.isBlank()) throw new IllegalArgumentException("JWT_SECRET chưa được cấu hình");
        byte[] decoded;
        try { decoded = Base64.getDecoder().decode(secret); }
        catch (IllegalArgumentException ex) { decoded = secret.getBytes(StandardCharsets.UTF_8); }
        if (decoded.length < 32) throw new IllegalArgumentException("JWT secret cho HS256 phải dài ít nhất 32 bytes");
        return decoded;
    }

    public String generateToken(UserDetails userDetails) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expirationTime);
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(userDetails.getUsername()).issueTime(issuedAt).expirationTime(expiresAt)
                .claim("authorities", userDetails.getAuthorities().stream().map(Object::toString).toList());
        if (userDetails instanceof User user) {
            claims.claim("userId", user.getId()).claim("fullName", user.getFullName());
        }
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(), claims.build());
        try {
            signedJWT.sign(new MACSigner(secretBytes));
            return signedJWT.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Không thể ký JWT bằng Nimbus", ex);
        }
    }

    public String extractUsername(String token) {
        try { return SignedJWT.parse(token).getJWTClaimsSet().getSubject(); }
        catch (ParseException ex) { throw new InvalidJwtException("JWT sai định dạng", ex); }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) return false;
            if (!jwt.verify(new MACVerifier(secretBytes))) return false;
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            return expiration != null && expiration.after(new Date())
                    && userDetails.getUsername().equals(claims.getSubject());
        } catch (ParseException | JOSEException | RuntimeException ex) { return false; }
    }

    public long getExpirationTime() { return expirationTime; }
}
