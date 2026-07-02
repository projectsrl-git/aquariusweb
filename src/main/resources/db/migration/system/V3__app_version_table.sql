-- ════════════════════════════════════════════════════════════════════════
--  AquariusWeb — SYSTEM DB — V3: tabella versioni dell'app
--
--  Ogni volta che si avvia una versione "nuova" (rispetto all'ultima
--  registrata) viene inserito un record. Questo dà uno storico audit-trail
--  di quando le varie release sono andate in produzione.
--
--  La versione "corrente" è semplicemente l'ultimo record per applied_at.
-- ════════════════════════════════════════════════════════════════════════

CREATE TABLE aq_web_app_version (
    id           BIGINT        NOT NULL IDENTITY(1,1),
    version      VARCHAR(50)   NOT NULL,
    build_time   VARCHAR(50),
    notes        VARCHAR(2000),
    applied_at   DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT PK_aq_web_app_version PRIMARY KEY (id)
)
GO

CREATE INDEX IX_aq_web_app_version_applied ON aq_web_app_version(applied_at)
GO
