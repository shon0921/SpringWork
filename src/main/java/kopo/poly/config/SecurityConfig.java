package kopo.poly.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/forgotpassword",
                                "/forgotpassword2",
                                "/forgotpassword3",
                                "/trackingResult",
                                "/guestdeliverytracking",
                                "/user/register1", // `UserController`의 회원가입 관련 경로들 허용
                                "/user/resendAuthCode",
                                "/user/register2",
                                "/user/login",
                                "/user/forgotpassword1",
                                "/user/forgotpassword2",
                                "/user/forgotpassword3",
                                "/api/auth/**",
                                "/css/**", // 모든 정적 리소스 허용
                                "/js/**",
                                "/img/**",
                                "/vendor/**",
                                "/html/**"
                        ).permitAll()
                        .requestMatchers("/main/**").hasRole("USER") // "/main" 경로만 "USER" 역할 가진 사용자에게 허용
                        .anyRequest().authenticated() // 나머지 모든 요청은 인증 필요
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}