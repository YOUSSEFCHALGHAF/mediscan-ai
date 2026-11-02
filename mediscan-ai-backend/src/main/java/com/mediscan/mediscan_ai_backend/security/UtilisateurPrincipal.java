package com.mediscan.mediscan_ai_backend.security;


import com.mediscan.mediscan_ai_backend.entity.Utilisateur;
import com.mediscan.mediscan_ai_backend.entity.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Représente l'utilisateur authentifié.
 * Les contrôleurs le récupèrent avec @AuthenticationPrincipal pour connaître son id et son rôle.
 */
@Getter
public class UtilisateurPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String motDePasseHash;
    private final Role role;
    private final boolean actif;
    private final String nomComplet;

    public UtilisateurPrincipal(Utilisateur u) {
        this.id = u.getId();
        this.email = u.getEmail();
        this.motDePasseHash = u.getMotDePasseHash();
        this.role = u.getRole();
        this.actif = u.isActif();
        this.nomComplet = u.getNomComplet();
    }

    /** Spring Security attend le préfixe "ROLE_" pour hasRole("PATIENT"). */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return motDePasseHash;
    }

    /** L'identifiant de connexion est l'email. */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return actif;
    }
}