package kopo.poly.controller;

import kopo.poly.dto.UserDTO;
import kopo.poly.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserDTO user) {
        log.info("로그인 시도: {}", user.getUserId());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUserId(), user.getPassword())
            );
        } catch (Exception e) {
            log.warn("로그인 실패: {}", e.getMessage());
            return ResponseEntity.status(401).body("로그인 실패: 사용자 이름 또는 비밀번호가 올바르지 않습니다.");
        }

        final String token = jwtUtil.generateToken(user.getUserId());
        log.info("로그인 성공, 토큰 발급: {}", token);
        return ResponseEntity.ok(token);
    }
}