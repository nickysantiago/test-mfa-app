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

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
            User.withUsername("user1").password("password").roles("USER").build()
        );
    }

    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public MfaAuthenticationProvider mfaAuthenticationProvider(UserDetailsService uds, MfaService mfaService) {
        return new MfaAuthenticationProvider(uds, mfaService);
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService uds, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       DaoAuthenticationProvider daoProvider,
                                                       MfaAuthenticationProvider mfaProvider) throws Exception {

        AuthenticationManagerBuilder amb = http.getSharedObject(AuthenticationManagerBuilder.class);
        
        amb.authenticationProvider(daoProvider);
        amb.authenticationProvider(mfaProvider);

        return amb.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthenticationManager authManager,
                                           CustomAuthenticationSuccessHandler customSuccessHandler) throws Exception {

        MfaAuthenticationFilter mfaFilter = new MfaAuthenticationFilter();
        mfaFilter.setAuthenticationManager(authManager);
        mfaFilter.setAuthenticationSuccessHandler(new MfaSuccessHandler());
        mfaFilter.setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/mfa?error"));

        http
          .authenticationManager(authManager)
          .authorizeRequests()
            .antMatchers("/", "/login", "/mfa", "/mfa/verify").permitAll()
            .anyRequest().authenticated()
            .and()
          .formLogin()
            .successHandler(customSuccessHandler)
            .permitAll()
            .and()
          .logout()
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll();

        http.addFilterBefore(mfaFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}