package main.java.com.example.mfademo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import java.security.Principal;

import org.springframework.stereotype.Controller;
// import org.springframework.web.bind.annotation.RestController;

@Controller
public class MainController {
    @GetMapping("/login")
    public String login() {
        return "login";
    }

   /*  @GetMapping("/home")
    public String home() {
        return "home";
    } */

    // Display success page after full authentication
    @GetMapping("/home")
    public String home(Model model, Principal principal) {
        model.addAttribute("username", principal.getName());
        return "home"; // returns home.html
    }

    @GetMapping("/mfa")
    public String mfaPage() {
        return "mfa";
    }

    // Root just redirects to /home
    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }
}
