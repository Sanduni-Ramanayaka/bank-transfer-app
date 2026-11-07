
package com.example.bank_transfer_app.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public void run(String... args) {

        // Insert sample customers if the database is empty
        if (customerRepository.count() == 0) {

            Customer kamal = new Customer(
                    "100001",
                    "Kamal",
                    "kamal@gmail.com",
                    new BigDecimal("50000.00")
            );

            Customer nimali = new Customer(
                    "100002",
                    "Nimali",
                    "nimali@gmail.com",
                    new BigDecimal("25000.00")
            );

            Customer saman = new Customer(
                    "100003",
                    "Saman",
                    "saman@gmail.com",
                    new BigDecimal("10000.00")
            );

            customerRepository.save(kamal);
            customerRepository.save(nimali);
            customerRepository.save(saman);

            System.out.println(
                    "Sample customers inserted successfully!"
            );
        }

        // Create bank fee account if it does not exist
        if (customerRepository
                .getByAccountNumber("999999")
                .isEmpty()) {

            Customer bankAccount = new Customer(
                    "999999",
                    "Bank Fee Account",
                    "bankfees@bank.local",
                    BigDecimal.ZERO
            );

            customerRepository.save(bankAccount);

            System.out.println(
                    "Bank fee account created successfully!"
            );
        }
    }
}
