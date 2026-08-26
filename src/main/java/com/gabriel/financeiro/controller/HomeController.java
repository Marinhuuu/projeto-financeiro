package com.gabriel.financeiro.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.entities.FaturaCartao;
import com.gabriel.financeiro.entities.Parcela;
import com.gabriel.financeiro.enums.StatusParcela;
import com.gabriel.financeiro.repository.DespesaRepository;
import com.gabriel.financeiro.repository.FaturaCartaoRepository;
import com.gabriel.financeiro.repository.ParcelaRepository;
import com.gabriel.financeiro.repository.ReceitaRepository;
import com.gabriel.financeiro.service.CicloFinanceiroService;

@Controller
public class HomeController {

	private final CicloFinanceiroService cicloFinanceiroService;
	private final ReceitaRepository receitaRepo;
	private final DespesaRepository despesaRepo;
	private final ParcelaRepository parcelaRepo;
	private final FaturaCartaoRepository faturaRepo;

	public HomeController(CicloFinanceiroService cicloFinanceiroService, ReceitaRepository receitaRepo,
			DespesaRepository despesaRepo, ParcelaRepository parcelaRepo, FaturaCartaoRepository faturaRepo) {

		this.cicloFinanceiroService = cicloFinanceiroService;
		this.receitaRepo = receitaRepo;
		this.despesaRepo = despesaRepo;
		this.parcelaRepo = parcelaRepo;
		this.faturaRepo = faturaRepo;
	}

	@GetMapping("/home")
	public String home(Model model) {

		CicloFinanceiro ciclo = cicloFinanceiroService.getCicloAtual();

		// =========================================================
		// RECEITAS
		// =========================================================

		BigDecimal totalReceitas = receitaRepo.somarReceitas();

		if (totalReceitas == null) {
			totalReceitas = BigDecimal.ZERO;
		}

		// =========================================================
		// DESPESAS
		// =========================================================

		BigDecimal totalDespesas = despesaRepo.somarDespesas();

		if (totalDespesas == null) {
			totalDespesas = BigDecimal.ZERO;
		}

		// =========================================================
		// DESPESAS DE TERCEIROS
		// =========================================================

		BigDecimal totalDespesasTerceiros = despesaRepo.somarDespesasDeTerceiros();

		if (totalDespesasTerceiros == null) {
			totalDespesasTerceiros = BigDecimal.ZERO;
		}

		long quantidadeDespesasTerceiros = despesaRepo.contarDespesasDeTerceiros();

		// =========================================================
		// MINHAS DESPESAS
		//
		// Pessoa == null = despesa própria
		// =========================================================

		BigDecimal totalDespesasProprias = totalDespesas.subtract(totalDespesasTerceiros);

		// =========================================================
		// PARCELAS
		//
		// Pessoa == null -> minhas parcelas
		// Pessoa != null -> parcelas de terceiros
		// =========================================================

		List<Parcela> parcelas = parcelaRepo.findAll();

		BigDecimal totalParcelasProprias = BigDecimal.ZERO;
		BigDecimal totalParcelasTerceiros = BigDecimal.ZERO;

		long quantidadeParcelasProprias = 0;
		long quantidadeParcelasTerceiros = 0;

		for (Parcela parcela : parcelas) {

			// Só considera parcelas pendentes
			if (parcela.getStatusParcela() != StatusParcela.PENDENTE) {
				continue;
			}

			if (parcela.getValorParcela() == null) {
				continue;
			}

			// =====================================================
			// PARCELA PRÓPRIA
			// =====================================================

			if (parcela.getDespesa() != null && parcela.getDespesa().getPessoa() == null) {

				totalParcelasProprias = totalParcelasProprias.add(parcela.getValorParcela());

				quantidadeParcelasProprias++;
			}

			// =====================================================
			// PARCELA DE TERCEIRO
			// =====================================================

			else if (parcela.getDespesa() != null && parcela.getDespesa().getPessoa() != null) {

				totalParcelasTerceiros = totalParcelasTerceiros.add(parcela.getValorParcela());

				quantidadeParcelasTerceiros++;
			}
		}

		// =========================================================
		// VALOR INDIVIDUAL DAS PARCELAS
		// =========================================================

		BigDecimal valorParcelaPropria = BigDecimal.ZERO;
		BigDecimal valorParcelaTerceiro = BigDecimal.ZERO;

		if (quantidadeParcelasProprias > 0) {

			valorParcelaPropria = totalParcelasProprias.divide(
					BigDecimal.valueOf(quantidadeParcelasProprias),
					2,
					RoundingMode.HALF_UP
			);
		}

		if (quantidadeParcelasTerceiros > 0) {

			valorParcelaTerceiro = totalParcelasTerceiros.divide(
					BigDecimal.valueOf(quantidadeParcelasTerceiros),
					2,
					RoundingMode.HALF_UP
			);
		}

		// =========================================================
		// FATURAS DO MÊS
		// =========================================================

		LocalDate hoje = LocalDate.now();

		// Dia do fechamento da fatura
		int diaFechamento = 5;

		// Próxima data de fechamento
		LocalDate dataReferencia;

		if (hoje.getDayOfMonth() >= diaFechamento) {
		    dataReferencia = LocalDate.of(
		            hoje.getYear(),
		            hoje.getMonth(),
		            diaFechamento
		    ).plusMonths(1);
		} else {
		    dataReferencia = LocalDate.of(
		            hoje.getYear(),
		            hoje.getMonth(),
		            diaFechamento
		    );
		}

		Integer mesAtual = dataReferencia.getMonthValue();
		Integer anoAtual = dataReferencia.getYear();

		List<FaturaCartao> faturas =
		        faturaRepo.findByMesReferenciaAndAnoReferencia(mesAtual, anoAtual);
		

		// =========================================================
		// TOTAL DAS FATURAS DO MÊS
		// =========================================================
		BigDecimal somaFaturaMes = faturas.stream()
		        .map(FaturaCartao::getValorTotal)
		        .filter(valor -> valor != null)
		        .reduce(BigDecimal.ZERO, BigDecimal::add);

		// =========================================================
		// SALDO DO MÊS
		//
		// Receitas - Despesas
		//
		// A fatura NÃO é descontada novamente aqui porque a despesa
		// do cartão já pertence ao total de despesas.
		// =========================================================

		BigDecimal saldoMes = totalReceitas.subtract(totalDespesas);

		// =========================================================
		// QUANTIDADE DE FATURAS
		// =========================================================

		long totalFaturasAbertas = faturas.stream()
				.filter(f -> f.getStatusFatura() != null)
				.count();

		// =========================================================
		// ENVIA PARA O THYMELEAF
		// =========================================================

		model.addAttribute("ciclo", ciclo);

		model.addAttribute("totalReceitas", totalReceitas);

		model.addAttribute("totalDespesas", totalDespesas);

		model.addAttribute("totalDespesasProprias", totalDespesasProprias);

		model.addAttribute("totalDespesasTerceiros", totalDespesasTerceiros);

		model.addAttribute("quantidadeDespesasTerceiros", quantidadeDespesasTerceiros);

		// Totais das parcelas
		model.addAttribute("totalParcelasProprias", totalParcelasProprias);

		model.addAttribute("totalParcelasTerceiros", totalParcelasTerceiros);

		// Valores individuais das parcelas
		model.addAttribute("valorParcelaPropria", valorParcelaPropria);

		model.addAttribute("valorParcelaTerceiro", valorParcelaTerceiro);

		// Quantidades
		model.addAttribute("quantidadeParcelasProprias", quantidadeParcelasProprias);

		model.addAttribute("quantidadeParcelasTerceiros", quantidadeParcelasTerceiros);

		model.addAttribute("totalFaturaMes", somaFaturaMes);	
		
		model.addAttribute("totalFaturasAbertas", totalFaturasAbertas);

		model.addAttribute("faturas", faturas);

		// Saldo
		model.addAttribute("saldoMes", saldoMes);

		// Referência atual
		model.addAttribute("mesAtual", mesAtual);

		model.addAttribute("anoAtual", anoAtual);
		model.addAttribute("dataReferencia", dataReferencia);

		return "home";
	}
}