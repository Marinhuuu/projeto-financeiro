package com.gabriel.financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gabriel.financeiro.entities.Parcela;
import com.gabriel.financeiro.enums.StatusParcela;
import com.gabriel.financeiro.repository.ParcelaRepository;


@Service
public class ParcelaService {

	private final ParcelaRepository parcelaRepo;
	private final FaturaCartaoService faturaService;

	public ParcelaService(ParcelaRepository parcelaRepo, FaturaCartaoService faturaService) {
		this.parcelaRepo = parcelaRepo;
		this.faturaService = faturaService;
	}

	public Page<Parcela> ListParcela(Pageable pageable) {
		return parcelaRepo.findAll(pageable);
	}

	public BigDecimal calcularTotalPendente() {
	    return parcelaRepo.somarPorStatus(StatusParcela.PENDENTE);
	}

	public BigDecimal calcularTotalPago() {
	    return parcelaRepo.somarPorStatus(StatusParcela.PAGA);
	}

	public BigDecimal calcularPercentualPago() {

	    BigDecimal total = parcelaRepo.somarTodas();

	    if (total.compareTo(BigDecimal.ZERO) == 0) {
	        return BigDecimal.ZERO;
	    }

	    return calcularTotalPago()
	            .divide(total, 4, RoundingMode.HALF_UP)
	            .multiply(BigDecimal.valueOf(100));
	}

	@Transactional
	public Parcela pagarParcela(UUID id) {

	    Parcela parcela = buscarPorId(id);

	    parcela.setStatusParcela(StatusParcela.PAGA);

	    Parcela salva = parcelaRepo.save(parcela);

	    if (salva.getFatura() != null) {
	        faturaService.sincronizar(salva.getFatura());
	    }

	    return salva;
	}

	@Transactional
	public Parcela desfazerPagamento(UUID id) {

	    Parcela parcela = buscarPorId(id);

	    if (parcela.getDataVencimento() != null && parcela.getDataVencimento().isBefore(LocalDate.now())) {
	        parcela.setStatusParcela(StatusParcela.ATRASADA);
	    } else {
	        parcela.setStatusParcela(StatusParcela.PENDENTE);
	    }

	    Parcela salva = parcelaRepo.save(parcela);

	    if (salva.getFatura() != null) {
	        faturaService.sincronizar(salva.getFatura());
	    }

	    return salva;
	}

	public Parcela buscarPorId(UUID id) {
	    return parcelaRepo.findById(id)
	            .orElseThrow(() -> new RuntimeException("Parcela não encontrada"));
	}

	@Transactional
	public void atualizarParcelasAtrasadas() {

	    parcelaRepo.marcarAtrasadas(StatusParcela.PENDENTE, StatusParcela.ATRASADA, LocalDate.now());
	}
}
