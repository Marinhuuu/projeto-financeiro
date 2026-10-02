package com.gabriel.financeiro.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gabriel.financeiro.entities.FormaPagamento;
import com.gabriel.financeiro.repository.FormaPagamentoRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

@Service
public class FormaPagamentoService {

	private final FormaPagamentoRepository formaPagamentoRepo;

	public FormaPagamentoService(FormaPagamentoRepository formaPagamentoRepo) {
		this.formaPagamentoRepo = formaPagamentoRepo;
	}

	@PostConstruct
	@Transactional
	public void inicializarFormasPadrao() {
		if (formaPagamentoRepo.count() == 0) {
			formaPagamentoRepo.save(new FormaPagamento(null, "Cartão de Crédito", "CARTAO_CREDITO", true));
			formaPagamentoRepo.save(new FormaPagamento(null, "PIX", "PIX", false));
			formaPagamentoRepo.save(new FormaPagamento(null, "Dinheiro", "DINHEIRO", false));
			formaPagamentoRepo.save(new FormaPagamento(null, "Cartão de Débito", "DEBITO", false));
		}
	}

	public List<FormaPagamento> listarTodas() {
		return formaPagamentoRepo.findAll();
	}

	public FormaPagamento buscarPorId(UUID id) {
		return formaPagamentoRepo.findById(id)
				.orElseThrow(() -> new RuntimeException("Forma de pagamento não encontrada"));
	}

	public FormaPagamento buscarPorCodigo(String codigo) {
		return formaPagamentoRepo.findByCodigo(codigo).orElse(null);
	}

	public FormaPagamento salvar(FormaPagamento formaPagamento) {
		return formaPagamentoRepo.save(formaPagamento);
	}
}
