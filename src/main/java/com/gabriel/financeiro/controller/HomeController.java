package com.gabriel.financeiro.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gabriel.financeiro.config.CicloSelecionado;
import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.entities.FaturaCartao;
import com.gabriel.financeiro.enums.StatusFatura;
import com.gabriel.financeiro.repository.DespesaRepository;
import com.gabriel.financeiro.repository.FaturaCartaoRepository;
import com.gabriel.financeiro.repository.ParcelaRepository;
import com.gabriel.financeiro.repository.ReceitaRepository;
import com.gabriel.financeiro.service.CicloFinanceiroService;
import com.gabriel.financeiro.service.FaturaCartaoService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class HomeController {

	private final CicloFinanceiroService cicloFinanceiroService;
	private final FaturaCartaoService faturaService;
	private final ReceitaRepository receitaRepo;
	private final DespesaRepository despesaRepo;
	private final ParcelaRepository parcelaRepo;
	private final FaturaCartaoRepository faturaRepo;
	private final CicloSelecionado cicloSelecionado;

	public HomeController(CicloFinanceiroService cicloFinanceiroService, FaturaCartaoService faturaService,
			ReceitaRepository receitaRepo, DespesaRepository despesaRepo, ParcelaRepository parcelaRepo,
			FaturaCartaoRepository faturaRepo, CicloSelecionado cicloSelecionado) {

		this.cicloSelecionado = cicloSelecionado;
		this.cicloFinanceiroService = cicloFinanceiroService;
		this.faturaService = faturaService;
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
	public String home(@RequestParam(name = CicloSelecionado.PARAMETRO, required = false) String cicloParam,
			HttpServletRequest request, Model model) {

		// O período da home é o ciclo financeiro selecionado (padrão: o atual), que
		// segue o dia de fechamento da ConfiguracaoFinanceira. O mês de referência
		// das faturas é o mês em que o ciclo termina.
		CicloFinanceiro ciclo = cicloSelecionado.resolver(cicloParam, request, model);

		// O painel é sempre de um ciclo: "todos" exibe o ciclo atual
		if (ciclo == null) {
			ciclo = cicloFinanceiroService.getCicloAtual();
			model.addAttribute("cicloSelecionado", ciclo);
			model.addAttribute("cicloTodos", false);
			model.addAttribute("cicloEhAtual", true);
		}

		LocalDate inicioCiclo = ciclo.getDataInicio();
		LocalDate fimCiclo = ciclo.getDataFim();

		LocalDate dataReferencia = fimCiclo;

		YearMonth referencia = YearMonth.from(fimCiclo);
		Integer mesAtual = referencia.getMonthValue();
		Integer anoAtual = referencia.getYear();

		// =========================================================
		// RECEITAS DO CICLO
		// =========================================================

		BigDecimal totalReceitas = receitaRepo.somarReceitasPorPeriodo(inicioCiclo, fimCiclo);

		// =========================================================
		// DESPESAS PROPRIAS FORA DO CARTAO (pessoa == null)
		// =========================================================

		BigDecimal totalDespesasForaCartao = despesaRepo.somarDespesasPessoaisNaoCartaoPorPeriodo(inicioCiclo, fimCiclo);

		// =========================================================
		// MINHAS PARCELAS NAS FATURAS DO MES DE REFERENCIA
		// =========================================================

		BigDecimal totalParcelasProprias = parcelaRepo.somarParcelasPropriasPorMesEAno(mesAtual, anoAtual);

		// Minhas despesas do mes: fora do cartao (PIX, debito, dinheiro...) + parcelas do cartao
		BigDecimal totalDespesasProprias = totalDespesasForaCartao.add(totalParcelasProprias);

		long quantidadeParcelasProprias = parcelaRepo.contarParcelasPropriasPorMesEAno(mesAtual, anoAtual);

		// =========================================================
		// FATURAS DO MES
		// =========================================================

		List<FaturaCartao> faturas = faturaRepo.findByMesReferenciaAndAnoReferencia(mesAtual, anoAtual);

		faturaService.atualizarStatus(faturas);

		// TOTAL DAS FATURAS DO MES
		BigDecimal somaFaturaMes = faturas.stream().map(FaturaCartao::getValorTotal).filter(valor -> valor != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// =========================================================
		// CALCULO DO SALDO ESTIMADO DO CICLO
		// =========================================================

		// Total de saidas do proprio usuario: despesas fora do cartao + parcelas proprias
		BigDecimal totalGastosPropriosMes = totalDespesasProprias;

		// Saldo estimado = Receitas - Gastos proprios
		BigDecimal saldoMes = totalReceitas.subtract(totalGastosPropriosMes);

		// =========================================================
		// QUANTIDADE DE FATURAS EM ABERTO (nao pagas)
		// =========================================================

		long totalFaturasAbertas = faturas.stream().filter(f -> f.getStatusFatura() != StatusFatura.PAGA).count();

		// =========================================================
		// ENVIA PARA O THYMELEAF
		// =========================================================

		model.addAttribute("ciclo", ciclo);
		model.addAttribute("totalReceitas", totalReceitas);
		model.addAttribute("totalDespesasProprias", totalDespesasProprias);
		model.addAttribute("totalDespesasForaCartao", totalDespesasForaCartao);

		// Parcelas do mes
		model.addAttribute("totalParcelasProprias", totalParcelasProprias);
		model.addAttribute("quantidadeParcelasProprias", quantidadeParcelasProprias);

		model.addAttribute("totalFaturaMes", somaFaturaMes);
		model.addAttribute("totalGastosPropriosMes", totalGastosPropriosMes);
		model.addAttribute("totalFaturasAbertas", totalFaturasAbertas);
		model.addAttribute("faturas", faturas);

		// Saldo
		model.addAttribute("saldoMes", saldoMes);

		// Referencia atual
		model.addAttribute("mesAtual", mesAtual);
		model.addAttribute("anoAtual", anoAtual);
		model.addAttribute("dataReferencia", dataReferencia);

		return "home";
	}
}
