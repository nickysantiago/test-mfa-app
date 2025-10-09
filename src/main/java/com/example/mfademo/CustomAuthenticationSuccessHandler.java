package main.java.com.example.mfademo;

import org.springframework.security.core.Authentication;
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
            MfaAuthenticationToken pre = new MfaAuthenticationToken(user.getUsername());
            pre.setDetails(auth.getDetails());
            req.getSession().setAttribute("PRE_AUTH_USERNAME", user.getUsername());
            req.getSession().setAttribute("SPRING_SECURITY_CONTEXT", null); // clear full auth
            res.sendRedirect("/mfa");
            return;
        }

        SavedRequest saved = requestCache.getRequest(req, res);
        String target = (saved != null) ? saved.getRedirectUrl() : "/";
        res.sendRedirect(target);
    }
}
