-- ════════════════════════════════════════════════════════════════════════
--  AquariusWeb — SYSTEM DB — V2: seed iniziale di tenant + super-admin
--
--  Inserisce i due tenant noti (Impresind, Tremonti) e un super-admin di
--  default. Le credenziali reali dei DB sono in application.properties,
--  questa tabella serve solo per la UI (combobox di login, display name).
-- ════════════════════════════════════════════════════════════════════════

INSERT INTO tenants (tenant_id, display_name, db_type, enabled)
VALUES ('impresind', 'Impresind', 'sqlserver', 1)
GO

INSERT INTO tenants (tenant_id, display_name, db_type, enabled)
VALUES ('tremonti', 'Tremonti', 'sqlserver', 1)
GO

-- Super-admin di default: username=admin, password=admin (BCrypt strength=10).
-- Cambia subito la password in produzione.
INSERT INTO super_admins (username, password_hash, full_name, enabled)
VALUES ('admin',
        '$2a$10$RyW4OANBJusf.UgKZctGXOQHeprPnfQf9HXbeJHIQibBr60VPpBNa',
        'Super Admin',
        1)
GO
