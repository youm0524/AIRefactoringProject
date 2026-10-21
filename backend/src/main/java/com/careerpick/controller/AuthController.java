package com.careerpick.controller;

import com.careerpick.dto.LoginRequest;
import com.careerpick.dto.LoginResponse;
import com.careerpick.dto.UserView;
import com.careerpick.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login") public LoginResponse login(@Valid @RequestBody LoginRequest request) { return authService.login(request); }
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me") public UserView me(@RequestHeader(name="Authorization", required=false) String authorization) { return authService.requireUser(authorization); }
}
