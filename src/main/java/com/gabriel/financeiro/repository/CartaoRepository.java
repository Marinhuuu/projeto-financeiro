package com.gabriel.financeiro.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gabriel.financeiro.entities.CartaoCredito;

public interface CartaoRepository extends JpaRepository<CartaoCredito, UUID>{

}
