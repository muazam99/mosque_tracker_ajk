package com.qiyam.finance.service;

import com.qiyam.finance.dto.FinanceAccountRequest;
import com.qiyam.finance.dto.FinanceReportRequest;
import com.qiyam.finance.dto.FinanceTransactionRequest;
import com.qiyam.shared.client.SupabaseClient;
import com.qiyam.shared.dto.PagedResponse;
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

    // ─── Helpers ─────────────────────────────────────────────

    /** Applies a resolved mosque scope (see {@link AccessControlService#resolveMosqueScope}) to query params. */
    private void applyMosqueScope(Map<String, String> params, Set<Integer> scope) {
        if (scope == null) return; // unrestricted (SUPER_ADMIN)
        if (scope.size() == 1) {
            params.put("mosque_id", "eq." + scope.iterator().next());
        } else {
            params.put("mosque_id", "in.(" + scope.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + ")");
        }
    }

    // ─── Accounts ─────────────────────────────────────────────

    public PagedResponse<Map<String, Object>> getAllAccounts(int limit, int offset, int page, Integer mosqueId) {
        var scope = accessControlService.resolveMosqueScope(null, Permission.FINANCE_READ, mosqueId);
        if (scope != null && scope.isEmpty()) return PagedResponse.empty(page, limit);
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "created_at.desc");
        applyMosqueScope(params, scope);
        var result = supabaseClient.getAllPaged("finance_accounts", params, Map.class);
        return PagedResponse.of((List<Map<String, Object>>) (List<?>) result.data(), result.total(), page, limit);
    }

    public Optional<Map<String, Object>> getAccountById(Long id) {
        accessControlService.requirePermission(null, Permission.FINANCE_READ);
        var row = (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("finance_accounts", "id", String.valueOf(id), Map.class);
        row.ifPresent(r -> accessControlService.requireRowMosqueAccess(null, r.get("mosque_id")));
        return row;
    }

    public Map<String, Object> createAccount(FinanceAccountRequest request) {
        accessControlService.requirePermission(null, Permission.FINANCE_WRITE);
        if (request.getMosqueId() != null) {
            accessControlService.requireRowMosqueAccess(null, request.getMosqueId());
        }
        var body = accountToMap(request);
        var result = supabaseClient.post("finance_accounts", body, Map.class);
        return result != null ? result : Map.of();
    }

    public Map<String, Object> updateAccount(Long id, FinanceAccountRequest request) {
        accessControlService.requirePermission(null, Permission.FINANCE_WRITE);
        verifyMosqueOwnership("finance_accounts", String.valueOf(id));
        var body = accountToMap(request);
        var result = supabaseClient.patch("finance_accounts", "id", String.valueOf(id), body, Map.class);
        return result != null ? result : Map.of();
    }

    public void deleteAccount(Long id) {
        accessControlService.requirePermission(null, Permission.FINANCE_DELETE);
        verifyMosqueOwnership("finance_accounts", String.valueOf(id));
        supabaseClient.delete("finance_accounts", "id", String.valueOf(id));
    }

    /** Fetches a row by id and, if found, verifies the caller has access to its mosque_id. */
    private void verifyMosqueOwnership(String table, String id) {
        var row = supabaseClient.getOne(table, "id", id, Map.class);
        row.ifPresent(r -> accessControlService.requireRowMosqueAccess(null, ((Map<?, ?>) r).get("mosque_id")));
    }

    // ─── Transactions ─────────────────────────────────────────

    public PagedResponse<Map<String, Object>> getAllTransactions(int limit, int offset, int page, Integer mosqueId) {
        var scope = accessControlService.resolveMosqueScope(null, Permission.FINANCE_READ, mosqueId);
        if (scope != null && scope.isEmpty()) return PagedResponse.empty(page, limit);
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "transaction_date.desc");
        applyMosqueScope(params, scope);
        var result = supabaseClient.getAllPaged("finance_transactions", params, Map.class);
        return PagedResponse.of((List<Map<String, Object>>) (List<?>) result.data(), result.total(), page, limit);
    }

    public Optional<Map<String, Object>> getTransactionById(Long id) {
        accessControlService.requirePermission(null, Permission.FINANCE_READ);
        var row = (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("finance_transactions", "id", String.valueOf(id), Map.class);
        row.ifPresent(r -> accessControlService.requireRowMosqueAccess(null, r.get("mosque_id")));
        return row;
    }

    public Map<String, Object> createTransaction(FinanceTransactionRequest request) {
        accessControlService.requirePermission(null, Permission.FINANCE_WRITE);
        if (request.getMosqueId() != null) {
            accessControlService.requireRowMosqueAccess(null, request.getMosqueId());
        }
        var body = transactionToMap(request);
        var result = supabaseClient.post("finance_transactions", body, Map.class);
        return result != null ? result : Map.of();
    }

    // ─── Reports (finance_audits) ────────────────────────────

    public PagedResponse<Map<String, Object>> getAllReports(int limit, int offset, int page, Integer mosqueId) {
        var scope = accessControlService.resolveMosqueScope(null, Permission.REPORTS_READ, mosqueId);
        if (scope != null && scope.isEmpty()) return PagedResponse.empty(page, limit);
        var params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        params.put("order", "created_at.desc");
        applyMosqueScope(params, scope);
        var result = supabaseClient.getAllPaged("finance_audits", params, Map.class);
        return PagedResponse.of((List<Map<String, Object>>) (List<?>) result.data(), result.total(), page, limit);
    }

    public Optional<Map<String, Object>> getReportById(Long id) {
        accessControlService.requirePermission(null, Permission.REPORTS_READ);
        var row = (Optional<Map<String, Object>>) (Optional<?>) supabaseClient.getOne("finance_audits", "id", String.valueOf(id), Map.class);
        row.ifPresent(r -> accessControlService.requireRowMosqueAccess(null, r.get("mosque_id")));
        return row;
    }

    public Map<String, Object> createReport(FinanceReportRequest request) {
        accessControlService.requirePermission(null, Permission.REPORTS_WRITE);
        if (request.getMosqueId() != null) {
            accessControlService.requireRowMosqueAccess(null, request.getMosqueId());
        }
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
