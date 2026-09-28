package com.exemplo.fornecedoresservice.config;

import com.exemplo.fornecedoresservice.model.Fornecedor;
import com.exemplo.fornecedoresservice.repository.FornecedorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final FornecedorRepository repository;

    public DataInitializer(FornecedorRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            repository.saveAll(List.of(
                    new Fornecedor("Alfa Tecnologia", "11.111.111/0001-91"),
                    new Fornecedor("Beta Distribuidora", "22.222.222/0001-91"),
                    new Fornecedor("Gama Informatica", "33.333.333/0001-91"),
                    new Fornecedor("Delta Equipamentos", "44.444.444/0001-91"),
                    new Fornecedor("Omega Suprimentos", "55.555.555/0001-91")
            ));
        }
    }
}
