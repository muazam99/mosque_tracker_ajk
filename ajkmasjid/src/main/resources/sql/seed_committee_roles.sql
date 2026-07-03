-- ============================================================
-- Seed committee_roles for mosque_id = 1
-- Hierarchy: lower level = higher privilege
-- ============================================================

INSERT INTO committee_roles (mosque_id, role_name, description, hierarchy_level, is_system_role) VALUES
(1, 'CHAIRMAN',      'Highest authority within the mosque.',           2, true),
(1, 'AUDITOR',       'Independent read-only reviewer.',                2, true),
(1, 'TREASURER',     'Manages finance, donations, expenses.',          3, true),
(1, 'SECRETARY',     'Manages announcements, meetings, documents.',    3, true),
(1, 'IMAM',          'Manages prayer schedules, religious activities.', 3, true),
(1, 'EVENT_MANAGER', 'Manages events, volunteers.',                    3, true),
(1, 'VOLUNTEER',     'Assists programs with limited access.',          4, true),
(1, 'PUBLIC_USER',   'Mobile app user. Public content only.',           5, true);

-- ============================================================
-- Seed roles table (for system-wide role loading)
-- ============================================================
INSERT INTO roles (name, hierarchy_level)
SELECT name, hierarchy_level FROM (VALUES
    ('SUPER_ADMIN',   1),
    ('CHAIRMAN',      2),
    ('AUDITOR',       2),
    ('TREASURER',     3),
    ('SECRETARY',     3),
    ('IMAM',          3),
    ('EVENT_MANAGER', 3),
    ('VOLUNTEER',     4),
    ('PUBLIC_USER',   5)
) AS t(name, hierarchy_level)
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.name = t.name);

-- ============================================================
-- Seed role_permissions (delete first to allow re-run)
-- ============================================================
DELETE FROM role_permissions;

-- CHAIRMAN - all permissions
INSERT INTO role_permissions (role_name, permission)
VALUES
('CHAIRMAN', 'DASHBOARD_READ'),
('CHAIRMAN', 'DASHBOARD_WRITE'),
('CHAIRMAN', 'FINANCE_READ'),
('CHAIRMAN', 'FINANCE_WRITE'),
('CHAIRMAN', 'FINANCE_DELETE'),
('CHAIRMAN', 'DONATIONS_READ'),
('CHAIRMAN', 'DONATIONS_WRITE'),
('CHAIRMAN', 'DONATIONS_DELETE'),
('CHAIRMAN', 'AUDIT_REPORTS_READ'),
('CHAIRMAN', 'AUDIT_REPORTS_WRITE'),
('CHAIRMAN', 'AUDIT_REPORTS_DELETE'),
('CHAIRMAN', 'REPORTS_READ'),
('CHAIRMAN', 'REPORTS_WRITE'),
('CHAIRMAN', 'DOCUMENTS_READ'),
('CHAIRMAN', 'DOCUMENTS_WRITE'),
('CHAIRMAN', 'DOCUMENTS_DELETE'),
('CHAIRMAN', 'ANNOUNCEMENTS_READ'),
('CHAIRMAN', 'ANNOUNCEMENTS_WRITE'),
('CHAIRMAN', 'ANNOUNCEMENTS_DELETE'),
('CHAIRMAN', 'MEETINGS_READ'),
('CHAIRMAN', 'MEETINGS_WRITE'),
('CHAIRMAN', 'MEETINGS_DELETE'),
('CHAIRMAN', 'ACTIVITIES_READ'),
('CHAIRMAN', 'ACTIVITIES_WRITE'),
('CHAIRMAN', 'ACTIVITIES_DELETE'),
('CHAIRMAN', 'VOLUNTEERS_READ'),
('CHAIRMAN', 'VOLUNTEERS_WRITE'),
('CHAIRMAN', 'VOLUNTEERS_DELETE'),
('CHAIRMAN', 'MOSQUE_SETTINGS_READ'),
('CHAIRMAN', 'MOSQUE_SETTINGS_WRITE'),
('CHAIRMAN', 'USERS_READ'),
('CHAIRMAN', 'USERS_WRITE'),
('CHAIRMAN', 'USERS_DELETE'),
('CHAIRMAN', 'MEMBERS_READ'),
('CHAIRMAN', 'MEMBERS_WRITE'),
('CHAIRMAN', 'MEMBERS_DELETE');

-- TREASURER
INSERT INTO role_permissions (role_name, permission)
VALUES
('TREASURER', 'DASHBOARD_READ'),
('TREASURER', 'DASHBOARD_WRITE'),
('TREASURER', 'FINANCE_READ'),
('TREASURER', 'FINANCE_WRITE'),
('TREASURER', 'FINANCE_DELETE'),
('TREASURER', 'DONATIONS_READ'),
('TREASURER', 'DONATIONS_WRITE'),
('TREASURER', 'DONATIONS_DELETE'),
('TREASURER', 'AUDIT_REPORTS_READ'),
('TREASURER', 'AUDIT_REPORTS_WRITE'),
('TREASURER', 'AUDIT_REPORTS_DELETE'),
('TREASURER', 'REPORTS_READ'),
('TREASURER', 'REPORTS_WRITE'),
('TREASURER', 'DOCUMENTS_READ'),
('TREASURER', 'DOCUMENTS_WRITE');

-- SECRETARY
INSERT INTO role_permissions (role_name, permission)
VALUES
('SECRETARY', 'DASHBOARD_READ'),
('SECRETARY', 'DASHBOARD_WRITE'),
('SECRETARY', 'ANNOUNCEMENTS_READ'),
('SECRETARY', 'ANNOUNCEMENTS_WRITE'),
('SECRETARY', 'ANNOUNCEMENTS_DELETE'),
('SECRETARY', 'DOCUMENTS_READ'),
('SECRETARY', 'DOCUMENTS_WRITE'),
('SECRETARY', 'DOCUMENTS_DELETE'),
('SECRETARY', 'MEETINGS_READ'),
('SECRETARY', 'MEETINGS_WRITE'),
('SECRETARY', 'MEETINGS_DELETE'),
('SECRETARY', 'MOSQUE_SETTINGS_READ'),
('SECRETARY', 'MOSQUE_SETTINGS_WRITE'),
('SECRETARY', 'REPORTS_READ'),
('SECRETARY', 'REPORTS_WRITE'),
('SECRETARY', 'DONATIONS_READ'),
('SECRETARY', 'DONATIONS_WRITE'),
('SECRETARY', 'ACTIVITIES_READ'),
('SECRETARY', 'ACTIVITIES_WRITE');

-- IMAM
INSERT INTO role_permissions (role_name, permission)
VALUES
('IMAM', 'DASHBOARD_READ'),
('IMAM', 'DASHBOARD_WRITE'),
('IMAM', 'ACTIVITIES_READ'),
('IMAM', 'ACTIVITIES_WRITE'),
('IMAM', 'ANNOUNCEMENTS_READ'),
('IMAM', 'ANNOUNCEMENTS_WRITE'),
('IMAM', 'DONATIONS_READ'),
('IMAM', 'MEETINGS_READ'),
('IMAM', 'MEETINGS_WRITE'),
('IMAM', 'DOCUMENTS_READ'),
('IMAM', 'DOCUMENTS_WRITE'),
('IMAM', 'REPORTS_READ');

-- EVENT_MANAGER
INSERT INTO role_permissions (role_name, permission)
VALUES
('EVENT_MANAGER', 'ACTIVITIES_READ'),
('EVENT_MANAGER', 'ACTIVITIES_WRITE'),
('EVENT_MANAGER', 'ACTIVITIES_DELETE'),
('EVENT_MANAGER', 'VOLUNTEERS_READ'),
('EVENT_MANAGER', 'VOLUNTEERS_WRITE'),
('EVENT_MANAGER', 'VOLUNTEERS_DELETE'),
('EVENT_MANAGER', 'DASHBOARD_READ'),
('EVENT_MANAGER', 'DASHBOARD_WRITE'),
('EVENT_MANAGER', 'DONATIONS_READ'),
('EVENT_MANAGER', 'DONATIONS_WRITE'),
('EVENT_MANAGER', 'ANNOUNCEMENTS_READ'),
('EVENT_MANAGER', 'ANNOUNCEMENTS_WRITE'),
('EVENT_MANAGER', 'MEETINGS_READ'),
('EVENT_MANAGER', 'MEETINGS_WRITE'),
('EVENT_MANAGER', 'REPORTS_READ'),
('EVENT_MANAGER', 'REPORTS_WRITE');

-- AUDITOR
INSERT INTO role_permissions (role_name, permission)
VALUES
('AUDITOR', 'DASHBOARD_READ'),
('AUDITOR', 'FINANCE_READ'),
('AUDITOR', 'DONATIONS_READ'),
('AUDITOR', 'AUDIT_REPORTS_READ'),
('AUDITOR', 'AUDIT_REPORTS_WRITE'),
('AUDITOR', 'AUDIT_REPORTS_DELETE'),
('AUDITOR', 'DOCUMENTS_READ'),
('AUDITOR', 'MEETINGS_READ'),
('AUDITOR', 'REPORTS_READ');

-- VOLUNTEER
INSERT INTO role_permissions (role_name, permission)
VALUES
('VOLUNTEER', 'DASHBOARD_READ'),
('VOLUNTEER', 'ACTIVITIES_READ'),
('VOLUNTEER', 'VOLUNTEERS_READ'),
('VOLUNTEER', 'ANNOUNCEMENTS_READ');

-- PUBLIC_USER has no permissions (empty set)
-- SUPER_ADMIN implicitly has all via hierarchy level 1
