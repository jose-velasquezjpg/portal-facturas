package pe.edu.utp.portalfacturas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/login", "/recuperar-clave").permitAll()
                .requestMatchers("/facturas/nueva", "/facturas/guardar").hasAnyRole("ADMIN", "COMPRAS", "CONTABILIDAD", "PROVEEDOR")
                .requestMatchers("/facturas/**").hasAnyRole("ADMIN", "COMPRAS", "CONTABILIDAD", "TESORERIA", "PROVEEDOR")
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/facturas", true).permitAll())
            .rememberMe(remember -> remember.tokenValiditySeconds(86400).alwaysRemember(false))
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
            .exceptionHandling(ex -> ex.accessDeniedPage("/error/403"));
        // CSRF permanece habilitado; no se desactiva para facilitar formularios.
        return http.build();
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
