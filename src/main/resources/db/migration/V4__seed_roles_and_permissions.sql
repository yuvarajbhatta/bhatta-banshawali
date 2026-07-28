-- Seeds the roles and permissions already decided in
-- docs/06-ui-ux-specification.md's role/permission matrix (Phase 0), so
-- Phase 3's signup/verification work has reference data to assign against
-- instead of re-deriving it. VISITOR is intentionally not a stored row --
-- it is the implicit, unauthenticated default, per the same doc.

INSERT INTO permissions (name, description) VALUES
    ('VIEW_OWN_VERIFICATION_STATUS', 'View own signup/verification status'),
    ('VIEW_DASHBOARD', 'View the member dashboard and family snapshot'),
    ('VIEW_WHOLE_TREE', 'View the Whole Banshawali tree, subject to privacy rules'),
    ('SUGGEST_CORRECTION', 'Suggest a correction to genealogy data'),
    ('PROPOSE_PEOPLE_RELATIONSHIPS', 'Propose or edit people and relationships within approved scope'),
    ('APPROVE_SIGNUPS_AND_CHANGES', 'Approve or reject signup requests and change requests'),
    ('MERGE_DUPLICATES', 'Merge duplicate person records'),
    ('MANAGE_USERS_AND_ROLES', 'Manage user accounts and role assignments'),
    ('MANAGE_SYSTEM_SETTINGS', 'Manage system-wide settings, security controls, and backups');

INSERT INTO roles (name, description) VALUES
    ('PENDING_MEMBER', 'Submitted signup information; not yet verified'),
    ('VERIFIED_MEMBER', 'Verified family member with full member access'),
    ('FAMILY_EDITOR', 'Verified member who can also propose genealogy edits'),
    ('ADMINISTRATOR', 'Reviews signups and changes, manages users and duplicates'),
    ('SUPER_ADMINISTRATOR', 'Full system administration, including destructive operations');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'PENDING_MEMBER' AND p.name = 'VIEW_OWN_VERIFICATION_STATUS';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'VERIFIED_MEMBER'
  AND p.name IN ('VIEW_OWN_VERIFICATION_STATUS', 'VIEW_DASHBOARD', 'VIEW_WHOLE_TREE', 'SUGGEST_CORRECTION');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'FAMILY_EDITOR'
  AND p.name IN ('VIEW_OWN_VERIFICATION_STATUS', 'VIEW_DASHBOARD', 'VIEW_WHOLE_TREE', 'SUGGEST_CORRECTION',
                 'PROPOSE_PEOPLE_RELATIONSHIPS');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMINISTRATOR'
  AND p.name IN ('VIEW_OWN_VERIFICATION_STATUS', 'VIEW_DASHBOARD', 'VIEW_WHOLE_TREE', 'SUGGEST_CORRECTION',
                 'PROPOSE_PEOPLE_RELATIONSHIPS', 'APPROVE_SIGNUPS_AND_CHANGES', 'MERGE_DUPLICATES',
                 'MANAGE_USERS_AND_ROLES');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMINISTRATOR'
  AND p.name IN ('VIEW_OWN_VERIFICATION_STATUS', 'VIEW_DASHBOARD', 'VIEW_WHOLE_TREE', 'SUGGEST_CORRECTION',
                 'PROPOSE_PEOPLE_RELATIONSHIPS', 'APPROVE_SIGNUPS_AND_CHANGES', 'MERGE_DUPLICATES',
                 'MANAGE_USERS_AND_ROLES', 'MANAGE_SYSTEM_SETTINGS');
