-- ════════════════════════════════════════════════════════════════════════
--  AquariusWeb — SYSTEM DB — V1: schema iniziale
--
--  Tabelle metadata multi-tenant + super-admin.
--  Applicato dal SqlMigrationRunner (vedi config/) al primo avvio.
--
--  Syntax SQL Server-compatibile. H2 in MODE=MSSQLServer accetta IDENTITY,
--  DATETIME, GETDATE() come SQL Server.
--
--  Convenzione: ogni statement è seguito da una riga con solo "GO" che il
--  migrator usa come batch separator (NON viene inviata al DB).
-- ════════════════════════════════════════════════════════════════════════

CREATE TABLE tenants (
    tenant_id     VARCHAR(64)  NOT NULL,
    display_name  VARCHAR(200) NOT NULL,
    db_type       VARCHAR(40),
    enabled       BIT          NOT NULL DEFAULT 1,
    logo_path     VARCHAR(250),
    created_at    DATETIME     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT PK_tenants PRIMARY KEY (tenant_id)
)
GO

CREATE TABLE super_admins (
    id            BIGINT       NOT NULL IDENTITY(1,1),
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    email         VARCHAR(200),
    full_name     VARCHAR(200),
    enabled       BIT          NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT PK_super_admins PRIMARY KEY (id),
    CONSTRAINT UK_super_admins_username UNIQUE (username)
)
GO
