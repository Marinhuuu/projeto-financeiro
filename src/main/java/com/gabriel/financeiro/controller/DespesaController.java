package com.gabriel.financeiro.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gabriel.financeiro.config.CicloSelecionado;
import com.gabriel.financeiro.dto.GastoDoCiclo;
import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.entities.Categoria;
import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.entities.Despesa;
import com.gabriel.financeiro.entities.FormaPagamento;
import com.gabriel.financeiro.service.CartaoService;
import com.gabriel.financeiro.service.CategoriaService;
import com.gabriel.financeiro.service.DespesaService;

import jakarta.servlet.http.HttpServletRequest;
import com.gabriel.financeiro.service.FormaPagamentoService;
import com.gabriel.financeiro.service.PessoaService;

@Controller
@RequestMapping("/despesa")
public class DespesaController {

	private final DespesaService despesaServ;
	private final CategoriaService categoriaServ;
	private final CartaoService cartaoServ;
	private final PessoaService pessoaServ;
	private final FormaPagamentoService formaPagamentoServ;
	private final CicloSelecionado cicloSelecionado;

	public DespesaController(DespesaService despesaServ, CategoriaService categoriaServ, CartaoService cartaoServ,
			PessoaService pessoaServ, FormaPagamentoService formaPagamentoServ, CicloSelecionado cicloSelecionado) {

		this.despesaServ = despesaServ;
		this.categoriaServ = categoriaServ;
		this.cartaoServ = cartaoServ;
		this.pessoaServ = pessoaServ;
		this.formaPagamentoServ = formaPagamentoServ;
		this.cicloSelecionado = cicloSelecionado;
	}

	// =========================================================
	// TELA DE CADASTRO DE DESPESA
	// =========================================================

	@GetMapping
	public String pageDespesa(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(name = CicloSelecionado.PARAMETRO, required = false) String cicloParam,
			HttpServletRequest request, Model model) {

		Pageable pageable = PageRequest.of(page, size, Sort.by("dataCompra").descending());

		CicloFinanceiro ciclo = cicloSelecionado.resolver(cicloParam, request, model);

		Page<Despesa> paginaDespesa = despesaServ.listarDespesasDoCiclo(ciclo, pageable);

		Despesa despesa = new Despesa();
		despesa.setCategoria(new Categoria());
		despesa.setCartao(new CartaoCredito());
		despesa.setFormaPagamento(new FormaPagamento());

		model.addAttribute("despesa", despesa);
		model.addAttribute("paginaDespesa", paginaDespesa);

		// =====================================================
		// FORMAS DE PAGAMENTO
		// =====================================================
		model.addAttribute("formasPagamento", formaPagamentoServ.listarTodas());

		// =====================================================
		// CATEGORIAS (somente tipo DESPESA)
		// =====================================================
		model.addAttribute("categorias", categoriaServ.listByTipo("DESPESA"));

		// =====================================================
		// CARTOES
		// =====================================================
		Pageable pageableCartao = PageRequest.of(0, 100, Sort.by("nome").ascending());
		List<CartaoCredito> cartoes = cartaoServ.ListCartao(pageableCartao).getContent();
		model.addAttribute("cartoes", cartoes);

		// Auto-selecao: se houver exatamente 1 cartao, envia para o template
		if (cartoes.size() == 1) {
			model.addAttribute("cartaoUnico", cartoes.get(0));
			// th:field do select usa este id para já vir selecionado
			despesa.getCartao().setId(cartoes.get(0).getId());
		}

		// =====================================================
		// PESSOAS
		// =====================================================
		Pageable pageablePessoa = PageRequest.of(0, 100, Sort.by("nome").ascending());
		model.addAttribute("pessoas", pessoaServ.listPessoa(pageablePessoa));

		return "despesa";
	}

	// =========================================================
	// INSERIR DESPESA
	// =========================================================

	@PostMapping
	public String insertDespesa(Despesa despesa, RedirectAttributes redirectAttributes) {
		try {
			despesaServ.insertDespesa(despesa);
			redirectAttributes.addFlashAttribute("mensagem", "Despesa cadastrada com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro ao cadastrar despesa: " + e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
		}
		return "redirect:/despesa";
	}

	// =========================================================
	// GERENCIAR DESPESAS
	// =========================================================

	@GetMapping("/despesas")
	public String gerenciarDespesas(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(name = CicloSelecionado.PARAMETRO, required = false) String cicloParam,
			HttpServletRequest request, Model model) {

		Pageable pageable = PageRequest.of(page, size);

		CicloFinanceiro ciclo = cicloSelecionado.resolver(cicloParam, request, model);

		// Gastos consolidados: despesas fora do cartão + parcelas das faturas do ciclo
		Page<GastoDoCiclo> paginaGastos = despesaServ.listarGastosDoCiclo(ciclo, pageable);

		model.addAttribute("paginaGastos", paginaGastos);

		// Total do ciclo selecionado: despesas fora do cartão + parcelas do cartão
		model.addAttribute("ciclo", ciclo);

		if (ciclo != null) {
			BigDecimal totalForaCartao = despesaServ.somarDespesasPropriasForaDoCartao(ciclo);
			BigDecimal totalParcelas = despesaServ.somarParcelasPropriasDoCiclo(ciclo);

			model.addAttribute("totalForaCartao", totalForaCartao);
			model.addAttribute("totalParcelas", totalParcelas);
			model.addAttribute("totalDespesas", totalForaCartao.add(totalParcelas));
		}

		return "despesas";
	}

	// =========================================================
	// EXCLUIR
	// =========================================================

	@PostMapping("/excluir")
	public String excluirDespesa(@RequestParam UUID id, RedirectAttributes redirectAttributes) {
		try {
			despesaServ.excluirDespesa(id);
			redirectAttributes.addFlashAttribute("mensagem", "Despesa excluida com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro ao excluir despesa: " + e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
		}
		return "redirect:/despesa/despesas";
	}

	@PostMapping("/excluir-recorrencia")
	public String excluirRecorrencia(@RequestParam UUID id, RedirectAttributes redirectAttributes) {
		try {
			int excluidas = despesaServ.excluirRecorrenciaAPartirDe(id);
			redirectAttributes.addFlashAttribute("mensagem",
					excluidas == 1 ? "Despesa excluida com sucesso!" : excluidas + " despesas da recorrencia excluidas com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro ao excluir recorrencia: " + e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
		}
		return "redirect:/despesa/despesas";
	}

	// =========================================================
	// DESPESAS DE TERCEIROS
	// =========================================================

	@GetMapping("/terceiros")
	public String gastosDeTerceiros(@RequestParam(name = CicloSelecionado.PARAMETRO, required = false) String cicloParam,
			HttpServletRequest request, Model model) {

		CicloFinanceiro ciclo = cicloSelecionado.resolver(cicloParam, request, model);

		List<Despesa> despesasTerceiros = despesaServ.listarDespesasDeTerceiros(ciclo);

		model.addAttribute("despesasTerceiros", despesasTerceiros);

		return "despesas-terceiros";
	}

	// =========================================================
	// MARCAR COMO DEVOLVIDO
	// =========================================================

	@PostMapping("/terceiros/devolver")
	public String marcarComoDevolvido(@RequestParam UUID id, RedirectAttributes redirectAttributes) {
		try {
			despesaServ.marcarComoDevolvido(id);
			redirectAttributes.addFlashAttribute("mensagem", "Despesa marcada como devolvida!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro: " + e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
		}
		return "redirect:/despesa/terceiros";
	}

	// =========================================================
	// DESFAZER DEVOLUCAO
	// =========================================================

	@PostMapping("/terceiros/desfazer-devolucao")
	public String marcarComoNaoDevolvido(@RequestParam UUID id, RedirectAttributes redirectAttributes) {
		try {
			despesaServ.marcarComoNaoDevolvido(id);
			redirectAttributes.addFlashAttribute("mensagem", "Devolucao desfeita com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro: " + e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
		}
		return "redirect:/despesa/terceiros";
	}
}
