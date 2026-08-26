package com.gabriel.financeiro.service;

import org.springframework.stereotype.Service;

import com.gabriel.financeiro.entities.ConfiguracaoFinanceira;
import com.gabriel.financeiro.repository.ConfiguracaoFinanceiraRepository;

@Service
public class ConfiguracaoFinanceiraService {

    private final ConfiguracaoFinanceiraRepository configRepo;

    public ConfiguracaoFinanceiraService(
            ConfiguracaoFinanceiraRepository configRepo) {
        this.configRepo = configRepo;
    }

    public ConfiguracaoFinanceira getConfiguracao() {

        return configRepo.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> criarConfiguracaoPadrao());
    }

    private ConfiguracaoFinanceira criarConfiguracaoPadrao() {

        ConfiguracaoFinanceira config = new ConfiguracaoFinanceira();

        config.setDiaFechamento(25);

        return configRepo.save(config);
    }

    public ConfiguracaoFinanceira salvar(
            ConfiguracaoFinanceira config) {

        if (config.getDiaFechamento() == null ||
            config.getDiaFechamento() < 1 ||
            config.getDiaFechamento() > 31) {

            throw new IllegalArgumentException(
                    "O dia de fechamento deve estar entre 1 e 31."
            );
        }

        return configRepo.save(config);
    }
}