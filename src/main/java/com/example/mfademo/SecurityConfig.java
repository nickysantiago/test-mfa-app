package main.java.com.example.mfademo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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

    // Builds and exposes the AuthenticationManager bean used for authentication.
    // Registers both the standard DAO provider and the custom MFA provider.
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       DaoAuthenticationProvider daoProvider,
                                                       MfaAuthenticationProvider mfaProvider) throws Exception {

        AuthenticationManagerBuilder amb = http.getSharedObject(AuthenticationManagerBuilder.class);
        
        // Add both authentication providers in the desired order
        amb.authenticationProvider(daoProvider);
        amb.authenticationProvider(mfaProvider);

        return amb.build();
    }

    // Defines the security filter chain for HTTP requests.
    // Configures routes, login/logout behavior, and integrates the MFA filter.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthenticationManager authManager,
                                           CustomAuthenticationSuccessHandler customSuccessHandler) throws Exception {

        // Create and configure the custom MFA authentication filter
        MfaAuthenticationFilter mfaFilter = new MfaAuthenticationFilter();
        mfaFilter.setAuthenticationManager(authManager);
        mfaFilter.setAuthenticationSuccessHandler(new MfaSuccessHandler());
        mfaFilter.setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/mfa?error"));

        http
          // Set the AuthenticationManager to be used across the filter chain
          .authenticationManager(authManager)
          .authorizeRequests()
            // Allow public access to login and home pages
            .antMatchers("/", "/login").permitAll()
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
          // Configure logout handling
          .logout()
            .logoutUrl("/logout")              // POST endpoint for logging out
            .logoutSuccessUrl("/login")        // Redirect to login after logout
            .invalidateHttpSession(true)       // Invalidate session on logout
            .deleteCookies("JSESSIONID")       // Clear session cookie
            .permitAll();

        // Add the custom MFA filter before the standard UsernamePasswordAuthenticationFilter
        http.addFilterBefore(mfaFilter, UsernamePasswordAuthenticationFilter.class);

        // Build and return the finalized security configuration
        return http.build();
    }
}
