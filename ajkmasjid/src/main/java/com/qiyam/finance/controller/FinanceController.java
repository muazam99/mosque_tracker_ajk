package com.qiyam.finance.controller;

import com.qiyam.finance.dto.FinanceAccountRequest;
import com.qiyam.finance.dto.FinanceReportRequest;
import com.qiyam.finance.dto.FinanceTransactionRequest;
import com.qiyam.finance.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
public class FinanceController {
    private final FinanceService financeService;

    // ─── Accounts ─────────────────────────────────────────────

    @GetMapping("/accounts")
    public ResponseEntity<List<Map<String, Object>>> getAllAccounts(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(financeService.getAllAccounts(limit, offset));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<Map<String, Object>> getAccountById(@PathVariable Long id) {
        return financeService.getAccountById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/accounts")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody FinanceAccountRequest request) {
        var created = financeService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<Map<String, Object>> updateAccount(
            @PathVariable Long id, @RequestBody FinanceAccountRequest request) {
        var updated = financeService.updateAccount(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        financeService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Transactions ─────────────────────────────────────────

    @GetMapping("/transactions")
    public ResponseEntity<List<Map<String, Object>>> getAllTransactions(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(financeService.getAllTransactions(limit, offset));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<Map<String, Object>> getTransactionById(@PathVariable Long id) {
        return financeService.getTransactionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/transactions")
    public ResponseEntity<Map<String, Object>> createTransaction(@RequestBody FinanceTransactionRequest request) {
        var created = financeService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── Reports (finance_audits) ────────────────────────────

    @GetMapping("/reports")
    public ResponseEntity<List<Map<String, Object>>> getAllReports(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(financeService.getAllReports(limit, offset));
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<Map<String, Object>> getReportById(@PathVariable Long id) {
        return financeService.getReportById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reports")
    public ResponseEntity<Map<String, Object>> createReport(@RequestBody FinanceReportRequest request) {
        var created = financeService.createReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
