package com.aquarius.security;

import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal di Aquarius: come UserDetails standard ma porta dietro
 *  - tenantId (la società scelta al login)
 *  - tenantDisplayName (per visualizzarla nella header)
 *  - esercizio (anno contabile selezionato — replica di PUB_ANNO di VFP)
 *  - mustResetPassword (true se l'utente ha loggato con la password legacy
 *    e va costretto a impostarne una nuova BCrypt)
 *
 * Equivalente Spring delle variabili PUBLIC PUB_CODOPE / PUB_CODSOC / PUB_ANNO
 * del vecchio Aquarius VFP.
 */
@Getter
@ToString
public class AquariusPrincipal implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final String passwordHash;          // mai esposto in toString
    private final String tenantId;
    private final String tenantDisplayName;
    private final String fullName;
    private final Integer esercizio;
    private final boolean mustResetPassword;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    public AquariusPrincipal(String username,
                             String passwordHash,
                             String tenantId,
                             String tenantDisplayName,
                             String fullName,
                             Integer esercizio,
                             boolean mustResetPassword,
                             boolean enabled,
                             List<String> roles) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.tenantId = tenantId;
        this.tenantDisplayName = tenantDisplayName;
        this.fullName = fullName;
        this.esercizio = esercizio;
        this.mustResetPassword = mustResetPassword;
        this.enabled = enabled;
        this.authorities = roles == null ? List.of()
                : roles.stream().map(SimpleGrantedAuthority::new).map(a -> (GrantedAuthority) a).toList();
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return passwordHash; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return !mustResetPassword; }
    @Override public boolean isEnabled()               { return enabled; }
}
