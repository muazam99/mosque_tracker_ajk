package com.qiyam.shared.base;

import com.qiyam.shared.security.AccessControlService;
import com.qiyam.shared.security.Permission;
import com.qiyam.shared.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Base service providing convenient access to authentication and permission checks.
 */
public abstract class BaseService {

    protected final AccessControlService accessControlService;

    protected BaseService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    protected Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    protected UserPrincipal getUser() {
        return UserPrincipal.requireFrom(getAuth());
    }

    protected Integer getMosqueId() {
        return accessControlService.getMosqueId(getAuth());
    }

    protected void requirePermission(Permission permission) {
        accessControlService.requirePermission(getAuth(), permission);
    }

    protected void requirePermissionAndMosque(Permission permission, Integer targetMosqueId) {
        accessControlService.requirePermissionAndMosque(getAuth(), permission, targetMosqueId);
    }
}
