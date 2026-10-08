package com.gabriel.financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.entities.Parcela;
import com.gabriel.financeiro.enums.StatusParcela;
import com.gabriel.financeiro.repository.ParcelaRepository;


@Service
public class ParcelaService {

	private final ParcelaRepository parcelaRepo;
	private final FaturaCartaoService faturaService;
	private final CicloFinanceiroService cicloService;

	public ParcelaService(ParcelaRepository parcelaRepo, FaturaCartaoService faturaService,
			CicloFinanceiroService cicloService) {
		this.parcelaRepo = parcelaRepo;
		this.faturaService = faturaService;
		this.cicloService = cicloService;
	}

	// =========================================================
	// PARCELAS DO CICLO
	// O ciclo aponta para as faturas que fecham dentro dele (ver FaturasDoCiclo).
	// Em todos os métodos, ciclo null = todas as parcelas.
	// =========================================================

	public Page<Parcela> listarParcelasDoCiclo(CicloFinanceiro ciclo, Pageable pageable) {

		if (ciclo == null) {
			return parcelaRepo.findAll(pageable);
		}

		return parcelaRepo.listarDoCiclo(cicloService.faturasDoCiclo(ciclo), pageable);
	}

	public BigDecimal calcularTotalPendente(CicloFinanceiro ciclo) {
	    return somarPorStatus(StatusParcela.PENDENTE, ciclo);
	}

	public BigDecimal calcularTotalPago(CicloFinanceiro ciclo) {
	    return somarPorStatus(StatusParcela.PAGA, ciclo);
	}

	public BigDecimal calcularPercentualPago(CicloFinanceiro ciclo) {

	    BigDecimal total;

	    if (ciclo == null) {
	        total = parcelaRepo.somarTodas();
	    } else {
	        total = parcelaRepo.somarDoCiclo(cicloService.faturasDoCiclo(ciclo));
	    }

	    if (total.compareTo(BigDecimal.ZERO) == 0) {
	        return BigDecimal.ZERO;
	    }

	    return calcularTotalPago(ciclo)
	            .divide(total, 4, RoundingMode.HALF_UP)
	            .multiply(BigDecimal.valueOf(100));
	}

	private BigDecimal somarPorStatus(StatusParcela status, CicloFinanceiro ciclo) {

		if (ciclo == null) {
			return parcelaRepo.somarPorStatus(status);
		}

		return parcelaRepo.somarPorStatusDoCiclo(status, cicloService.faturasDoCiclo(ciclo));
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
