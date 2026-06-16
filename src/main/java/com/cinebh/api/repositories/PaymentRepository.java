package com.cinebh.api.repositories;

import com.cinebh.api.entities.Payment;
import com.cinebh.api.repositories.custom.PaymentQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID>, PaymentQueryRepository {
}
