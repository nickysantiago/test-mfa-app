package main.java.com.example.mfademo;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;

// @Component
public class KeycloakLogoutHandler implements LogoutSuccessHandler {

    @Value("${app.keycloak.logout-url}")
    private String keycloakLogoutUrl;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Override
    public void onLogoutSuccess(HttpServletRequest req, HttpServletResponse res,
                                Authentication auth) throws IOException {

        if (auth != null && auth.getPrincipal() instanceof OidcUser oidcUser) {
            String idToken = oidcUser.getIdToken().getTokenValue();
            String postLogoutUri = appBaseUrl + "/login";

            String logoutUrl = keycloakLogoutUrl
                + "?id_token_hint=" + idToken
                + "&post_logout_redirect_uri=" + postLogoutUri;

            res.sendRedirect(logoutUrl);
        } else {
            res.sendRedirect(appBaseUrl + "/login");
        }
    }
}
