package com.upiiz.platform_api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(10); }

    @Bean public AuthenticationManager authenticationManager(UserDetailsService uds, PasswordEncoder enc){
        var p=new DaoAuthenticationProvider(); p.setUserDetailsService(uds); p.setPasswordEncoder(enc);
        return new ProviderManager(p);
    }

    @Bean public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter f) throws Exception {
        http.csrf(csrf->csrf.disable()).cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth->auth
                        .requestMatchers("/upiiz/public/v1/auth/**","/swagger-ui/**","/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(f, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean public CorsConfigurationSource corsConfigurationSource(){
        var cfg=new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:3030", "http://localhost:3021", "http://localhost:4200"));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","X-App-BaseUrl"));
        cfg.setAllowCredentials(true);
        var src=new UrlBasedCorsConfigurationSource(); src.registerCorsConfiguration("/**",cfg); return src;
    }
}
