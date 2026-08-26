package com.gabriel.financeiro.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gabriel.financeiro.entities.Categoria;
import com.gabriel.financeiro.repository.CategoriaRepository;

@Service

public class CategoriaService {

	private final CategoriaRepository categoriaRepo;

	public CategoriaService(CategoriaRepository categoriaRepo) {
		this.categoriaRepo = categoriaRepo;
	}

	public Page<Categoria> listCategoria(Pageable pageable) {

		return categoriaRepo.findAll(pageable);

	}
	
	public Categoria getById(UUID id) {
		return categoriaRepo.findById(id)
				.orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
	}

	public Categoria insertCategoria(Categoria categoria) {
		
		return categoriaRepo.save(categoria);
		
	}
	
	public Categoria updateCategoria(UUID Id, Categoria categoria) {
		Categoria Categoria = categoriaRepo.findById(Id)
				.orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
		
		Categoria.setNome(categoria.getNome());
		Categoria.setTipo(categoria.getTipo());
	
		return categoriaRepo.save(Categoria);
	}

	public void deleteCategoria(UUID id) {
		 categoriaRepo.deleteById(id);
		
	}
	
	
}
