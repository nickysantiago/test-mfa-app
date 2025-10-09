package main.java.com.example.mfademo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/")
    public String hello() {
        return "Hello, world! Spring Boot + Spring Security are wired.";
    }

    // Protected page for testing — triggers login -> MFA
    @GetMapping("/secure")
    public String secure() {
        return "You made it! This is a protected page (MFA completed).";
    }
}
