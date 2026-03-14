package main.java.com.example.mfademo;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class KeycloakLogoutHandler implements LogoutSuccessHandler {

    // Keycloak's end_session endpoint — ends the SSO session on the Keycloak side
    private static final String KEYCLOAK_LOGOUT_URL =
        "http://192.168.0.101:8777/realms/test-mfa-app/protocol/openid-connect/logout";

    // Where Keycloak should send the user after it ends the session
    private static final String POST_LOGOUT_REDIRECT_URI =
        "http://192.168.0.101:8013/login";

    @Override
    public void onLogoutSuccess(HttpServletRequest req, HttpServletResponse res,
                                Authentication auth) throws IOException {

        if (auth != null && auth.getPrincipal() instanceof OidcUser oidcUser) {
            // For SSO users, redirect to Keycloak's logout endpoint
            // id_token_hint tells Keycloak which session to end
            String idToken = oidcUser.getIdToken().getTokenValue();

            String keycloakLogout = KEYCLOAK_LOGOUT_URL
                + "?id_token_hint=" + idToken
                + "&post_logout_redirect_uri=" + POST_LOGOUT_REDIRECT_URI;

            res.sendRedirect(keycloakLogout);
        } else {
            // For form login users, just redirect to login as before
            res.sendRedirect(POST_LOGOUT_REDIRECT_URI);
        }
    }
}
