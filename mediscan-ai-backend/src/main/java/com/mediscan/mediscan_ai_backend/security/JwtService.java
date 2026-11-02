package com.mediscan.mediscan_ai_backend.security;

import com.mediscan.mediscan_ai_backend.entity.Utilisateur;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Génère et vérifie les tokens JWT (signature HMAC-SHA avec la clé secrète).
 * La clé vient d'app.jwt.secret (application-local.properties), encodée en Base64.
 */
@Service
public class JwtService {

    private final SecretKey cleSignature;

    @Getter
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secretBase64,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret est vide : renseignez-le dans application-local.properties (étape 2)");
        }
        byte[] octets = Decoders.BASE64.decode(secretBase64.trim());
        if (octets.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret est trop courte (" + octets.length + " octets) : 32 octets minimum, 64 recommandés");
        }
        this.cleSignature = Keys.hmacShaKeyFor(octets);
        this.expirationMs = expirationMs;
    }

    /** Crée un token contenant l'email (subject), l'id et le rôle. */
    public String genererToken(Utilisateur utilisateur) {
        Date maintenant = new Date();
        return Jwts.builder()
                .subject(utilisateur.getEmail())
                .claim("uid", utilisateur.getId())
                .claim("role", utilisateur.getRole().name())
                .claim("nom", utilisateur.getNomComplet())
                .issuedAt(maintenant)
                .expiration(new Date(maintenant.getTime() + expirationMs))
                .signWith(cleSignature)
                .compact();
    }

    /** Lit l'email contenu dans le token. Lᵉve une JwtException si le token est invalide ou expiré. */
    public String extraireEmail(String token) {
        return lireClaims(token).getSubject();
    }

    /** Vrai si la signature est correcte, le token non expiré, et qu'il appartient bien à cet utilisateur. */
    public boolean estValide(String token, UserDetails utilisateur) {
        try {
            Claims claims = lireClaims(token);
            return claims.getSubject().equalsIgnoreCase(utilisateur.getUsername())
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Vérifie la signature et l'expiration, puis renvoie le contenu du token. */
    private Claims lireClaims(String token) {
        return Jwts.parser()
                .verifyWith(cleSignature)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}