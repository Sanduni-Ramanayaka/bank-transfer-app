package com.example.bank_transfer_app.controller;

import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bank_transfer_app.dto.TransferRequest;
import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.service.BankService;
import com.example.bank_transfer_app.service.TransactionAuditService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BankController {


    private final BankService bankService;

    private final TransactionAuditService auditService;


    // =========================================
    // DASHBOARD
    // =========================================

    @GetMapping("/")
    public String dashboard(
            @RequestParam(required = false) String keyword,
            Model model) {

        // Customer list or search results
        model.addAttribute(
                "customers",
                bankService.searchCustomers(keyword)
        );

        // Latest transactions
        model.addAttribute(
                "transactions",
                bankService.getLatestTransactions()
        );

        // Dashboard statistics
        model.addAttribute(
                "totalBalance",
                bankService.getTotalBalance()
        );

        model.addAttribute(
                "successfulCount",
                bankService.getSuccessfulCount()
        );

        model.addAttribute(
                "failedCount",
                bankService.getFailedCount()
        );

        // Transfer form object
        model.addAttribute(
                "transferRequest",
                new TransferRequest()
        );

        model.addAttribute("keyword", keyword);

        return "bank";
    }


    // =========================================
    // ADD CUSTOMER
    // =========================================

    @PostMapping("/customers")
    public String addCustomer(

            @RequestParam String accountNumber,

            @RequestParam String name,

            @RequestParam String email,

            @RequestParam BigDecimal openingBalance,

            RedirectAttributes redirectAttributes) {

        try {

            Customer customer = bankService.createCustomer(
                    accountNumber,
                    name,
                    email,
                    openingBalance
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Customer " + customer.getName()
                            + " was created successfully."
            );

        } catch (RuntimeException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

        }

        return "redirect:/";
    }


    // =========================================
    // MONEY TRANSFER
    // =========================================

    @PostMapping("/transfer")
    public String transfer(

            @Valid
            @ModelAttribute("transferRequest")
            TransferRequest request,

            BindingResult bindingResult,

            RedirectAttributes redirectAttributes) {


        // Check form validation first
        if (bindingResult.hasErrors()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    bindingResult.getFieldErrors()
                            .get(0)
                            .getDefaultMessage()
            );

            return "redirect:/";
        }


        try {

            // Main transactional operation
            bankService.transfer(request);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Money transferred successfully."
            );

        } catch (RuntimeException exception) {

            /*
             * The BankService transaction has already
             * rolled back before this catch block executes.
             */

            // Save the failure independently
            if (request.getAmount() != null
                    && request.getAmount().compareTo(BigDecimal.ZERO) > 0) {

                auditService.recordFailure(
                        request.getSenderAccount(),
                        request.getReceiverAccount(),
                        request.getAmount(),
                        exception.getMessage()
                );

            }

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

        }

        return "redirect:/";
    }


    // =========================================
    // ACCOUNT TRANSACTION HISTORY
    // =========================================

    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(

            @PathVariable String accountNumber,

            Model model) {


        Customer customer = bankService.getCustomer(accountNumber);

        model.addAttribute(
                "customer",
                customer
        );

        model.addAttribute(
                "history",
                bankService.getAccountHistory(accountNumber)
        );

        return "history";
    }

}
