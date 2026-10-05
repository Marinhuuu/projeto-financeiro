package com.gabriel.financeiro.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gabriel.financeiro.entities.Categoria;
import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.entities.Receita;
import com.gabriel.financeiro.repository.CategoriaRepository;
import com.gabriel.financeiro.repository.ReceitaRepository;

@Service
public class ReceitaService {

	private final ReceitaRepository receitaRepo;

	private final CicloFinanceiroService cicloFinanceiroService;

	private final CategoriaRepository categoriaRepo;

	public ReceitaService(
			ReceitaRepository receitaRepo,
			CicloFinanceiroService cicloFinanceiroService,
			CategoriaRepository categoriaRepo) {

		this.receitaRepo = receitaRepo;
		this.cicloFinanceiroService = cicloFinanceiroService;
		this.categoriaRepo = categoriaRepo;
	}

	public Page<Receita> listReceita(Pageable pageable) {

		return receitaRepo.findAll(pageable);
	}

	public Receita insertReceita(Receita receita) {

		if (receita.getDescricao() == null || receita.getDescricao().isBlank()) {
			throw new RuntimeException("Informe a descrição da receita");
		}

		if (receita.getValor() == null || receita.getValor().compareTo(BigDecimal.ZERO) <= 0) {
			throw new RuntimeException("O valor da receita deve ser maior que zero");
		}

		if (receita.getDataEntrada() == null) {
			throw new RuntimeException("Informe a data de entrada");
		}

		if (receita.getCategoria() == null || receita.getCategoria().getId() == null) {
			throw new RuntimeException("É necessário selecionar uma categoria");
		}

		Categoria categoria = categoriaRepo.findById(receita.getCategoria().getId())
				.orElseThrow(() -> new RuntimeException("Categoria não encontrada"));

		if (!"RECEITA".equalsIgnoreCase(categoria.getTipo())) {
			throw new RuntimeException("A categoria selecionada não é do tipo RECEITA");
		}

		receita.setCategoria(categoria);

		LocalDate dataEntrada = receita.getDataEntrada();

		CicloFinanceiro ciclo =
				cicloFinanceiroService.getOuCriarCiclo(dataEntrada);

		receita.setCiclo(ciclo);

		return receitaRepo.save(receita);
	}

	public List<Receita> buscarPorMesEAno(Integer mes, Integer ano) {

	    LocalDate inicio = LocalDate.of(ano, mes, 1);
	    LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());

	    return receitaRepo.findByDataEntradaBetween(inicio, fim);
	}

	public void excluirReceita(UUID id) {
	    receitaRepo.deleteById(id);
	}
}
