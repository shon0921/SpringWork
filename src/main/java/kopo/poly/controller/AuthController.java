package kopo.poly.controller;

import kopo.poly.dto.UserDTO;
import kopo.poly.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserDTO user) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUserId(), user.getPassword())
        );

        final String token = jwtUtil.generateToken(user.getUserId());
        return ResponseEntity.ok(token);
    }
}