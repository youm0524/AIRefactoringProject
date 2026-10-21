package com.careerpick.auth;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login") public LoginResponse login(@Valid @RequestBody LoginRequest request) { return authService.login(request); }
    @GetMapping("/me") public UserView me(@RequestHeader(name="Authorization", required=false) String authorization) { return authService.requireUser(authorization); }
}
