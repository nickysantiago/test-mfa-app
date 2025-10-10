package main.java.com.example.mfademo;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final MfaService mfaService;
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    public CustomAuthenticationSuccessHandler(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth) throws IOException {
        UserDetails user = (UserDetails) auth.getPrincipal();

        if (mfaService.isMfaEnabled(user.getUsername())) {
            // Store username for MFA verification step
            req.getSession().setAttribute("PRE_AUTH_USERNAME", user.getUsername());
            
            // CRITICAL: Clear the authentication from SecurityContext
            // This prevents the user from being fully authenticated until MFA is complete
            SecurityContextHolder.clearContext();
            
            // Also clear it from the session explicitly
            req.getSession().removeAttribute("SPRING_SECURITY_CONTEXT");
            
            // Redirect to MFA page
            res.sendRedirect("/mfa");
            return;
        }

        // No MFA required - proceed to original destination
        SavedRequest saved = requestCache.getRequest(req, res);
        String target = (saved != null) ? saved.getRedirectUrl() : "/";
        res.sendRedirect(target);
    }
}