package com.qiyam.shared.security;

/**
 * Granular permissions for module-level access control.
 * Maps directly to rows in the permission matrix.
 */
public enum Permission {
    // Dashboard
    DASHBOARD_READ,
    DASHBOARD_WRITE,

    // Finance
    FINANCE_READ,
    FINANCE_WRITE,
    FINANCE_DELETE,

    // Donations
    DONATIONS_READ,
    DONATIONS_WRITE,
    DONATIONS_DELETE,

    // Audit Reports
    AUDIT_REPORTS_READ,
    AUDIT_REPORTS_WRITE,
    AUDIT_REPORTS_DELETE,

    // Reports
    REPORTS_READ,
    REPORTS_WRITE,

    // Documents
    DOCUMENTS_READ,
    DOCUMENTS_WRITE,
    DOCUMENTS_DELETE,

    // Announcements
    ANNOUNCEMENTS_READ,
    ANNOUNCEMENTS_WRITE,
    ANNOUNCEMENTS_DELETE,

    // Meetings
    MEETINGS_READ,
    MEETINGS_WRITE,
    MEETINGS_DELETE,

    // Activities
    ACTIVITIES_READ,
    ACTIVITIES_WRITE,
    ACTIVITIES_DELETE,

    // Volunteers
    VOLUNTEERS_READ,
    VOLUNTEERS_WRITE,
    VOLUNTEERS_DELETE,

    // Mosque Settings
    MOSQUE_SETTINGS_READ,
    MOSQUE_SETTINGS_WRITE,

    // Users
    USERS_READ,
    USERS_WRITE,
    USERS_DELETE,

    // Members
    MEMBERS_READ,
    MEMBERS_WRITE,
    MEMBERS_DELETE,
}
