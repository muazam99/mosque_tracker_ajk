package com.qiyam.committee.service;

import com.qiyam.committee.dto.RoleMemberRequest;
import com.qiyam.shared.client.SupabaseClient;
import com.qiyam.shared.exception.ConflictException;
import com.qiyam.shared.exception.ResourceNotFoundException;
import com.qiyam.shared.security.AccessControlService;
import com.qiyam.shared.security.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommitteeServiceTest {

    @Mock
    private SupabaseClient supabaseClient;

    @Mock
    private AccessControlService accessControlService;

    private CommitteeService committeeService;

    private static final String COMMITTEE_ID = "committee-1";
    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final Long MOSQUE_ID = 5L;

    @BeforeEach
    void setUp() {
        committeeService = new CommitteeService(supabaseClient, accessControlService);
    }

    private void stubCommitteeExists() {
        when(supabaseClient.getOne(eq("committee_roles"), eq("id"), eq(COMMITTEE_ID), eq(Map.class)))
                .thenReturn(Optional.of(Map.of("id", COMMITTEE_ID, "role_name", "TREASURER")));
    }

    // ─── getRoleMembers ────────────────────────────────────────

    @Test
    void getRoleMembers_returnsEmptyList_whenNoMembers() {
        stubCommitteeExists();
        when(supabaseClient.getAll(eq("committee_role_member"), anyMap(), eq(Map.class)))
                .thenReturn(List.of());
        when(accessControlService.getMosqueIds(null)).thenReturn(Set.of());

        var result = committeeService.getRoleMembers(COMMITTEE_ID);

        assertThat(result).isEmpty();
        verify(accessControlService).requirePermission(null, Permission.MEMBERS_READ);
    }

    @Test
    void getRoleMembers_throws404_whenCommitteeDoesNotExist() {
        when(supabaseClient.getOne(eq("committee_roles"), eq("id"), eq(COMMITTEE_ID), eq(Map.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> committeeService.getRoleMembers(COMMITTEE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getRoleMembers_filtersOutRowsOutsideCallersMosqueAccess() {
        stubCommitteeExists();
        when(supabaseClient.getAll(eq("committee_role_member"), anyMap(), eq(Map.class)))
                .thenReturn(List.of(
                        Map.of("id", "row-1", "mosque_id", 5),
                        Map.of("id", "row-2", "mosque_id", 9)));
        when(accessControlService.getMosqueIds(null)).thenReturn(Set.of(5));

        var result = committeeService.getRoleMembers(COMMITTEE_ID);

        assertThat(result).extracting(m -> m.get("id")).containsExactly("row-1");
    }

    @Test
    void getRoleMembers_superAdminSeesAllMosques() {
        stubCommitteeExists();
        when(supabaseClient.getAll(eq("committee_role_member"), anyMap(), eq(Map.class)))
                .thenReturn(List.of(
                        Map.of("id", "row-1", "mosque_id", 5),
                        Map.of("id", "row-2", "mosque_id", 9)));
        when(accessControlService.getMosqueIds(null)).thenReturn(null);

        var result = committeeService.getRoleMembers(COMMITTEE_ID);

        assertThat(result).hasSize(2);
    }

    // ─── addRoleMember ─────────────────────────────────────────

    @Test
    void addRoleMember_createsRowAndReturnsIt() {
        var request = new RoleMemberRequest();
        request.setUserId(USER_ID);
        request.setMosqueId(MOSQUE_ID);

        stubCommitteeExists();
        when(supabaseClient.getOne(eq("users"), eq("id"), eq(USER_ID), eq(Map.class)))
                .thenReturn(Optional.of(Map.of("id", USER_ID)));
        when(supabaseClient.getOne(eq("mosques"), eq("id"), eq(String.valueOf(MOSQUE_ID)), eq(Map.class)))
                .thenReturn(Optional.of(Map.of("id", MOSQUE_ID)));
        when(supabaseClient.getAll(eq("committee_role_member"), anyMap(), eq(Map.class)))
                .thenReturn(List.of());
        when(supabaseClient.postReturning(eq("committee_role_member"), any(), eq(Map.class)))
                .thenReturn(Map.of(
                        "id", "new-member-id",
                        "committee_role_id", COMMITTEE_ID,
                        "user_id", USER_ID,
                        "mosque_id", MOSQUE_ID,
                        "created_at", "2026-07-31T00:00:00Z"));

        var result = committeeService.addRoleMember(COMMITTEE_ID, request);

        assertThat(result.get("id")).isEqualTo("new-member-id");
        assertThat(result.get("created_at")).isNotNull();
        verify(accessControlService).requirePermissionAndMosque(null, Permission.MEMBERS_WRITE, MOSQUE_ID.intValue());
    }

    @Test
    void addRoleMember_rejectsMissingUserId() {
        var request = new RoleMemberRequest();
        request.setMosqueId(MOSQUE_ID);

        assertThatThrownBy(() -> committeeService.addRoleMember(COMMITTEE_ID, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addRoleMember_rejectsMissingMosqueId() {
        var request = new RoleMemberRequest();
        request.setUserId(USER_ID);

        assertThatThrownBy(() -> committeeService.addRoleMember(COMMITTEE_ID, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addRoleMember_throws404_whenUserDoesNotExist() {
        var request = new RoleMemberRequest();
        request.setUserId(USER_ID);
        request.setMosqueId(MOSQUE_ID);

        stubCommitteeExists();
        when(supabaseClient.getOne(eq("users"), eq("id"), eq(USER_ID), eq(Map.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> committeeService.addRoleMember(COMMITTEE_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addRoleMember_throws404_whenMosqueDoesNotExist() {
        var request = new RoleMemberRequest();
        request.setUserId(USER_ID);
        request.setMosqueId(MOSQUE_ID);

        stubCommitteeExists();
        when(supabaseClient.getOne(eq("users"), eq("id"), eq(USER_ID), eq(Map.class)))
                .thenReturn(Optional.of(Map.of("id", USER_ID)));
        when(supabaseClient.getOne(eq("mosques"), eq("id"), eq(String.valueOf(MOSQUE_ID)), eq(Map.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> committeeService.addRoleMember(COMMITTEE_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addRoleMember_throws409_onDuplicateMembership() {
        var request = new RoleMemberRequest();
        request.setUserId(USER_ID);
        request.setMosqueId(MOSQUE_ID);

        stubCommitteeExists();
        when(supabaseClient.getOne(eq("users"), eq("id"), eq(USER_ID), eq(Map.class)))
                .thenReturn(Optional.of(Map.of("id", USER_ID)));
        when(supabaseClient.getOne(eq("mosques"), eq("id"), eq(String.valueOf(MOSQUE_ID)), eq(Map.class)))
                .thenReturn(Optional.of(Map.of("id", MOSQUE_ID)));
        when(supabaseClient.getAll(eq("committee_role_member"), anyMap(), eq(Map.class)))
                .thenReturn(List.of(Map.of("id", "existing-row")));

        assertThatThrownBy(() -> committeeService.addRoleMember(COMMITTEE_ID, request))
                .isInstanceOf(ConflictException.class);
    }

    // ─── removeRoleMember ──────────────────────────────────────

    @Test
    void removeRoleMember_deletesRow() {
        stubCommitteeExists();
        when(supabaseClient.getOne(eq("committee_role_member"), eq("id"), eq("member-1"), eq(Map.class)))
                .thenReturn(Optional.of(Map.of("id", "member-1", "committee_role_id", COMMITTEE_ID, "mosque_id", 5)));

        committeeService.removeRoleMember(COMMITTEE_ID, "member-1");

        verify(accessControlService).requirePermissionAndMosque(null, Permission.MEMBERS_DELETE, 5);
        verify(supabaseClient).delete("committee_role_member", "id", "member-1");
    }

    @Test
    void removeRoleMember_throws404_whenMemberDoesNotExist() {
        stubCommitteeExists();
        when(supabaseClient.getOne(eq("committee_role_member"), eq("id"), eq("missing"), eq(Map.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> committeeService.removeRoleMember(COMMITTEE_ID, "missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeRoleMember_throws404_whenMemberBelongsToDifferentCommittee() {
        stubCommitteeExists();
        when(supabaseClient.getOne(eq("committee_role_member"), eq("id"), eq("member-1"), eq(Map.class)))
                .thenReturn(Optional.of(Map.of("id", "member-1", "committee_role_id", "other-committee", "mosque_id", 5)));

        assertThatThrownBy(() -> committeeService.removeRoleMember(COMMITTEE_ID, "member-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
