package main.java.com.example.mfademo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import java.security.Principal;

import org.springframework.stereotype.Controller;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Controller
public class MainController {
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Display success page after full authentication
    @GetMapping("/home")
    public String home(Model model, Authentication authentication) {
      if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
        // SSO login via Keycloak
        model.addAttribute("username", oidcUser.getPreferredUsername());
        model.addAttribute("loginMethod", "SSO (Keycloak)");
      } else {
        // Traditional form login
        model.addAttribute("username", authentication.getName());
        model.addAttribute("loginMethod", "Form Login + MFA");
      }
      return "home";
    }  

    @GetMapping("/mfa")
    public String mfaPage(HttpServletRequest req, HttpServletResponse res) throws IOException {
        // Check if user has completed username/password authentication
        String preAuthUsername = (String) req.getSession().getAttribute("PRE_AUTH_USERNAME");
        
        if (preAuthUsername == null) {
            // No pre-auth session - redirect to login
            res.sendRedirect("/login");
            return null;
        }
        
        return "mfa";
    }

    // Root just redirects to /home
    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }
}
