package com.careerpick.auth;

public record LoginResponse(String token, UserView user) {}
