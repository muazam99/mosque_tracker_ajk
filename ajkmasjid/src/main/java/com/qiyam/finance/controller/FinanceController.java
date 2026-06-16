package com.qiyam.finance.controller;

import com.qiyam.finance.dto.FinanceAccountRequest;
import com.qiyam.finance.dto.FinanceReportRequest;
import com.qiyam.finance.dto.FinanceTransactionRequest;
import com.qiyam.finance.service.FinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
@Tag(name = "Finance", description = "Manage finances including accounts, transactions, and audit reports")
public class FinanceController {
    private final FinanceService financeService;

    // ─── Accounts ─────────────────────────────────────────────

    @GetMapping("/accounts")
    @Operation(summary = "Get all accounts", description = "Returns a paginated list of all financial accounts")
    public ResponseEntity<List<Map<String, Object>>> getAllAccounts(
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Number of records to skip")
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(financeService.getAllAccounts(limit, offset));
    }

    @GetMapping("/accounts/{id}")
    @Operation(summary = "Get account by ID", description = "Returns a single financial account by its ID")
    public ResponseEntity<Map<String, Object>> getAccountById(@Parameter(description = "Account ID") @PathVariable Long id) {
        return financeService.getAccountById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/accounts")
    @Operation(summary = "Create an account", description = "Creates a new financial account")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody FinanceAccountRequest request) {
        var created = financeService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/accounts/{id}")
    @Operation(summary = "Update an account", description = "Updates an existing financial account by ID")
    public ResponseEntity<Map<String, Object>> updateAccount(
            @Parameter(description = "Account ID") @PathVariable Long id, @RequestBody FinanceAccountRequest request) {
        var updated = financeService.updateAccount(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/accounts/{id}")
    @Operation(summary = "Delete an account", description = "Deletes a financial account by ID")
    public ResponseEntity<Void> deleteAccount(@Parameter(description = "Account ID") @PathVariable Long id) {
        financeService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Transactions ─────────────────────────────────────────

    @GetMapping("/transactions")
    @Operation(summary = "Get all transactions", description = "Returns a paginated list of all financial transactions")
    public ResponseEntity<List<Map<String, Object>>> getAllTransactions(
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Number of records to skip")
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(financeService.getAllTransactions(limit, offset));
    }

    @GetMapping("/transactions/{id}")
    @Operation(summary = "Get transaction by ID", description = "Returns a single financial transaction by its ID")
    public ResponseEntity<Map<String, Object>> getTransactionById(@Parameter(description = "Transaction ID") @PathVariable Long id) {
        return financeService.getTransactionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/transactions")
    @Operation(summary = "Create a transaction", description = "Creates a new financial transaction")
    public ResponseEntity<Map<String, Object>> createTransaction(@RequestBody FinanceTransactionRequest request) {
        var created = financeService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── Reports (finance_audits) ────────────────────────────

    @GetMapping("/reports")
    @Operation(summary = "Get all reports", description = "Returns a paginated list of all financial audit reports")
    public ResponseEntity<List<Map<String, Object>>> getAllReports(
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Number of records to skip")
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(financeService.getAllReports(limit, offset));
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Get report by ID", description = "Returns a single audit report by its ID")
    public ResponseEntity<Map<String, Object>> getReportById(@Parameter(description = "Report ID") @PathVariable Long id) {
        return financeService.getReportById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reports")
    @Operation(summary = "Create a report", description = "Creates a new financial audit report")
    public ResponseEntity<Map<String, Object>> createReport(@RequestBody FinanceReportRequest request) {
        var created = financeService.createReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
