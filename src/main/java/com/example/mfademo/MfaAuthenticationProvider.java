package main.java.com.example.mfademo;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * AuthenticationProvider that handles MfaAuthenticationToken (verifies the MFA code).
 */
public class MfaAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final MfaService mfaService;

    public MfaAuthenticationProvider(UserDetailsService userDetailsService, MfaService mfaService) {
        this.userDetailsService = userDetailsService;
        this.mfaService = mfaService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof MfaAuthenticationToken)) {
            return null;
        }

        Object principalObj = authentication.getPrincipal();
        String username;
        UserDetails user;

        // principal might be String (pre-auth) or already a UserDetails (defensive)
        if (principalObj instanceof String) {
            username = (String) principalObj;
            user = userDetailsService.loadUserByUsername(username);
        } else if (principalObj instanceof UserDetails) {
            user = (UserDetails) principalObj;
            username = user.getUsername();
        } else {
            throw new BadCredentialsException("Unsupported principal type: " + principalObj);
        }

        // credentials are expected to be the submitted MFA code (String)
        Object credentials = authentication.getCredentials();
        String code = (credentials == null) ? null : credentials.toString();

        if (!mfaService.verifyCode(user, code)) {
            throw new BadCredentialsException("Invalid MFA code");
        }

        // Build an authenticated token carrying the real UserDetails and authorities
        MfaAuthenticationToken authenticated =
                new MfaAuthenticationToken(user, user.getAuthorities());
        authenticated.setDetails(authentication.getDetails());
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return MfaAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
