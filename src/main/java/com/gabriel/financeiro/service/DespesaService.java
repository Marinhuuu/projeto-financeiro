package com.gabriel.financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gabriel.financeiro.dto.GastoDoCiclo;
import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.entities.Categoria;
import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.entities.Despesa;
import com.gabriel.financeiro.entities.FaturaCartao;
import com.gabriel.financeiro.entities.Parcela;
import com.gabriel.financeiro.entities.Pessoa;
import com.gabriel.financeiro.entities.FormaPagamento;
import com.gabriel.financeiro.enums.StatusParcela;
import com.gabriel.financeiro.repository.CartaoRepository;
import com.gabriel.financeiro.repository.CategoriaRepository;
import com.gabriel.financeiro.repository.DespesaRepository;
import com.gabriel.financeiro.repository.FormaPagamentoRepository;
import com.gabriel.financeiro.repository.ParcelaRepository;
import com.gabriel.financeiro.repository.PessoaRepository;

@Service
public class DespesaService {

	private static final int MAX_PARCELAS = 72;

	private static final int MAX_OCORRENCIAS = 60;

	private final DespesaRepository despesaRepo;
	private final CicloFinanceiroService cicloService;
	private final ParcelaRepository parcelaRepo;
	private final FaturaCartaoService faturaService;
	private final CartaoRepository cartaoRepo;
	private final PessoaRepository pessoaRepo;
	private final CategoriaRepository categoriaRepo;
	private final FormaPagamentoRepository formaPagamentoRepo;

	public DespesaService(DespesaRepository despesaRepo, CicloFinanceiroService cicloService,
			ParcelaRepository parcelaRepo, FaturaCartaoService faturaService, CartaoRepository cartaoRepo,
			PessoaRepository pessoaRepo, CategoriaRepository categoriaRepo,
			FormaPagamentoRepository formaPagamentoRepo) {
		this.despesaRepo = despesaRepo;
		this.cicloService = cicloService;
		this.parcelaRepo = parcelaRepo;
		this.faturaService = faturaService;
		this.cartaoRepo = cartaoRepo;
		this.pessoaRepo = pessoaRepo;
		this.categoriaRepo = categoriaRepo;
		this.formaPagamentoRepo = formaPagamentoRepo;
	}

	/*
	 * Despesas com data de compra no ciclo; ciclo null = todas.
	 */
	public Page<Despesa> listarDespesasDoCiclo(CicloFinanceiro ciclo, Pageable pageable) {

		if (ciclo == null) {
			return despesaRepo.findAll(pageable);
		}

		return despesaRepo.findByDataCompraBetween(ciclo.getDataInicio(), ciclo.getDataFim(), pageable);
	}

	/*
	 * Todos os gastos do ciclo numa só listagem: despesas fora do cartão com data
	 * de compra no ciclo + parcelas das faturas do mês de referência do ciclo.
	 * Compras no cartão entram só pelas parcelas. Ciclo null = todos os gastos.
	 */
	public Page<GastoDoCiclo> listarGastosDoCiclo(CicloFinanceiro ciclo, Pageable pageable) {

		List<Despesa> despesas;
		List<Parcela> parcelas;

		if (ciclo == null) {
			despesas = despesaRepo.listarDespesasSemParcelas();
			parcelas = parcelaRepo.findAll();
		} else {
			YearMonth referencia = YearMonth.from(ciclo.getDataFim());
			despesas = despesaRepo.listarDespesasSemParcelasPorPeriodo(ciclo.getDataInicio(), ciclo.getDataFim());
			parcelas = parcelaRepo.findByFaturaMesReferenciaAndFaturaAnoReferencia(referencia.getMonthValue(),
					referencia.getYear());
		}

		List<GastoDoCiclo> gastos = new ArrayList<>();
		despesas.forEach(d -> gastos.add(GastoDoCiclo.deDespesa(d)));
		parcelas.forEach(p -> gastos.add(GastoDoCiclo.deParcela(p)));

		gastos.sort(Comparator.comparing(GastoDoCiclo::getData, Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparing(g -> g.getDespesa().getDescricao(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
				.thenComparing(GastoDoCiclo::getNumeroParcela, Comparator.nullsFirst(Comparator.naturalOrder())));

		int inicio = (int) Math.min(pageable.getOffset(), gastos.size());
		int fim = Math.min(inicio + pageable.getPageSize(), gastos.size());

		return new PageImpl<>(gastos.subList(inicio, fim), pageable, gastos.size());
	}

	// =========================================================
	// TOTAIS DO CICLO (despesas próprias: pessoa == null)
	// =========================================================

	/*
	 * Despesas fora do cartão (PIX, débito, dinheiro...) com data de compra no ciclo.
	 */
	public BigDecimal somarDespesasPropriasForaDoCartao(CicloFinanceiro ciclo) {
		return despesaRepo.somarDespesasPessoaisNaoCartaoPorPeriodo(ciclo.getDataInicio(), ciclo.getDataFim());
	}

	/*
	 * Parcelas próprias nas faturas do mês de referência do ciclo (mês em que ele termina).
	 */
	public BigDecimal somarParcelasPropriasDoCiclo(CicloFinanceiro ciclo) {
		YearMonth referencia = YearMonth.from(ciclo.getDataFim());
		return parcelaRepo.somarParcelasPropriasPorMesEAno(referencia.getMonthValue(), referencia.getYear());
	}

	/*
	 * Total gasto no mês: despesas fora do cartão + parcelas do cartão.
	 */
	public BigDecimal somarDespesasPropriasDoCiclo(CicloFinanceiro ciclo) {
		return somarDespesasPropriasForaDoCartao(ciclo).add(somarParcelasPropriasDoCiclo(ciclo));
	}

	// =========================================================
	// CADASTRAR DESPESA
	// =========================================================

	@Transactional
	public Despesa insertDespesa(Despesa despesa) {

		if (despesa.getDescricao() == null || despesa.getDescricao().isBlank()) {
			throw new RuntimeException("Informe a descrição da despesa");
		}

		if (despesa.getValorTotal() == null || despesa.getValorTotal().compareTo(BigDecimal.ZERO) <= 0) {
			throw new RuntimeException("O valor da despesa deve ser maior que zero");
		}

		if (despesa.getDataCompra() == null) {
			throw new RuntimeException("Informe a data da compra");
		}

		// Se não informou pessoa,
		// consideramos que a despesa é do próprio Gabriel.
		// Pessoa continua null.

		FormaPagamento formaPagamento = null;
		if (despesa.getFormaPagamento() != null && despesa.getFormaPagamento().getId() != null) {
			formaPagamento = formaPagamentoRepo.findById(despesa.getFormaPagamento().getId())
					.orElseThrow(() -> new RuntimeException("Forma de pagamento não encontrada"));
		} else if (despesa.getFormaPagamento() != null && despesa.getFormaPagamento().getCodigo() != null) {
			formaPagamento = formaPagamentoRepo.findByCodigo(despesa.getFormaPagamento().getCodigo())
					.orElse(null);
		}
		despesa.setFormaPagamento(formaPagamento);

		boolean isCartao = formaPagamento != null && (
				Boolean.TRUE.equals(formaPagamento.getPermiteParcelamento()) ||
				"CARTAO_CREDITO".equalsIgnoreCase(formaPagamento.getCodigo()) ||
				"CARTAO_VISTA".equalsIgnoreCase(formaPagamento.getCodigo()) ||
				"CARTAO".equalsIgnoreCase(formaPagamento.getCodigo()) ||
				"Cartão de Crédito".equalsIgnoreCase(formaPagamento.getNome())
		);

		if (isCartao) {

			if (despesa.getCartao() == null || despesa.getCartao().getId() == null) {

				throw new RuntimeException("É necessário selecionar um cartão de crédito");
			}

			CartaoCredito cartao = cartaoRepo.findById(despesa.getCartao().getId())
					.orElseThrow(() -> new RuntimeException("Cartão não encontrado"));

			despesa.setCartao(cartao);

			// Crédito à Vista: sempre 1 parcela
			if ("CARTAO_VISTA".equalsIgnoreCase(formaPagamento.getCodigo())) {
				despesa.setQtdParcelas(1);
			} else if (despesa.getQtdParcelas() == null || despesa.getQtdParcelas() < 1) {
				despesa.setQtdParcelas(1);
			} else if (despesa.getQtdParcelas() > MAX_PARCELAS) {
				throw new RuntimeException("A quantidade de parcelas deve ser no máximo " + MAX_PARCELAS);
			}

		} else {

			// Despesa que não utiliza cartão
			despesa.setCartao(null);
			despesa.setQtdParcelas(null);
		}

		// Garante valor padrão
		if (despesa.getDevolvido() == null) {
			despesa.setDevolvido(false);
		}

		if (despesa.getPessoa() != null && despesa.getPessoa().getId() != null) {

			Pessoa pessoa = pessoaRepo.findById(despesa.getPessoa().getId())
					.orElseThrow(() -> new RuntimeException("Pessoa não encontrada"));

			despesa.setPessoa(pessoa);

		} else {

			despesa.setPessoa(null);
		}

		if (despesa.getCategoria() == null || despesa.getCategoria().getId() == null) {

		    throw new RuntimeException("É necessário selecionar uma categoria");
		}

		Categoria categoria = categoriaRepo.findById(despesa.getCategoria().getId())
		        .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));

		if (!"DESPESA".equalsIgnoreCase(categoria.getTipo())) {
			throw new RuntimeException("A categoria selecionada não é do tipo DESPESA");
		}

		despesa.setCategoria(categoria);

		// Recorrência: a despesa se repete todo mês por totalOcorrencias meses
		int ocorrencias = 1;

		if (Boolean.TRUE.equals(despesa.getRecorrente())) {

			Integer total = despesa.getTotalOcorrencias();

			if (total == null || total < 2 || total > MAX_OCORRENCIAS) {
				throw new RuntimeException("A recorrência deve ser de 2 a " + MAX_OCORRENCIAS + " meses");
			}

			ocorrencias = total;

			despesa.setGrupoRecorrencia(UUID.randomUUID());
			despesa.setOcorrencia(1);

		} else {

			despesa.setGrupoRecorrencia(null);
			despesa.setOcorrencia(null);
			despesa.setTotalOcorrencias(null);
		}

		Despesa primeira = null;

		for (int i = 0; i < ocorrencias; i++) {

			Despesa atual = i == 0 ? despesa : copiarParaMesSeguinte(despesa, i);

			// Cada ocorrência cai no ciclo (e nas faturas) do seu próprio mês
			atual.setCiclo(cicloService.getOuCriarCiclo(atual.getDataCompra()));

			Despesa salva = despesaRepo.save(atual);

			if (isCartao) {

				gerarParcelas(salva);
			}

			if (primeira == null) {
				primeira = salva;
			}
		}

		return primeira;
	}

	/*
	 * Cópia da despesa original "meses" meses depois.
	 * plusMonths sobre a data original ajusta o dia ao fim do mês sem acumular deslocamento
	 * (31/01 -> 28/02 -> 31/03).
	 */
	private Despesa copiarParaMesSeguinte(Despesa original, int meses) {

		Despesa copia = new Despesa();

		copia.setDescricao(original.getDescricao());
		copia.setValorTotal(original.getValorTotal());
		copia.setDataCompra(original.getDataCompra().plusMonths(meses));
		copia.setFormaPagamento(original.getFormaPagamento());
		copia.setQtdParcelas(original.getQtdParcelas());
		copia.setCategoria(original.getCategoria());
		copia.setCartao(original.getCartao());
		copia.setPessoa(original.getPessoa());
		copia.setDevolvido(false);
		copia.setGrupoRecorrencia(original.getGrupoRecorrencia());
		copia.setOcorrencia(meses + 1);
		copia.setTotalOcorrencias(original.getTotalOcorrencias());

		return copia;
	}

	// =========================================================
	// LISTAR DESPESAS DE TERCEIROS
	// =========================================================

	/*
	 * Despesas de terceiros com data de compra no ciclo; ciclo null = todas.
	 */
	public List<Despesa> listarDespesasDeTerceiros(CicloFinanceiro ciclo) {

		if (ciclo == null) {
			return despesaRepo.listarDespesasDeTerceiros();
		}

		return despesaRepo.listarDespesasDeTerceirosPorPeriodo(ciclo.getDataInicio(), ciclo.getDataFim());
	}

	// =========================================================
	// MARCAR COMO DEVOLVIDO
	// =========================================================

	@Transactional
	public void marcarComoDevolvido(UUID id) {

		Despesa despesa = despesaRepo.findById(id).orElseThrow(() -> new RuntimeException("Despesa não encontrada"));

		if (despesa.getPessoa() == null) {
			throw new RuntimeException("Somente despesas de terceiros podem ser marcadas como devolvidas");
		}

		despesa.setDevolvido(true);

		despesaRepo.save(despesa);
	}

	// =========================================================
	// MARCAR COMO NÃO DEVOLVIDO
	// =========================================================

	@Transactional
	public void marcarComoNaoDevolvido(UUID id) {

		Despesa despesa = despesaRepo.findById(id).orElseThrow(() -> new RuntimeException("Despesa não encontrada"));

		despesa.setDevolvido(false);

		despesaRepo.save(despesa);
	}

	// =========================================================
	// EXCLUIR DESPESA
	// =========================================================

	@Transactional
	public void excluirDespesa(UUID id) {

		excluir(despesaRepo.findById(id).orElseThrow(() -> new RuntimeException("Despesa não encontrada")));
	}

	/*
	 * Exclui a ocorrência informada e as seguintes da mesma recorrência
	 * (as anteriores, já lançadas, são mantidas). Retorna quantas foram excluídas.
	 */
	@Transactional
	public int excluirRecorrenciaAPartirDe(UUID id) {

		Despesa despesa = despesaRepo.findById(id).orElseThrow(() -> new RuntimeException("Despesa não encontrada"));

		if (!despesa.isParteDeRecorrencia()) {
			excluir(despesa);
			return 1;
		}

		List<Despesa> seguintes = despesaRepo.findByGrupoRecorrenciaAndOcorrenciaGreaterThanEqualOrderByOcorrenciaAsc(
				despesa.getGrupoRecorrencia(), despesa.getOcorrencia());

		for (Despesa d : seguintes) {
			excluir(d);
		}

		return seguintes.size();
	}

	private void excluir(Despesa despesa) {

		Set<FaturaCartao> faturasAfetadas = new LinkedHashSet<>();

		for (Parcela parcela : parcelaRepo.findByDespesa(despesa)) {

			if (parcela.getFatura() != null) {
				faturasAfetadas.add(parcela.getFatura());
			}

			parcelaRepo.delete(parcela);
		}

		// Recalcula o total das faturas a partir das parcelas restantes
		for (FaturaCartao fatura : faturasAfetadas) {
			faturaService.sincronizar(fatura);
		}

		despesaRepo.delete(despesa);
	}

	// =========================================================
	// GERAR PARCELAS
	// =========================================================

	private void gerarParcelas(Despesa despesa) {

		Integer qtdParcelas = despesa.getQtdParcelas();

		BigDecimal valorParcela = despesa.getValorTotal().divide(BigDecimal.valueOf(qtdParcelas), 2, RoundingMode.DOWN);

		BigDecimal valorRestante = despesa.getValorTotal();

		// A primeira parcela segue a regra de fechamento; as demais vão para os meses seguintes.
		// (Somar meses à data da compra coloca duas parcelas na mesma fatura no fim do mês.)
		YearMonth primeiraReferencia = faturaService.calcularReferencia(despesa.getCartao(), despesa.getDataCompra());

		for (int i = 1; i <= qtdParcelas; i++) {

			FaturaCartao fatura = faturaService.getOuCriarFatura(despesa.getCartao(),
					primeiraReferencia.plusMonths(i - 1));

			BigDecimal valorAtual;

			if (i == qtdParcelas) {

				valorAtual = valorRestante;

			} else {

				valorAtual = valorParcela;

				valorRestante = valorRestante.subtract(valorAtual);
			}

			Parcela parcela = new Parcela();

			parcela.setQtdParcela(i);

			parcela.setValorParcela(valorAtual);

			parcela.setDataVencimento(fatura.getDataVencimento());

			parcela.setStatusParcela(StatusParcela.PENDENTE);

			parcela.setDespesa(despesa);

			parcela.setFatura(fatura);

			parcelaRepo.save(parcela);

			faturaService.sincronizar(fatura);
		}
	}
}
