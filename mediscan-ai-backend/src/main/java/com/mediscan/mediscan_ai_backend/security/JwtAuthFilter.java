package com.mediscan.mediscan_ai_backend.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Exécuté une fois par requête :
 * 1. lit l'en-tête "Authorization : Bearer <token>"
 * 2. vérifie le token
 * 3. si valide, déclare l'utilisateur comme authentifié pour cette requête.
 * Si le token est absent ou invalide, la requête continue SANS authentification :
 * Spring Security renverra alors 401 sur les routes protégées.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String entete = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (entete == null || !entete.startsWith(PREFIXE)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = entete.substring(PREFIXE.length()).trim();

        try {
            String email = jwtService.extraireEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails utilisateur = userDetailsService.loadUserByUsername(email);

                if (utilisateur.isEnabled() && jwtService.estValide(token, utilisateur)) {
                    UsernamePasswordAuthenticationToken authentification =
                            new UsernamePasswordAuthenticationToken(
                                    utilisateur, null, utilisateur.getAuthorities());
                    authentification.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContext contexte = SecurityContextHolder.createEmptyContext();
                    contexte.setAuthentication(authentification);
                    SecurityContextHolder.setContext(contexte);
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            // Token expiré, modifié ou utilisateur supprimé : on ignore, la requête reste anonyme
            log.debug("Token JWT refusé sur {} : {}", request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}