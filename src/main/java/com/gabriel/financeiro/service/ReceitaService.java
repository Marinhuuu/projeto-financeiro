package com.gabriel.financeiro.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gabriel.financeiro.entities.Categoria;
import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.entities.Receita;
import com.gabriel.financeiro.repository.CategoriaRepository;
import com.gabriel.financeiro.repository.ReceitaRepository;

@Service
public class ReceitaService {

	private static final int MAX_OCORRENCIAS = 60;

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

	@Transactional
	public Receita insertReceita(Receita receita) {

		if (receita.getDescricao() == null || receita.getDescricao().isBlank()) {
			throw new RuntimeException("Informe a descrição da receita");
		}

		if (receita.getValor() == null || receita.getValor().compareTo(BigDecimal.ZERO) <= 0) {
			throw new RuntimeException("O valor da receita deve ser maior que zero");
		}

		if (receita.getValor().stripTrailingZeros().scale() > 2) {
			throw new RuntimeException("O valor deve ter no máximo 2 casas decimais");
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

		// Recorrência: a receita se repete todo mês por totalOcorrencias meses
		int ocorrencias = 1;

		if (Boolean.TRUE.equals(receita.getRecorrente())) {

			Integer total = receita.getTotalOcorrencias();

			if (total == null || total < 2 || total > MAX_OCORRENCIAS) {
				throw new RuntimeException("A recorrência deve ser de 2 a " + MAX_OCORRENCIAS + " meses");
			}

			ocorrencias = total;

			receita.setGrupoRecorrencia(UUID.randomUUID());
			receita.setOcorrencia(1);

		} else {

			receita.setRecorrente(false);
			receita.setGrupoRecorrencia(null);
			receita.setOcorrencia(null);
			receita.setTotalOcorrencias(null);
		}

		Receita primeira = null;

		for (int i = 0; i < ocorrencias; i++) {

			Receita atual = i == 0 ? receita : copiarParaMesSeguinte(receita, i);

			// Cada ocorrência cai no ciclo do seu próprio mês
			LocalDate dataEntrada = atual.getDataEntrada();

			CicloFinanceiro ciclo =
					cicloFinanceiroService.getOuCriarCiclo(dataEntrada);

			atual.setCiclo(ciclo);

			Receita salva = receitaRepo.save(atual);

			if (primeira == null) {
				primeira = salva;
			}
		}

		return primeira;
	}

	/*
	 * Cópia da receita original "meses" meses depois (plusMonths sobre a data
	 * original, como em DespesaService: 31/01 -> 28/02 -> 31/03).
	 */
	private Receita copiarParaMesSeguinte(Receita original, int meses) {

		Receita copia = new Receita();

		copia.setDescricao(original.getDescricao());
		copia.setValor(original.getValor());
		copia.setDataEntrada(original.getDataEntrada().plusMonths(meses));
		copia.setRecorrente(true);
		copia.setCategoria(original.getCategoria());
		copia.setGrupoRecorrencia(original.getGrupoRecorrencia());
		copia.setOcorrencia(meses + 1);
		copia.setTotalOcorrencias(original.getTotalOcorrencias());

		return copia;
	}

	/*
	 * Receitas com data de entrada no ciclo; ciclo null = todas.
	 */
	public List<Receita> listarReceitasDoCiclo(CicloFinanceiro ciclo) {

		if (ciclo == null) {
			return receitaRepo.findAllByOrderByDataEntradaDesc();
		}

		return receitaRepo.findByDataEntradaBetweenOrderByDataEntradaDesc(ciclo.getDataInicio(), ciclo.getDataFim());
	}

	public void excluirReceita(UUID id) {
	    receitaRepo.delete(buscarPorId(id));
	}

	/*
	 * Exclui a ocorrência informada e as seguintes da mesma recorrência
	 * (as anteriores são mantidas). Retorna quantas foram excluídas.
	 */
	@Transactional
	public int excluirRecorrenciaAPartirDe(UUID id) {

		Receita receita = buscarPorId(id);

		if (!receita.isParteDeRecorrencia()) {
			receitaRepo.delete(receita);
			return 1;
		}

		List<Receita> seguintes = receitaRepo.findByGrupoRecorrenciaAndOcorrenciaGreaterThanEqual(
				receita.getGrupoRecorrencia(), receita.getOcorrencia());

		receitaRepo.deleteAll(seguintes);

		return seguintes.size();
	}

	private Receita buscarPorId(UUID id) {
		return receitaRepo.findById(id).orElseThrow(() -> new RuntimeException("Receita não encontrada"));
	}
}
