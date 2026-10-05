package com.gabriel.financeiro.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gabriel.financeiro.entities.Categoria;
import com.gabriel.financeiro.repository.CategoriaRepository;
import com.gabriel.financeiro.repository.DespesaRepository;
import com.gabriel.financeiro.repository.ReceitaRepository;

@Service
public class CategoriaService {

	private final CategoriaRepository categoriaRepo;
	private final DespesaRepository despesaRepo;
	private final ReceitaRepository receitaRepo;

	public CategoriaService(CategoriaRepository categoriaRepo, DespesaRepository despesaRepo,
			ReceitaRepository receitaRepo) {
		this.categoriaRepo = categoriaRepo;
		this.despesaRepo = despesaRepo;
		this.receitaRepo = receitaRepo;
	}

	public Page<Categoria> listCategoria(Pageable pageable) {
		return categoriaRepo.findAll(pageable);
	}

	public List<Categoria> listByTipo(String tipo) {
		return categoriaRepo.findByTipoIgnoreCaseOrderByNomeAsc(tipo);
	}

	public Categoria getById(UUID id) {
		return categoriaRepo.findById(id)
				.orElseThrow(() -> new RuntimeException("Categoria nao encontrada"));
	}

	public Categoria insertCategoria(Categoria categoria) {
		validar(categoria);

		return categoriaRepo.save(categoria);
	}

	public Categoria updateCategoria(UUID id, Categoria categoria) {
		validar(categoria);

		Categoria existente = categoriaRepo.findById(id)
				.orElseThrow(() -> new RuntimeException("Categoria nao encontrada"));

		existente.setNome(categoria.getNome());
		existente.setTipo(categoria.getTipo());

		return categoriaRepo.save(existente);
	}

	public void deleteCategoria(UUID id) {
		Categoria categoria = getById(id);

		if (despesaRepo.existsByCategoria(categoria) || receitaRepo.existsByCategoria(categoria)) {
			throw new RuntimeException("Não é possível excluir: a categoria possui lançamentos vinculados");
		}

		categoriaRepo.delete(categoria);
	}

	private void validar(Categoria categoria) {
		if (categoria.getNome() == null || categoria.getNome().isBlank()) {
			throw new RuntimeException("Informe o nome da categoria");
		}

		if (!"DESPESA".equals(categoria.getTipo()) && !"RECEITA".equals(categoria.getTipo())) {
			throw new RuntimeException("O tipo da categoria deve ser DESPESA ou RECEITA");
		}
	}
}
