package com.gabriel.financeiro.service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.repository.CartaoRepository;
import com.gabriel.financeiro.repository.DespesaRepository;
import com.gabriel.financeiro.repository.FaturaCartaoRepository;

@Service
public class CartaoService {

	private final CartaoRepository cartaoRepo;
	private final DespesaRepository despesaRepo;
	private final FaturaCartaoRepository faturaRepo;
	private final FaturaCartaoService faturaService;

	public CartaoService(CartaoRepository cartaoRepo, DespesaRepository despesaRepo,
			FaturaCartaoRepository faturaRepo, FaturaCartaoService faturaService) {
		this.cartaoRepo = cartaoRepo;
		this.despesaRepo = despesaRepo;
		this.faturaRepo = faturaRepo;
		this.faturaService = faturaService;
	}

	public Page<CartaoCredito> ListCartao(Pageable pageable){

		return cartaoRepo.findAll(pageable);

	}

	public CartaoCredito getById(UUID id) {

		return cartaoRepo.findById(id).orElseThrow(() -> new RuntimeException("Cartao não encontrado"));

	}

	public CartaoCredito insertCartao(CartaoCredito cartao) {

		validar(cartao);

		return cartaoRepo.save(cartao);
	}

	@Transactional
	public CartaoCredito updateCartao(UUID id, CartaoCredito cartao) {

		validar(cartao);

		CartaoCredito Cartao = cartaoRepo.findById(id).orElseThrow(() -> new RuntimeException("Cartao não encontrado"));

		boolean mudouDatas = !Objects.equals(Cartao.getDiaFechamento(), cartao.getDiaFechamento())
				|| !Objects.equals(Cartao.getDiaVencimento(), cartao.getDiaVencimento());

		Cartao.setNome(cartao.getNome());
		Cartao.setLimite(cartao.getLimite());
		Cartao.setDiaVencimento(cartao.getDiaVencimento());
		Cartao.setDiaFechamento(cartao.getDiaFechamento());

		CartaoCredito salvo = cartaoRepo.save(Cartao);

		// Faturas e parcelas existentes passam a seguir os novos dias
		if (mudouDatas) {
			faturaService.recalcularFaturasDoCartao(salvo);
		}

		return salvo;

	}

	@Transactional
	public void deleteCartao(UUID id) {

		CartaoCredito cartao = getById(id);

		if (despesaRepo.existsByCartao(cartao)) {
			throw new RuntimeException("Não é possível excluir: o cartão possui despesas vinculadas");
		}

		// Sem despesas, as faturas que sobraram estão vazias
		faturaRepo.deleteAll(faturaRepo.findByCartao(cartao));

		cartaoRepo.delete(cartao);
	}

	private void validar(CartaoCredito cartao) {

		if (cartao.getNome() == null || cartao.getNome().isBlank()) {
			throw new RuntimeException("Informe o nome do cartão");
		}

		if (cartao.getLimite() != null && cartao.getLimite().compareTo(BigDecimal.ZERO) < 0) {
			throw new RuntimeException("O limite não pode ser negativo");
		}

		if (!diaValido(cartao.getDiaFechamento()) || !diaValido(cartao.getDiaVencimento())) {
			throw new RuntimeException("Os dias de fechamento e vencimento devem estar entre 1 e 31");
		}
	}

	private boolean diaValido(Integer dia) {
		return dia != null && dia >= 1 && dia <= 31;
	}

}
