package com.gabriel.financeiro.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.entities.Receita;
import com.gabriel.financeiro.repository.ReceitaRepository;

@Service
public class ReceitaService {

	private final ReceitaRepository receitaRepo;

	private final CicloFinanceiroService cicloFinanceiroService;

	public ReceitaService(
			ReceitaRepository receitaRepo,
			CicloFinanceiroService cicloFinanceiroService) {

		this.receitaRepo = receitaRepo;
		this.cicloFinanceiroService = cicloFinanceiroService;
	}

	public Page<Receita> listReceita(Pageable pageable) {

		return receitaRepo.findAll(pageable);
	}

	public Receita insertReceita(Receita receita) {

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