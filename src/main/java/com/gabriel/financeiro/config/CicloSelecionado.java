package com.gabriel.financeiro.config;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.service.CicloFinanceiroService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/*
 * Ciclo financeiro que as telas usam como filtro padrão.
 *
 * A escolha vem do parâmetro "ciclo" (referência "yyyy-MM" = mês em que o ciclo
 * termina, "atual" ou "todos") e fica guardada na sessão, para que todas as telas
 * sigam o mesmo ciclo. Sem escolha, vale o ciclo atual.
 */
@Component
public class CicloSelecionado {

	public static final String PARAMETRO = "ciclo";

	public static final String ATUAL = "atual";
	public static final String TODOS = "todos";

	private static final String CHAVE_SESSAO = "cicloSelecionado";

	private final CicloFinanceiroService cicloService;

	public CicloSelecionado(CicloFinanceiroService cicloService) {
		this.cicloService = cicloService;
	}

	/*
	 * Resolve o ciclo selecionado (atualizando a sessão se "parametro" for válido) e
	 * publica no model os dados do seletor de ciclo (fragments/ciclo :: seletor).
	 * Retorna null quando o usuário escolheu ver todos os ciclos.
	 */
	public CicloFinanceiro resolver(String parametro, HttpServletRequest request, Model model) {

		CicloFinanceiro atual = cicloService.getCicloAtual();
		YearMonth referenciaAtual = YearMonth.from(atual.getDataFim());

		HttpSession sessao = request.getSession();

		String escolha = normalizar(parametro);

		if (escolha != null) {
			sessao.setAttribute(CHAVE_SESSAO, escolha);
		} else {
			escolha = normalizar((String) sessao.getAttribute(CHAVE_SESSAO));
		}

		CicloFinanceiro ciclo;

		if (TODOS.equals(escolha)) {
			ciclo = null;
		} else if (escolha == null || ATUAL.equals(escolha) || escolha.equals(referenciaAtual.toString())) {
			ciclo = atual;
		} else {
			ciclo = cicloService.getCicloPorReferencia(YearMonth.parse(escolha));
		}

		// Navegação: a partir do ciclo exibido (ou do atual, quando "todos")
		YearMonth referencia = ciclo == null ? referenciaAtual : YearMonth.from(ciclo.getDataFim());

		model.addAttribute("cicloSelecionado", ciclo);
		model.addAttribute("cicloTodos", ciclo == null);
		model.addAttribute("cicloEhAtual", ciclo != null && referencia.equals(referenciaAtual));
		model.addAttribute("cicloAnteriorRef", referencia.minusMonths(1).toString());
		model.addAttribute("cicloProximoRef", referencia.plusMonths(1).toString());

		return ciclo;
	}

	/*
	 * Ciclo selecionado para cálculos fora de uma tela (ex.: respostas AJAX),
	 * sem alterar a sessão nem o model.
	 */
	public CicloFinanceiro daSessao(HttpServletRequest request) {

		HttpSession sessao = request.getSession(false);

		String escolha = sessao == null ? null : normalizar((String) sessao.getAttribute(CHAVE_SESSAO));

		if (TODOS.equals(escolha)) {
			return null;
		}

		if (escolha == null || ATUAL.equals(escolha)) {
			return cicloService.getCicloAtual();
		}

		return cicloService.getCicloPorReferencia(YearMonth.parse(escolha));
	}

	/*
	 * "atual", "todos" ou uma referência yyyy-MM válida; qualquer outro valor vira null.
	 */
	private String normalizar(String valor) {

		if (valor == null || valor.isBlank()) {
			return null;
		}

		String v = valor.trim().toLowerCase();

		if (ATUAL.equals(v) || TODOS.equals(v)) {
			return v;
		}

		try {
			YearMonth referencia = YearMonth.parse(v);

			if (referencia.getYear() < 1900 || referencia.getYear() > 9999) {
				return null;
			}

			return referencia.toString();

		} catch (DateTimeParseException e) {
			return null;
		}
	}
}
