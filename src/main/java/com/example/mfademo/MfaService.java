package main.java.com.example.mfademo;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Simple contract for demo MFA service.
 */
public interface MfaService {
    boolean isMfaEnabled(String username);
    boolean verifyCode(UserDetails user, String code);
}

