package com.gabriel.financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.entities.Categoria;
import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.entities.Despesa;
import com.gabriel.financeiro.entities.FaturaCartao;
import com.gabriel.financeiro.entities.Parcela;
import com.gabriel.financeiro.entities.Pessoa;
import com.gabriel.financeiro.enums.StatusParcela;
import com.gabriel.financeiro.repository.CartaoRepository;
import com.gabriel.financeiro.repository.CategoriaRepository;
import com.gabriel.financeiro.repository.DespesaRepository;
import com.gabriel.financeiro.repository.ParcelaRepository;
import com.gabriel.financeiro.repository.PessoaRepository;

import jakarta.transaction.Transactional;

@Service
public class DespesaService {

	private final DespesaRepository despesaRepo;
	private final CicloFinanceiroService cicloService;
	private final ParcelaRepository parcelaRepo;
	private final FaturaCartaoService faturaService;
	private final CartaoRepository cartaoRepo;
	private final PessoaRepository pessoaRepo;
	private final CategoriaRepository categoriaRepo;



	public DespesaService(DespesaRepository despesaRepo, CicloFinanceiroService cicloService,
			ParcelaRepository parcelaRepo, FaturaCartaoService faturaService, CartaoRepository cartaoRepo,
			PessoaRepository pessoaRepo, CategoriaRepository categoriaRepo) {
		this.despesaRepo = despesaRepo;
		this.cicloService = cicloService;
		this.parcelaRepo = parcelaRepo;
		this.faturaService = faturaService;
		this.cartaoRepo = cartaoRepo;
		this.pessoaRepo = pessoaRepo;
		this.categoriaRepo = categoriaRepo;
	}

	public Page<Despesa> ListDespesa(Pageable pageable) {
		return despesaRepo.findAll(pageable);
	}

	// =========================================================
	// CADASTRAR DESPESA
	// =========================================================

	public Despesa insertDespesa(Despesa despesa) {

		LocalDate dataCompra = despesa.getDataCompra();

		CicloFinanceiro ciclo = cicloService.getOuCriarCiclo(dataCompra);

		despesa.setCiclo(ciclo);

		// Se não informou pessoa,
		// consideramos que a despesa é do próprio Gabriel.
		//
		// Não precisamos fazer nada aqui.
		// Pessoa continua null.

		if ("CARTAO".equals(despesa.getFormaPagamento())) {

			if (despesa.getCartao() == null || despesa.getCartao().getId() == null) {

				throw new RuntimeException("É necessário selecionar um cartão de crédito");
			}

			CartaoCredito cartao = cartaoRepo.findById(despesa.getCartao().getId())
					.orElseThrow(() -> new RuntimeException("Cartão não encontrado"));

			despesa.setCartao(cartao);

			if (despesa.getQtdParcelas() == null || despesa.getQtdParcelas() < 1) {

				despesa.setQtdParcelas(1);
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

		despesa.setCategoria(categoria);

		Despesa despesaSalva = despesaRepo.save(despesa);

		if ("CARTAO".equals(despesaSalva.getFormaPagamento())) {

			gerarParcelas(despesaSalva);
		}

		return despesaSalva;
	}

	// =========================================================
	// LISTAR DESPESAS DE TERCEIROS
	// =========================================================

	public List<Despesa> listarDespesasDeTerceiros() {

		return despesaRepo.listarDespesasDeTerceiros();
	}

	// =========================================================
	// MARCAR COMO DEVOLVIDO
	// =========================================================

	public void marcarComoDevolvido(UUID id) {

		Despesa despesa = despesaRepo.findById(id).orElseThrow(() -> new RuntimeException("Despesa não encontrada"));

		despesa.setDevolvido(true);

		despesaRepo.save(despesa);
	}

	// =========================================================
	// MARCAR COMO NÃO DEVOLVIDO
	// =========================================================

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

		Despesa despesa = despesaRepo.findById(id).orElseThrow(() -> new RuntimeException("Despesa não encontrada"));

		List<Parcela> parcelas = parcelaRepo.findByDespesa(despesa);

		for (Parcela parcela : parcelas) {

			FaturaCartao fatura = parcela.getFatura();

			if (fatura != null) {

				BigDecimal novoValor = fatura.getValorTotal().subtract(parcela.getValorParcela());

				if (novoValor.compareTo(BigDecimal.ZERO) < 0) {

					novoValor = BigDecimal.ZERO;
				}

				fatura.setValorTotal(novoValor);

				faturaService.salvar(fatura);
			}

			parcelaRepo.delete(parcela);
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

		for (int i = 1; i <= qtdParcelas; i++) {

			FaturaCartao fatura = faturaService.getOuCriarFatura(despesa.getCartao(),
					despesa.getDataCompra().plusMonths(i - 1));

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

			fatura.setValorTotal(fatura.getValorTotal().add(valorAtual));

			faturaService.salvar(fatura);

			parcelaRepo.save(parcela);
		}
	}
}