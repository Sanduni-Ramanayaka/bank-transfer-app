
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

    // DASHBOARD
    @GetMapping("/")
    public String dashboard(
            @RequestParam(required = false) String keyword,
            Model model) {

        model.addAttribute("customers",
                bankService.searchCustomers(keyword));

        model.addAttribute("transactions",
                bankService.getLatestTransactions());

        model.addAttribute("totalBalance",
                bankService.getTotalBalance());

        model.addAttribute("successfulCount",
                bankService.getSuccessfulCount());

        model.addAttribute("failedCount",
                bankService.getFailedCount());

        model.addAttribute("transferRequest",
                new TransferRequest());

        model.addAttribute("keyword", keyword);

        return "bank";
    }

    // CREATE CUSTOMER
    @PostMapping("/customers")
    public String createCustomer(
            @RequestParam String accountNumber,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam BigDecimal openingBalance,
            RedirectAttributes redirectAttributes) {

        try {
            bankService.createCustomer(
                    accountNumber,
                    name,
                    email,
                    openingBalance);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Customer created successfully.");

        } catch (RuntimeException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/";
    }

    // MONEY TRANSFER
    @PostMapping("/transfer")
    public String transfer(
            @Valid @ModelAttribute TransferRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please enter valid transfer details.");

            return "redirect:/";
        }

        try {
            bankService.transfer(request);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Money transferred successfully.");

        } catch (RuntimeException exception) {

            // Record failed transfer separately
            if (request.getAmount() != null
                    && request.getAmount().compareTo(BigDecimal.ZERO) > 0) {

                auditService.recordFailure(
                        request.getSenderAccount(),
                        request.getReceiverAccount(),
                        request.getAmount(),
                        exception.getMessage());
            }

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/";
    }

    // ACCOUNT TRANSACTION HISTORY
    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(
            @PathVariable String accountNumber,
            Model model) {

        Customer customer =
                bankService.getCustomer(accountNumber);

        model.addAttribute("customer", customer);

        model.addAttribute("history",
                bankService.getAccountHistory(accountNumber));

        model.addAttribute("totalSent",
                bankService.getTotalSentByAccount(accountNumber));

        return "history";
    }

    // LARGE TRANSFERS
    @GetMapping("/large-transfers")
    public String largeTransfers(
            @RequestParam(defaultValue = "10000") BigDecimal min,
            Model model) {

        if (min.compareTo(BigDecimal.ZERO) < 0) {
            min = BigDecimal.ZERO;
        }

        model.addAttribute("transfers",
                bankService.getLargeTransfers(min));

        model.addAttribute("min", min);

        return "large-transfers";
    }

    // DEPOSIT MONEY
    @PostMapping("/deposit")
    public String deposit(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        try {
            bankService.deposit(accountNumber, amount);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Deposit completed successfully.");

        } catch (RuntimeException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/";
    }

    // WITHDRAW MONEY
    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        try {
            bankService.withdraw(accountNumber, amount);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Withdrawal completed successfully.");

        } catch (RuntimeException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/";
    }

    @GetMapping("/deposit")
public String showDepositPage(Model model) {
    model.addAttribute("customers", bankService.searchCustomers(null));
    return "deposit";
}

@GetMapping("/withdraw")
public String showWithdrawPage(Model model) {
    model.addAttribute("customers", bankService.searchCustomers(null));
    return "withdraw";
}
}
