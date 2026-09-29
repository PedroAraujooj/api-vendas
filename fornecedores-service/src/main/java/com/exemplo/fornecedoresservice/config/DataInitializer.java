package com.exemplo.fornecedoresservice.config;

import com.exemplo.fornecedoresservice.model.Fornecedor;
import com.exemplo.fornecedoresservice.repository.FornecedorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    private final FornecedorRepository fornecedorRepository;

    public DataInitializer(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    @Override
    public void run(String... args) {
        fornecedorRepository.saveAll(List.of(
                new Fornecedor("Alfa Tecnologia", "11222333000181"),
                new Fornecedor("Beta Distribuidora", "22333444000181"),
                new Fornecedor("Gama Informatica", "33444555000181"),
                new Fornecedor("Delta Suprimentos", "44555666000181"),
                new Fornecedor("Epsilon Equipamentos", "55666777000181")
        ));
    }
}
