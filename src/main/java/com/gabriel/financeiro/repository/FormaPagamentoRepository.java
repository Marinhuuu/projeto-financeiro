package com.gabriel.financeiro.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gabriel.financeiro.entities.FormaPagamento;

@Repository
public interface FormaPagamentoRepository extends JpaRepository<FormaPagamento, UUID> {

	Optional<FormaPagamento> findByCodigo(String codigo);

	Optional<FormaPagamento> findByNomeIgnoreCase(String nome);
}
