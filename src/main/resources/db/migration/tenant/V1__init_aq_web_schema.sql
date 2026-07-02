-- ════════════════════════════════════════════════════════════════════════
--  AquariusWeb — TENANT DB — V1: schema web-only
--
--  Applicata da TenantMigrationsRunner a ciascun database tenant
--  (Impresind, Tremonti, ...) al primo avvio.
--
--  Strategia dati 1.3: NESSUNA modifica alle tabelle legacy. Solo CREATE
--  di tabelle nuove con prefisso aq_web_.
--
--  Convenzione: ogni statement separato da una riga con solo "GO"
--  (il migrator splitta lì e invia ogni batch al DB).
-- ════════════════════════════════════════════════════════════════════════

-- ─── aq_web_user_credentials ──────────────────────────────────────────
--
-- Credenziali "web" (BCrypt hash) dell'operatore Aquarius.
-- Chiave logica: operator_code = res_oper.CODICE (NO foreign key fisica).
--
CREATE TABLE [dbo].[aq_web_user_credentials] (
    [id]                       BIGINT IDENTITY(1,1) NOT NULL,
    [operator_code]            VARCHAR(20)  NOT NULL,
    [password_hash]            VARCHAR(200) NOT NULL,
    [must_reset_password]      BIT          NOT NULL CONSTRAINT [DF_aq_web_credentials_must_reset] DEFAULT (0),
    [reset_token]              VARCHAR(100) NULL,
    [reset_token_expires_at]   DATETIME     NULL,
    [last_login_at]            DATETIME     NULL,
    [created_at]               DATETIME     NOT NULL CONSTRAINT [DF_aq_web_credentials_created] DEFAULT (GETDATE()),
    [updated_at]               DATETIME     NULL,
    CONSTRAINT [PK_aq_web_user_credentials] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [UK_aq_web_credentials_op_code] UNIQUE ([operator_code])
)
GO
