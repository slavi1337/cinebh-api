package com.cinebh.api.services;

import com.cinebh.api.dto.auth.LoginRequest;
import com.cinebh.api.dto.auth.LoginResponse;
import com.cinebh.api.dto.auth.RegisterRequest;
import com.cinebh.api.dto.auth.VerifyRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    void register(RegisterRequest request);

    void verify(VerifyRequest request);

    LoginResponse login(LoginRequest request, HttpServletResponse response);

    LoginResponse getCurrentUser();

    void refresh(HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);
}
