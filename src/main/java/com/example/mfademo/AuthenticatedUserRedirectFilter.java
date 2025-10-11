package main.java.com.example.mfademo;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter that redirects fully authenticated users away from login/mfa pages.
 * If an authenticated user tries to access /login or /mfa, they are redirected to /home.
 */
@Component
public class AuthenticatedUserRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Check if user is fully authenticated
        if (auth != null && auth.isAuthenticated() && !isAnonymous(auth)) {
            String requestURI = request.getRequestURI();
            
            // Redirect authenticated users away from login and mfa pages
            if (requestURI.equals("/login") || requestURI.equals("/mfa/verify")) {
                response.sendRedirect("/home");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Check if the authentication is anonymous (not truly authenticated).
     */
    private boolean isAnonymous(Authentication auth) {
        return auth.getPrincipal().equals("anonymousUser");
    }
}