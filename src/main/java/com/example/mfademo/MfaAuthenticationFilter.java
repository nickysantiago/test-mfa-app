package main.java.com.example.mfademo;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MfaAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public MfaAuthenticationFilter() {
        super("/mfa/verify"); // handles POST /mfa/verify
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res)
            throws AuthenticationException {

        String username = (String) req.getSession().getAttribute("PRE_AUTH_USERNAME");
        String code = req.getParameter("code");

        if (username == null) {
            throw new AuthenticationServiceException("No username in session");
        }

        MfaAuthenticationToken token = new MfaAuthenticationToken(username);
        token.setCredentials(code);

        return this.getAuthenticationManager().authenticate(token);
    }
}
