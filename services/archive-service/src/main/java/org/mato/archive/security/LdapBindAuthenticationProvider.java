package org.mato.archive.security;

import org.mato.archive.user.UserRepository;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

/** Calls the affected Spring LDAP bind API, including for an empty password. */
@Component
public class LdapBindAuthenticationProvider implements AuthenticationProvider {
    private final LdapContextSource ldap;
    private final UserRepository users;
    private final String base;

    public LdapBindAuthenticationProvider(LdapContextSource ldap, UserRepository users,
                                          @Value("${ldap.base}") String base) {
        this.ldap = ldap;
        this.users = users;
        this.base = base;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName();
        if (!username.matches("[a-zA-Z0-9.]{1,64}")) {
            throw new BadCredentialsException("Invalid credentials");
        }
        String password = String.valueOf(authentication.getCredentials());
        String dn = "uid=" + username + ",ou=people," + base;
        try {
            var context = ldap.getContext(dn, password);
            context.close();
        } catch (org.springframework.ldap.NamingException | javax.naming.NamingException e) {
            throw new BadCredentialsException("Invalid credentials", e);
        }
        var profile = users.findById(username)
                .orElseThrow(() -> new BadCredentialsException("Unknown profile"));
        String role = profile.getRole();
        if (!(role.equals("admin") || role.equals("user"))
                || (role.equals("user") && (profile.getDepartment() == null
                        || profile.getDepartment().isBlank()))) {
            throw new BadCredentialsException("Invalid profile");
        }
        var principal = User.withUsername(username).password("")
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())).build();
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> type) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type);
    }
}
