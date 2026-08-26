package com.gabriel.financeiro.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gabriel.financeiro.entities.Pessoa;
import com.gabriel.financeiro.repository.PessoaRepository;

@Service
public class PessoaService {

    private final PessoaRepository pessoaRepo;

    public PessoaService(PessoaRepository pessoaRepo) {
        this.pessoaRepo = pessoaRepo;
    }

    public Page<Pessoa> listPessoa(Pageable pageable) {

        return pessoaRepo.findAll(pageable);

    }

    public Pessoa getById(UUID id) {

        return pessoaRepo.findById(id)
                .orElseThrow(() ->
                    new RuntimeException("Pessoa não encontrada")
                );

    }

    public Pessoa insertPessoa(Pessoa pessoa) {

        return pessoaRepo.save(pessoa);

    }

    public Pessoa updatePessoa(UUID id, Pessoa pessoa) {

        Pessoa pessoaExistente =
                pessoaRepo.findById(id)
                        .orElseThrow(() ->
                            new RuntimeException(
                                "Pessoa não encontrada"
                            )
                        );

        pessoaExistente.setNome(
                pessoa.getNome()
        );

        return pessoaRepo.save(
                pessoaExistente
        );

    }

    public void deletePessoa(UUID id) {

        pessoaRepo.deleteById(id);

    }
    }
