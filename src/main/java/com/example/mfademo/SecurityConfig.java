package main.java.com.example.mfademo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.util.List;

@Configuration
public class SecurityConfig {

    // Defines an in-memory user details service for testing/demo purposes.
    // Creates a single user with username 'user1' and password 'password'.
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
            User.withUsername("user1").password("password").roles("USER").build()
        );
    }

    // Defines the password encoder to use.
    // NoOpPasswordEncoder performs no encoding (plaintext passwords) — not for production use.
    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    // Custom authentication provider for handling MFA logic.
    // It uses the UserDetailsService for user lookup and MfaService for verifying MFA codes.
    @Bean
    public MfaAuthenticationProvider mfaAuthenticationProvider(UserDetailsService uds, MfaService mfaService) {
        return new MfaAuthenticationProvider(uds, mfaService);
    }

    // Standard DAO-based authentication provider (username/password authentication).
    // This is the default provider type used by Spring Security.
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService uds, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    // Defines the security filter chain for HTTP requests.
    // Configures routes, login/logout behavior, and integrates the MFA filter.
    // Note: AuthenticationManager is built locally so it does not replace Spring's
    // default one, which is required for OAuth2/SSO login to work correctly.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           DaoAuthenticationProvider daoProvider,
                                           MfaAuthenticationProvider mfaProvider,
                                           CustomAuthenticationSuccessHandler customSuccessHandler,
                                           KeycloakLogoutHandler keycloakLogoutHandler) throws Exception {

        // Build a local AuthenticationManager scoped to form login and MFA only.
        // This avoids overriding Spring's global manager, which handles OAuth2 login.
        AuthenticationManager localAuthManager = new ProviderManager(
            List.of(daoProvider, mfaProvider)
        );

        // Create and configure the custom MFA authentication filter
        MfaAuthenticationFilter mfaFilter = new MfaAuthenticationFilter();
        mfaFilter.setAuthenticationManager(localAuthManager);
        mfaFilter.setAuthenticationSuccessHandler(new MfaSuccessHandler());
        mfaFilter.setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/mfa?error"));

        http
          //.authenticationManager(localAuthManager)
          .authorizeRequests()
            // Allow public access to login page
            .antMatchers("/", "/login", "/bamboohr/connect", "/bamboohr/callback").permitAll()
            // Allow access to MFA verification endpoints
            .antMatchers("/mfa", "/mfa/verify").permitAll()
            // Require authentication for all other requests
            .anyRequest().authenticated()
            .and()
          // Configure form-based login
          .formLogin()
            // Custom success handler redirects user based on MFA status
            .successHandler(customSuccessHandler)
            .permitAll()
            .and()
          // Configure SSO login via Keycloak (OAuth2/OIDC)
          .oauth2Login()
            .defaultSuccessUrl("/home", true)
            .and()
          // Configure logout handling
          .logout()
            .logoutUrl("/logout")
            // .logoutSuccessUrl("/login") 
            .logoutSuccessHandler(keycloakLogoutHandler)
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll();

        // Add the custom MFA filter before the standard UsernamePasswordAuthenticationFilter
        http.addFilterBefore(mfaFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
