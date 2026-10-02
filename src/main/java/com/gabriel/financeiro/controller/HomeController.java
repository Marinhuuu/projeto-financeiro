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

	@GetMapping("/")
	public String redirectHome() {
		return "redirect:/home";
	}

	@GetMapping("/home")
	public String home(Model model) {

		CicloFinanceiro ciclo = cicloFinanceiroService.getCicloAtual();

		LocalDate hoje = LocalDate.now();

		// Dia do fechamento da fatura
		int diaFechamento = 5;

		// Próxima data de fechamento
		LocalDate dataReferencia;

		if (hoje.getDayOfMonth() >= diaFechamento) {
			dataReferencia = LocalDate.of(hoje.getYear(), hoje.getMonth(), diaFechamento).plusMonths(1);
		} else {
			dataReferencia = LocalDate.of(hoje.getYear(), hoje.getMonth(), diaFechamento);
		}

		Integer mesAtual = dataReferencia.getMonthValue();
		Integer anoAtual = dataReferencia.getYear();

		LocalDate inicioMes = LocalDate.of(anoAtual, mesAtual, 1);
		LocalDate fimMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());

		// =========================================================
		// RECEITAS DO MÊS
		// =========================================================

		BigDecimal totalReceitas = receitaRepo.somarReceitasPorPeriodo(inicioMes, fimMes);

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
		// MINHAS DESPESAS GERAIS (Pessoa == null)
		// =========================================================

		BigDecimal totalDespesasProprias = totalDespesas.subtract(totalDespesasTerceiros);

		// =========================================================
		// PARCELAS (PENDENTES GERAIS)
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

			// PARCELA PRÓPRIA
			if (parcela.getDespesa() != null && parcela.getDespesa().getPessoa() == null) {

				totalParcelasProprias = totalParcelasProprias.add(parcela.getValorParcela());
				quantidadeParcelasProprias++;
			}
			// PARCELA DE TERCEIRO
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

			valorParcelaPropria = totalParcelasProprias.divide(BigDecimal.valueOf(quantidadeParcelasProprias), 2,
					RoundingMode.HALF_UP);
		}

		if (quantidadeParcelasTerceiros > 0) {

			valorParcelaTerceiro = totalParcelasTerceiros.divide(BigDecimal.valueOf(quantidadeParcelasTerceiros), 2,
					RoundingMode.HALF_UP);
		}

		// =========================================================
		// FATURAS DO MÊS
		// =========================================================

		List<FaturaCartao> faturas = faturaRepo.findByMesReferenciaAndAnoReferencia(mesAtual, anoAtual);

		// TOTAL DAS FATURAS DO MÊS
		BigDecimal somaFaturaMes = faturas.stream().map(FaturaCartao::getValorTotal).filter(valor -> valor != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// =========================================================
		// CÁLCULO DO SALDO ESTIMADO DO MÊS
		//
		// Considera somente os gastos que pertencem ao próprio usuário (pessoa == null).
		// Para despesas parceladas no cartão, desconta apenas a parcela do mês em questão.
		// Despesas de terceiros NÃO impactam o saldo pessoal do usuário.
		// =========================================================

		// 1. Despesas próprias do mês que não usam cartão (à vista, pix, dinheiro, débito)
		BigDecimal despesasPropriasNaoCartaoMes = despesaRepo.somarDespesasPessoaisNaoCartaoPorPeriodo(inicioMes, fimMes);
		if (despesasPropriasNaoCartaoMes == null) {
			despesasPropriasNaoCartaoMes = BigDecimal.ZERO;
		}

		// 2. Parcelas do cartão pertencentes ao próprio usuário na fatura deste mês
		BigDecimal parcelasPropriasCartaoMes = parcelaRepo.somarParcelasPropriasPorMesEAno(mesAtual, anoAtual);
		if (parcelasPropriasCartaoMes == null) {
			parcelasPropriasCartaoMes = BigDecimal.ZERO;
		}

		// Total de saídas do próprio usuário no mês
		BigDecimal totalGastosPropriosMes = despesasPropriasNaoCartaoMes.add(parcelasPropriasCartaoMes);

		// Saldo estimado = Receitas do mês - Gastos próprios do mês
		BigDecimal saldoMes = totalReceitas.subtract(totalGastosPropriosMes);

		// =========================================================
		// QUANTIDADE DE FATURAS
		// =========================================================

		long totalFaturasAbertas = faturas.stream().filter(f -> f.getStatusFatura() != null).count();

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

		model.addAttribute("totalGastosPropriosMes", totalGastosPropriosMes);

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