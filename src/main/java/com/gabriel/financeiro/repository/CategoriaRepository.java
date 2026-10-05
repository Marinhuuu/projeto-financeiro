package com.gabriel.financeiro.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gabriel.financeiro.entities.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    List<Categoria> findByTipoIgnoreCaseOrderByNomeAsc(String tipo);

}
