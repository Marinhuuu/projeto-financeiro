package com.gabriel.financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gabriel.financeiro.entities.Parcela;
import com.gabriel.financeiro.enums.StatusParcela;
import com.gabriel.financeiro.repository.ParcelaRepository;


@Service
public class ParcelaService {

	private ParcelaRepository parcelaRepo;

	public ParcelaService(ParcelaRepository parcelaRepo) {
		this.parcelaRepo = parcelaRepo;
	}

	public Page<Parcela> ListParcela(Pageable pageable) {
		return parcelaRepo.findAll(pageable);
	}

	public BigDecimal calcularTotalPendente() {
	    return parcelaRepo.findAll().stream()
	            .filter(p -> p.getStatusParcela() == StatusParcela.PENDENTE)
	            .map(Parcela::getValorParcela)
	            .reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal calcularTotalPago() {
	    return parcelaRepo.findAll().stream()
	            .filter(p -> p.getStatusParcela() == StatusParcela.PAGA)
	            .map(Parcela::getValorParcela)
	            .reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal calcularPercentualPago() {

	    BigDecimal total = parcelaRepo.findAll().stream()
	            .map(Parcela::getValorParcela)
	            .reduce(BigDecimal.ZERO, BigDecimal::add);

	    BigDecimal totalPago = calcularTotalPago();

	    if (total.compareTo(BigDecimal.ZERO) == 0) {
	        return BigDecimal.ZERO;
	    }

	    return totalPago
	            .divide(total, 4, RoundingMode.HALF_UP)
	            .multiply(BigDecimal.valueOf(100));
	}
	
	public Parcela pagarParcela(UUID id) {

	    Parcela parcela = parcelaRepo.findById(id)
	            .orElseThrow(() -> new RuntimeException("Parcela não encontrada"));

	    parcela.setStatusParcela(StatusParcela.PAGA);

	    return parcelaRepo.save(parcela);
	}

	public Parcela desfazerPagamento(UUID id) {

	    Parcela parcela = parcelaRepo.findById(id)
	            .orElseThrow(() -> new RuntimeException("Parcela não encontrada"));

	    if (parcela.getDataVencimento() != null && parcela.getDataVencimento().isBefore(LocalDate.now())) {
	        parcela.setStatusParcela(StatusParcela.ATRASADA);
	    } else {
	        parcela.setStatusParcela(StatusParcela.PENDENTE);
	    }

	    return parcelaRepo.save(parcela);
	}

	public Parcela buscarPorId(UUID id) {
	    return parcelaRepo.findById(id)
	            .orElseThrow(() -> new RuntimeException("Parcela não encontrada"));
	}
	
	public void atualizarParcelasAtrasadas() {

	    LocalDate hoje = LocalDate.now();

	    for (Parcela parcela : parcelaRepo.findAll()) {

	        if (parcela.getStatusParcela() == StatusParcela.PENDENTE
	                && parcela.getDataVencimento().isBefore(hoje)) {

	            parcela.setStatusParcela(StatusParcela.ATRASADA);

	            parcelaRepo.save(parcela);
	        }
	    }
	}
}
