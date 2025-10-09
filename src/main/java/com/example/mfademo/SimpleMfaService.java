package main.java.com.example.mfademo;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Demo implementation:
 * - MFA enabled only for user "user1"
 * - Code is hardcoded to "1234"
 */
@Service
public class SimpleMfaService implements MfaService {

    @Override
    public boolean isMfaEnabled(String username) {
        return "user1".equals(username);
    }

    @Override
    public boolean verifyCode(UserDetails user, String code) {
        return "1234".equals(code);
    }
}
