package com.restaurant.model;

import com.restaurant.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Registered system user — can be a CUSTOMER, STAFF, or ADMIN.
 *
 * Implements Spring Security's UserDetails so it can be used directly by
 * the authentication framework. Authentication is username-based (not email).
 *
 * Guest orders do not require a User record; guests supply name/phone/email
 * directly on the order instead.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Full display name (e.g. "John Doe"). */
    @Column(nullable = false)
    private String name;

    /** Unique login handle; used as the JWT subject. */
    @Column(nullable = false, unique = true)
    private String username;

    /** Unique email address; stored but not used for login. */
    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt-hashed password. */
    @Column(nullable = false)
    private String password;

    /** Optional contact phone number. */
    private String phone;

    /** Role determines which endpoints the user may access. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Timestamp set automatically on first insert. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Monotonic counter embedded in every JWT this user receives.
     * Incremented on each successful login — older tokens become invalid
     * because their embedded version no longer matches the current one.
     * Enforces single-active-session-per-user and enables admin-initiated
     * force-logout by bumping this value server-side.
     */
    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private Long tokenVersion = 0L;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (tokenVersion == null) tokenVersion = 0L;
    }

    /**
     * Returns a single authority in the format "ROLE_<ROLE>" (e.g. ROLE_ADMIN).
     * Spring Security uses this for @PreAuthorize and hasRole() checks.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return username;
    }

    // All accounts are always active — no locking or expiry logic implemented.
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
