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
        verify(supabaseClient).getAll(eq("users"), captor.capture(), eq(Map.class));
        return captor.getValue();
    }

    @Test
    void getAll_searchByFullEmail_matchesEmailColumn() {
        when(supabaseClient.getAll(eq("users"), any(), eq(Map.class)))
                .thenReturn(List.of(Map.of("id", "u1", "email", "afiqtechno96@gmail.com")));

        var result = userService.getAll(20, 0, "afiqtechno96@gmail.com", null);

        assertThat(result).hasSize(1);
        var params = captureUsersQueryParams();
        assertThat(params.get("or"))
                .contains("email.ilike.*afiqtechno96@gmail.com*")
                .contains("username.ilike.*afiqtechno96@gmail.com*")
                .contains("fullname.ilike.*afiqtechno96@gmail.com*");
    }

    @Test
    void getAll_searchByPartialEmail_matchesEmailColumn() {
        when(supabaseClient.getAll(eq("users"), any(), eq(Map.class)))
                .thenReturn(List.of(Map.of("id", "u1", "email", "afiqtechno96@gmail.com")));

        var result = userService.getAll(20, 0, "afiqtechno", null);

        assertThat(result).hasSize(1);
        var params = captureUsersQueryParams();
        assertThat(params.get("or")).contains("email.ilike.*afiqtechno*");
    }

    @Test
    void getAll_noSearchTerm_omitsOrFilter() {
        when(supabaseClient.getAll(eq("users"), any(), eq(Map.class))).thenReturn(List.of());

        userService.getAll(20, 0, null, null);

        var params = captureUsersQueryParams();
        assertThat(params).doesNotContainKey("or");
    }

    @Test
    void getAll_escapesCommasAndParensInSearchTerm() {
        when(supabaseClient.getAll(eq("users"), any(), eq(Map.class))).thenReturn(List.of());

        userService.getAll(20, 0, "o'brien, (test)", null);

        var params = captureUsersQueryParams();
        assertThat(params.get("or")).contains("\\,").contains("\\(").contains("\\)");
    }

    // ─── mosqueId omission / global search ─────────────────────

    @Test
    void getAll_withoutMosqueId_doesNotQueryMosqueCommittees_andReturnsGlobalResults() {
        when(supabaseClient.getAll(eq("users"), any(), eq(Map.class)))
                .thenReturn(List.of(Map.of("id", "u1"), Map.of("id", "u2")));

        var result = userService.getAll(20, 0, "afiqtechno", null);

        assertThat(result).hasSize(2);
        verify(supabaseClient, never()).getAll(eq("mosque_committees"), any(), eq(Map.class));
        var params = captureUsersQueryParams();
        assertThat(params).doesNotContainKey("id"); // no mosque-membership id filter applied
        verify(accessControlService).requirePermission(null, Permission.USERS_READ);
    }

    @Test
    void getAll_withMosqueId_scopesToMosqueMembers() {
        when(supabaseClient.getAll(eq("mosque_committees"), any(), eq(Map.class)))
                .thenReturn(List.of(Map.of("user_id", "u1"), Map.of("user_id", "u2")));
        when(supabaseClient.getAll(eq("users"), any(), eq(Map.class)))
                .thenReturn(List.of(Map.of("id", "u1")));

        var result = userService.getAll(20, 0, null, 5L);

        assertThat(result).hasSize(1);
        var params = captureUsersQueryParams();
        assertThat(params.get("id")).isEqualTo("in.(u1,u2)");
    }

    @Test
    void getAll_withMosqueId_noMembers_returnsEmptyWithoutQueryingUsers() {
        when(supabaseClient.getAll(eq("mosque_committees"), any(), eq(Map.class)))
                .thenReturn(List.of());

        var result = userService.getAll(20, 0, null, 5L);

        assertThat(result).isEmpty();
        verify(supabaseClient, never()).getAll(eq("users"), any(), eq(Map.class));
    }
}
