package com.banquemisr.recruitment.config;

import com.banquemisr.recruitment.Authentication.Security.JwtAuthenticationFilter;
import com.banquemisr.recruitment.Authentication.Security.Property.LdapProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.List;

/**
 * Sole security configuration class for the application. Authentication is delegated
 * entirely to LDAP (bind authentication) - there is no local UserDetailsService / DB
 * password check. Because ldapAuthenticationProvider is the only AuthenticationProvider
 * bean in the context, Spring Security automatically uses it to build the shared
 * AuthenticationManager bean below.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(LdapProperties.class)
@AllArgsConstructor
public class SecurityConfig {

    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:3000",
            "http://localhost:4200"
    );

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LdapProperties ldapProperties;

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(ALLOWED_ORIGINS);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public BaseLdapPathContextSource contextSource() {

        LdapContextSource contextSource = new LdapContextSource();

        contextSource.setUrl(ldapProperties.getUrls());
        contextSource.setBase(ldapProperties.getBase());
        contextSource.setUserDn(ldapProperties.getUsername());
        contextSource.setPassword(ldapProperties.getPassword());

        contextSource.afterPropertiesSet();

        return contextSource;
    }

    @Bean
    public AuthenticationProvider ldapAuthenticationProvider(
            BaseLdapPathContextSource contextSource) {

        BindAuthenticator authenticator = new BindAuthenticator(contextSource);

        authenticator.setUserSearch(
                new FilterBasedLdapUserSearch(
                        ldapProperties.getPeopleOu(),
                        ldapProperties.getUserSearchFilter(),
                        contextSource
                )
        );

        // Roles come from LDAP group membership under ou=groups. Requires that OU to
        // actually exist with group entries whose "member" attribute lists user DNs -
        // this is separate directory setup from the ou=people entries LdapUserService writes.
        DefaultLdapAuthoritiesPopulator authorities =
                new DefaultLdapAuthoritiesPopulator(contextSource, "ou=groups");
        authorities.setGroupSearchFilter("(member={0})");
        authorities.setRolePrefix("");
        authorities.setSearchSubtree(true);
        authorities.setConvertToUpperCase(true);

        return new LdapAuthenticationProvider(authenticator, authorities);
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(String.format(
                    "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}",
                    Instant.now(),
                    authException.getMessage() != null ? authException.getMessage() : "Full authentication is required to access this resource"
            ));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(String.format(
                    "{\"timestamp\":\"%s\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"%s\"}",
                    Instant.now(),
                    "Access denied: You do not have permission to access this resource"
            ));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider ldapAuthenticationProvider,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(ldapAuthenticationProvider)
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}