package com.qiyam.finance.service;

import com.qiyam.finance.dto.FinanceAccountRequest;
import com.qiyam.finance.dto.FinanceReportRequest;
import com.qiyam.finance.dto.FinanceTransactionRequest;
import com.qiyam.shared.client.SupabaseClient;
import com.qiyam.shared.security.AccessControlService;
import com.qiyam.shared.security.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
@RequiredArgsConstructor
public class FinanceService {
    private final SupabaseClient supabaseClient;
    private final AccessControlService accessControlService;

    // ─── Accounts ─────────────────────────────────────────────

    public List<Map<String, Object>> getAllAccounts(int limit, int offset) {
        accessControlService.requirePermission(null, Permission.FINANCE_READ);
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "created_at.desc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("finance_accounts", params, Map.class);
    }

    public Optional<Map<String, Object>> getAccountById(Long id) {
        accessControlService.requirePermission(null, Permission.FINANCE_READ);
        return (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("finance_accounts", "id", String.valueOf(id), Map.class);
    }

    public Map<String, Object> createAccount(FinanceAccountRequest request) {
        accessControlService.requirePermission(null, Permission.FINANCE_WRITE);
        var body = accountToMap(request);
        var result = supabaseClient.post("finance_accounts", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> updateAccount(Long id, FinanceAccountRequest request) {
        accessControlService.requirePermission(null, Permission.FINANCE_WRITE);
        var body = accountToMap(request);
        var result = supabaseClient.patch("finance_accounts", "id", String.valueOf(id), body, Map.class);
        return result != null ? result : Map.of();
    }

    public void deleteAccount(Long id) {
        accessControlService.requirePermission(null, Permission.FINANCE_DELETE);
        supabaseClient.delete("finance_accounts", "id", String.valueOf(id));
    }

    // ─── Transactions ─────────────────────────────────────────

    public List<Map<String, Object>> getAllTransactions(int limit, int offset) {
        accessControlService.requirePermission(null, Permission.FINANCE_READ);
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "transaction_date.desc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("finance_transactions", params, Map.class);
    }

    public Optional<Map<String, Object>> getTransactionById(Long id) {
        accessControlService.requirePermission(null, Permission.FINANCE_READ);
        return (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("finance_transactions", "id", String.valueOf(id), Map.class);
    }

    public Map<String, Object> createTransaction(FinanceTransactionRequest request) {
        accessControlService.requirePermission(null, Permission.FINANCE_WRITE);
        var body = transactionToMap(request);
        var result = supabaseClient.post("finance_transactions", body, Map.class);
        return result != null ? result : Map.of();
    }

    // ─── Reports (finance_audits) ────────────────────────────

    public List<Map<String, Object>> getAllReports(int limit, int offset) {
        accessControlService.requirePermission(null, Permission.REPORTS_READ);
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "created_at.desc");
        return (List<Map<String, Object>>) (List<?>) supabaseClient.getAll("finance_audits", params, Map.class);
    }

    public Optional<Map<String, Object>> getReportById(Long id) {
        accessControlService.requirePermission(null, Permission.REPORTS_READ);
        return (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("finance_audits", "id", String.valueOf(id), Map.class);
    }

    public Map<String, Object> createReport(FinanceReportRequest request) {
        accessControlService.requirePermission(null, Permission.REPORTS_WRITE);
        var body = reportToMap(request);
        var result = supabaseClient.post("finance_audits", body, Map.class);
        return result != null ? result : Map.of();
    }

    // ─── Mappers ─────────────────────────────────────────────

    private Map<String, Object> accountToMap(FinanceAccountRequest r) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "mosque_id", r.getMosqueId());
        putIfNotNull(map, "account_name", r.getAccountName());
        putIfNotNull(map, "account_type", r.getAccountType());
        putIfNotNull(map, "balance", r.getBalance());
        putIfNotNull(map, "bank_name", r.getBankName());
        putIfNotNull(map, "account_number", r.getAccountNumber());
        putIfNotNull(map, "status", r.getStatus());
        return map;
    }

    private Map<String, Object> transactionToMap(FinanceTransactionRequest r) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "mosque_id", r.getMosqueId());
        putIfNotNull(map, "finance_account_id", r.getFinanceAccountId());
        putIfNotNull(map, "category_id", r.getCategoryId());
        putIfNotNull(map, "transaction_type", r.getTransactionType());
        putIfNotNull(map, "amount", r.getAmount());
        putIfNotNull(map, "reference_no", r.getReferenceNo());
        putIfNotNull(map, "description", r.getDescription());
        putIfNotNull(map, "transaction_date", r.getTransactionDate());
        putIfNotNull(map, "receipt_url", r.getReceiptUrl());
        putIfNotNull(map, "created_by", r.getCreatedBy());
        putIfNotNull(map, "approved_by", r.getApprovedBy());
        putIfNotNull(map, "status", r.getStatus());
        return map;
    }

    private Map<String, Object> reportToMap(FinanceReportRequest r) {
        var map = new HashMap<String, Object>();
        putIfNotNull(map, "mosque_id", r.getMosqueId());
        putIfNotNull(map, "audit_period_start", r.getAuditPeriodStart());
        putIfNotNull(map, "audit_period_end", r.getAuditPeriodEnd());
        putIfNotNull(map, "audited_by", r.getAuditedBy());
        putIfNotNull(map, "report_url", r.getReportUrl());
        putIfNotNull(map, "status", r.getStatus());
        putIfNotNull(map, "remarks", r.getRemarks());
        return map;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
