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
                    new Fornecedor("Alfa Tecnologia", "11222333000181"),
                    new Fornecedor("Beta Distribuidora", "11444777000161"),
                    new Fornecedor("Gama Informatica", "12345678000195"),
                    new Fornecedor("Delta Equipamentos", "45723174000110"),
                    new Fornecedor("Omega Suprimentos", "60746948000112")
            ));
        }
    }
}
