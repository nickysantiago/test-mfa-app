package main.java.com.example.mfademo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class Auth0LogoutHandler implements LogoutSuccessHandler {

    @Value("${app.auth0.logout-url}")
    private String auth0LogoutUrl;

    @Value("${app.auth0.client-id}")
    private String clientId;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Override
    public void onLogoutSuccess(HttpServletRequest req, HttpServletResponse res,
                                Authentication auth) throws IOException {

        if (auth != null && auth.getPrincipal() instanceof OidcUser) {
            // Auth0 logout requires client_id and returnTo
            String logoutUrl = auth0LogoutUrl
                + "?client_id=" + clientId
                + "&returnTo=" + appBaseUrl + "/login";

            res.sendRedirect(logoutUrl);
        } else {
            // Form login users — redirect straight to login
            res.sendRedirect(appBaseUrl + "/login");
        }
    }
}