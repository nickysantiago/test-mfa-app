package main.java.com.example.mfademo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // Note: using the SecurityFilterChain bean approach works in 2.7.x (Spring Security 5.7+),
    // but we use the classic authorizeRequests() / antMatchers() style that is common with 2.7.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
          .authorizeRequests()
            .antMatchers("/").permitAll()      // allow root for initial testing
            .anyRequest().authenticated()
            .and()
          .formLogin()                         // default login page for quick testing
            .permitAll()
            .and()
          .logout()
            .permitAll();

        return http.build();
    }
}


/* SPRINGBOOT 3.5.X CONFIGURATION #################
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
          // allow public access to root for initial testing:
          .authorizeHttpRequests(auth -> auth
              .requestMatchers("/").permitAll()
              .anyRequest().authenticated()
          )
          // enable the default form login for other endpoints (you can test it later)
          .formLogin(Customizer.withDefaults())
          // keep defaults for CSRF, session management, etc.
          ;
        return http.build();
    }
} */
