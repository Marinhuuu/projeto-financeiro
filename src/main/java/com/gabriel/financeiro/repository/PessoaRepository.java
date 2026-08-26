package com.gabriel.financeiro.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gabriel.financeiro.entities.Pessoa;

public interface PessoaRepository extends JpaRepository<Pessoa, UUID> {

}
