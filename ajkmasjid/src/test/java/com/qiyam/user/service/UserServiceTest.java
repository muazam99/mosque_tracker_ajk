package com.qiyam.user.service;

import com.qiyam.shared.client.SupabaseClient;
import com.qiyam.shared.security.AccessControlService;
import com.qiyam.shared.security.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private SupabaseClient supabaseClient;

    @Mock
    private AccessControlService accessControlService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(supabaseClient, accessControlService);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> captureUsersQueryParams() {
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(supabaseClient).getAllPaged(eq("users"), captor.capture(), eq(Map.class));
        return captor.getValue();
    }

    @Test
    void getAll_searchByFullEmail_matchesEmailColumn() {
        when(supabaseClient.getAllPaged(eq("users"), any(), eq(Map.class)))
                .thenReturn(new SupabaseClient.PageResult<>(
                        List.of(Map.of("id", "u1", "email", "afiqtechno96@gmail.com")), 1));

        var result = userService.getAll(20, 0, 1, "afiqtechno96@gmail.com", null);

        assertThat(result.data()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
        var params = captureUsersQueryParams();
        assertThat(params.get("or"))
                .contains("email.ilike.*afiqtechno96@gmail.com*")
                .contains("username.ilike.*afiqtechno96@gmail.com*")
                .contains("fullname.ilike.*afiqtechno96@gmail.com*");
    }

    @Test
    void getAll_searchByPartialEmail_matchesEmailColumn() {
        when(supabaseClient.getAllPaged(eq("users"), any(), eq(Map.class)))
                .thenReturn(new SupabaseClient.PageResult<>(
                        List.of(Map.of("id", "u1", "email", "afiqtechno96@gmail.com")), 1));

        var result = userService.getAll(20, 0, 1, "afiqtechno", null);

        assertThat(result.data()).hasSize(1);
        var params = captureUsersQueryParams();
        assertThat(params.get("or")).contains("email.ilike.*afiqtechno*");
    }

    @Test
    void getAll_noSearchTerm_omitsOrFilter() {
        when(supabaseClient.getAllPaged(eq("users"), any(), eq(Map.class)))
                .thenReturn(new SupabaseClient.PageResult<>(List.of(), 0));

        userService.getAll(20, 0, 1, null, null);

        var params = captureUsersQueryParams();
        assertThat(params).doesNotContainKey("or");
    }

    @Test
    void getAll_escapesCommasAndParensInSearchTerm() {
        when(supabaseClient.getAllPaged(eq("users"), any(), eq(Map.class)))
                .thenReturn(new SupabaseClient.PageResult<>(List.of(), 0));

        userService.getAll(20, 0, 1, "o'brien, (test)", null);

        var params = captureUsersQueryParams();
        assertThat(params.get("or")).contains("\\,").contains("\\(").contains("\\)");
    }

    // ─── pagination metadata ─────────────────────────────────

    @Test
    void getAll_returnsPagingMetadataFromContentRangeTotal() {
        when(supabaseClient.getAllPaged(eq("users"), any(), eq(Map.class)))
                .thenReturn(new SupabaseClient.PageResult<>(
                        List.of(Map.of("id", "u1")), 47));

        var result = userService.getAll(20, 40, 3, null, null);

        assertThat(result.total()).isEqualTo(47);
        assertThat(result.page()).isEqualTo(3);
        assertThat(result.perPage()).isEqualTo(20);
        assertThat(result.totalPages()).isEqualTo(3); // ceil(47/20)
    }

    // ─── mosqueId omission / global search ─────────────────────

    @Test
    void getAll_withoutMosqueId_doesNotQueryMosqueCommittees_andReturnsGlobalResults() {
        when(supabaseClient.getAllPaged(eq("users"), any(), eq(Map.class)))
                .thenReturn(new SupabaseClient.PageResult<>(
                        List.of(Map.of("id", "u1"), Map.of("id", "u2")), 2));

        var result = userService.getAll(20, 0, 1, "afiqtechno", null);

        assertThat(result.data()).hasSize(2);
        verify(supabaseClient, never()).getAll(eq("mosque_committees"), any(), eq(Map.class));
        var params = captureUsersQueryParams();
        assertThat(params).doesNotContainKey("id"); // no mosque-membership id filter applied
        verify(accessControlService).requirePermission(null, Permission.USERS_READ);
    }

    @Test
    void getAll_withMosqueId_scopesToMosqueMembers() {
        when(supabaseClient.getAll(eq("mosque_committees"), any(), eq(Map.class)))
                .thenReturn(List.of(Map.of("user_id", "u1"), Map.of("user_id", "u2")));
        when(supabaseClient.getAllPaged(eq("users"), any(), eq(Map.class)))
                .thenReturn(new SupabaseClient.PageResult<>(List.of(Map.of("id", "u1")), 1));

        var result = userService.getAll(20, 0, 1, null, 5L);

        assertThat(result.data()).hasSize(1);
        var params = captureUsersQueryParams();
        assertThat(params.get("id")).isEqualTo("in.(u1,u2)");
    }

    @Test
    void getAll_withMosqueId_noMembers_returnsEmptyWithoutQueryingUsers() {
        when(supabaseClient.getAll(eq("mosque_committees"), any(), eq(Map.class)))
                .thenReturn(List.of());

        var result = userService.getAll(20, 0, 1, null, 5L);

        assertThat(result.data()).isEmpty();
        assertThat(result.total()).isZero();
        verify(supabaseClient, never()).getAllPaged(eq("users"), any(), eq(Map.class));
    }
}
