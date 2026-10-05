package com.gabriel.financeiro.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gabriel.financeiro.entities.FormaPagamento;
import com.gabriel.financeiro.repository.FormaPagamentoRepository;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormaPagamentoService {

	private final FormaPagamentoRepository formaPagamentoRepo;

	public FormaPagamentoService(FormaPagamentoRepository formaPagamentoRepo) {
		this.formaPagamentoRepo = formaPagamentoRepo;
	}

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void inicializarFormasPadrao() {
		garantirFormaPagamento("Cartão de Crédito",   "CARTAO_CREDITO", true);
		garantirFormaPagamento("Crédito à Vista",      "CARTAO_VISTA",   true);
		garantirFormaPagamento("PIX",                  "PIX",            false);
		garantirFormaPagamento("Dinheiro",             "DINHEIRO",       false);
		garantirFormaPagamento("Cartão de Débito",     "DEBITO",         false);
	}

	private void garantirFormaPagamento(String nome, String codigo, boolean permiteParcelamento) {
		if (formaPagamentoRepo.findByCodigo(codigo).isEmpty()) {
			formaPagamentoRepo.save(new FormaPagamento(null, nome, codigo, permiteParcelamento));
		}
	}

	// Ordem de exibição nos formulários; códigos fora da lista vão para o fim
	private static final List<String> ORDEM = List.of("CARTAO_CREDITO", "CARTAO_VISTA", "DEBITO", "PIX", "DINHEIRO");

	public List<FormaPagamento> listarTodas() {
		return formaPagamentoRepo.findAll().stream()
				.sorted(Comparator.comparingInt((FormaPagamento f) -> {
					int posicao = ORDEM.indexOf(f.getCodigo());
					return posicao < 0 ? ORDEM.size() : posicao;
				}).thenComparing(FormaPagamento::getNome))
				.toList();
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
