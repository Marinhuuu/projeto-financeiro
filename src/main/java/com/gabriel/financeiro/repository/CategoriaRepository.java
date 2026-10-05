package com.gabriel.financeiro.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.gabriel.financeiro.entities.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    List<Categoria> findByTipoIgnoreCaseOrderByNomeAsc(String tipo);

    Page<Categoria> findByTipoIgnoreCase(String tipo, Pageable pageable);

    long countByTipoIgnoreCase(String tipo);

}
