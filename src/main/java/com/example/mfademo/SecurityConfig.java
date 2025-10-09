package main.java.com.example.mfademo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder; // demo only
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    // demo user
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
            User.withUsername("user1").password("password").roles("USER").build()
        );
    }

    // demo encoder (plaintext) - DO NOT use in production
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    // expose MfaAuthenticationProvider as a bean so we can register it in the manager
    @Bean
    public MfaAuthenticationProvider mfaAuthenticationProvider(UserDetailsService uds, MfaService mfaService) {
        return new MfaAuthenticationProvider(uds, mfaService);
    }

    // Build an AuthenticationManager that contains both the Dao provider and the MFA provider
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       UserDetailsService uds,
                                                       PasswordEncoder passwordEncoder,
                                                       MfaAuthenticationProvider mfaProvider) throws Exception {

        AuthenticationManagerBuilder amb = http.getSharedObject(AuthenticationManagerBuilder.class);

        // Option A: register Dao via userDetailsService + passwordEncoder (recommended)
        amb.userDetailsService(uds).passwordEncoder(passwordEncoder);

        // Option B: explicit DaoAuthenticationProvider (equivalent)
        // DaoAuthenticationProvider dao = new DaoAuthenticationProvider();
        // dao.setUserDetailsService(uds);
        // dao.setPasswordEncoder(passwordEncoder);
        // amb.authenticationProvider(dao);

        // register MFA provider that handles MfaAuthenticationToken
        amb.authenticationProvider(mfaProvider);

        return amb.build();
    }

    // Configure HttpSecurity and add the MFA filter (constructed here so we can set its authManager)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthenticationManager authManager,
                                           CustomAuthenticationSuccessHandler customSuccessHandler) throws Exception {

        // create MFA filter and give it the authentication manager
        MfaAuthenticationFilter mfaFilter = new MfaAuthenticationFilter();
        mfaFilter.setAuthenticationManager(authManager);
        mfaFilter.setAuthenticationSuccessHandler(new MfaSuccessHandler());
        mfaFilter.setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/mfa?error"));

        http
          .authorizeRequests()
            .antMatchers("/", "/login", "/mfa", "/mfa/verify").permitAll()
            .anyRequest().authenticated()
            .and()
          .formLogin()
            .successHandler(customSuccessHandler)
            .permitAll()
            .and()
          .logout().permitAll();

        // ensure filter is registered before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(mfaFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
