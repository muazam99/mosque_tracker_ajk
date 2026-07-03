-- ============================================================
-- SEED DATA FOR QIYAM AJK MASJID RBAC SYSTEM
-- Run this in Supabase SQL Editor
-- ============================================================

-- ============================================================
-- 1. Seed committee_roles for mosque_id = 1
--    (created_at and updated_at auto-set by Supabase)
-- ============================================================

INSERT INTO committee_roles (mosque_id, role_name, description, hierarchy_level, is_system_role) VALUES
(1, 'CHAIRMAN',      'Highest authority within the mosque. Full access to all modules.',           2, true),
(1, 'AUDITOR',       'Independent read-only reviewer. Reports directly to Super Admin.',           2, true),
(1, 'TREASURER',     'Manages finance, donations, expenses and reports. Reports to Chairman.',     3, true),
(1, 'SECRETARY',     'Manages announcements, meetings, documents and administration.',             3, true),
(1, 'IMAM',          'Manages prayer schedules, religious activities and Islamic programs.',        3, true),
(1, 'EVENT_MANAGER', 'Manages events, volunteers and community engagement. Reports to Chairman.',  3, true),
(1, 'VOLUNTEER',     'Assists programs. Limited operational access.',                               4, true),
(1, 'PUBLIC_USER',   'Mobile app user. Can only interact with public content and personal profile.',5, true);

-- ============================================================
-- 2. Get the IDs of the committee_roles we just inserted
--    (Supabase auto-generates the id column)
-- ============================================================
-- After running the above, run this to find the IDs:
-- SELECT id, role_name FROM committee_roles WHERE mosque_id = 1 ORDER BY id;

-- ============================================================
-- 3. Seed role_permissions
--    Using committee_role_id references
--    REPLACE the X values below with the actual IDs from step 2
-- ============================================================
-- Example: If CHAIRMAN has id=1, TREASURER=3, SECRETARY=4, IMAM=5, EVENT_MANAGER=6, AUDITOR=2, VOLUNTEER=7
-- Replace the committee_role_id values with your actual IDs.

DELETE FROM role_permissions WHERE committee_role_id IN (
    SELECT id FROM committee_roles WHERE mosque_id = 1
);

-- CHAIRMAN (all permissions)
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'DASHBOARD_READ' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'DASHBOARD_WRITE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'FINANCE_READ' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'FINANCE_WRITE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'FINANCE_DELETE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'DONATIONS_READ' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'DONATIONS_WRITE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'DONATIONS_DELETE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'AUDIT_REPORTS_READ' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'AUDIT_REPORTS_WRITE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'AUDIT_REPORTS_DELETE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'REPORTS_READ' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'REPORTS_WRITE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'DOCUMENTS_READ' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'DOCUMENTS_WRITE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'DOCUMENTS_DELETE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'ANNOUNCEMENTS_READ' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'ANNOUNCEMENTS_WRITE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'ANNOUNCEMENTS_DELETE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'MEETINGS_READ' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'MEETINGS_WRITE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'MEETINGS_DELETE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'ACTIVITIES_READ' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'ACTIVITIES_WRITE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'ACTIVITIES_DELETE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'VOLUNTEERS_READ' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'VOLUNTEERS_WRITE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'VOLUNTEERS_DELETE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'MOSQUE_SETTINGS_READ' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'MOSQUE_SETTINGS_WRITE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'USERS_READ' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'USERS_WRITE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'USERS_DELETE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'MEMBERS_READ' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'MEMBERS_WRITE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, 'MEMBERS_DELETE' FROM committee_roles WHERE mosque_id = 1 AND role_name = 'CHAIRMAN';

-- TREASURER
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, p FROM committee_roles, (VALUES
    ('DASHBOARD_READ'), ('DASHBOARD_WRITE'),
    ('FINANCE_READ'), ('FINANCE_WRITE'), ('FINANCE_DELETE'),
    ('DONATIONS_READ'), ('DONATIONS_WRITE'), ('DONATIONS_DELETE'),
    ('AUDIT_REPORTS_READ'), ('AUDIT_REPORTS_WRITE'), ('AUDIT_REPORTS_DELETE'),
    ('REPORTS_READ'), ('REPORTS_WRITE'),
    ('DOCUMENTS_READ'), ('DOCUMENTS_WRITE')
) AS t(p) WHERE mosque_id = 1 AND role_name = 'TREASURER';

-- SECRETARY
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, p FROM committee_roles, (VALUES
    ('DASHBOARD_READ'), ('DASHBOARD_WRITE'),
    ('ANNOUNCEMENTS_READ'), ('ANNOUNCEMENTS_WRITE'), ('ANNOUNCEMENTS_DELETE'),
    ('DOCUMENTS_READ'), ('DOCUMENTS_WRITE'), ('DOCUMENTS_DELETE'),
    ('MEETINGS_READ'), ('MEETINGS_WRITE'), ('MEETINGS_DELETE'),
    ('MOSQUE_SETTINGS_READ'), ('MOSQUE_SETTINGS_WRITE'),
    ('REPORTS_READ'), ('REPORTS_WRITE'),
    ('DONATIONS_READ'), ('DONATIONS_WRITE'),
    ('ACTIVITIES_READ'), ('ACTIVITIES_WRITE')
) AS t(p) WHERE mosque_id = 1 AND role_name = 'SECRETARY';

-- IMAM
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, p FROM committee_roles, (VALUES
    ('DASHBOARD_READ'), ('DASHBOARD_WRITE'),
    ('ACTIVITIES_READ'), ('ACTIVITIES_WRITE'),
    ('ANNOUNCEMENTS_READ'), ('ANNOUNCEMENTS_WRITE'),
    ('DONATIONS_READ'),
    ('MEETINGS_READ'), ('MEETINGS_WRITE'),
    ('DOCUMENTS_READ'), ('DOCUMENTS_WRITE'),
    ('REPORTS_READ')
) AS t(p) WHERE mosque_id = 1 AND role_name = 'IMAM';

-- EVENT_MANAGER
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, p FROM committee_roles, (VALUES
    ('ACTIVITIES_READ'), ('ACTIVITIES_WRITE'), ('ACTIVITIES_DELETE'),
    ('VOLUNTEERS_READ'), ('VOLUNTEERS_WRITE'), ('VOLUNTEERS_DELETE'),
    ('DASHBOARD_READ'), ('DASHBOARD_WRITE'),
    ('DONATIONS_READ'), ('DONATIONS_WRITE'),
    ('ANNOUNCEMENTS_READ'), ('ANNOUNCEMENTS_WRITE'),
    ('MEETINGS_READ'), ('MEETINGS_WRITE'),
    ('REPORTS_READ'), ('REPORTS_WRITE')
) AS t(p) WHERE mosque_id = 1 AND role_name = 'EVENT_MANAGER';

-- AUDITOR
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, p FROM committee_roles, (VALUES
    ('DASHBOARD_READ'),
    ('FINANCE_READ'),
    ('DONATIONS_READ'),
    ('AUDIT_REPORTS_READ'), ('AUDIT_REPORTS_WRITE'), ('AUDIT_REPORTS_DELETE'),
    ('DOCUMENTS_READ'),
    ('MEETINGS_READ'),
    ('REPORTS_READ')
) AS t(p) WHERE mosque_id = 1 AND role_name = 'AUDITOR';

-- VOLUNTEER
INSERT INTO role_permissions (committee_role_id, permission_code)
SELECT id, p FROM committee_roles, (VALUES
    ('DASHBOARD_READ'),
    ('ACTIVITIES_READ'),
    ('VOLUNTEERS_READ'),
    ('ANNOUNCEMENTS_READ')
) AS t(p) WHERE mosque_id = 1 AND role_name = 'VOLUNTEER';

-- PUBLIC_USER has no permissions
