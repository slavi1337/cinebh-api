package com.cinebh.api.services;

import com.cinebh.api.dto.auth.RegisterRequest;
import com.cinebh.api.dto.auth.VerifyRequest;

public interface AuthService {
    void register(RegisterRequest request);

    void verify(VerifyRequest request);
}
