-- ════════════════════════════════════════════════════════════════════════
--  AquariusWeb — TENANT DB — V3: custom reports (porting CReaM)
--
--  Tabella che memorizza le definizioni di query SQL personalizzate
--  dell'utente. La logica di esecuzione (con whitelist SELECT-only e
--  PreparedStatement per i parametri) è in CustomReportService.
-- ════════════════════════════════════════════════════════════════════════

CREATE TABLE [dbo].[aq_web_custom_reports] (
    [id]               BIGINT IDENTITY(1,1) NOT NULL,
    [name]             VARCHAR(200)         NOT NULL,
    [description]      VARCHAR(500)         NULL,
    [sql_query]        VARCHAR(MAX)         NOT NULL,
    [parameters]       VARCHAR(MAX)         NULL,
    [category]         VARCHAR(50)          NULL,
    [output_format]    VARCHAR(20)          NULL,
    [chart_type]       VARCHAR(20)          NULL,
    [color]            VARCHAR(7)           NULL,
    [icon]             VARCHAR(50)          NULL,
    [is_active]        BIT                  NULL CONSTRAINT [DF_aq_web_custom_reports_active] DEFAULT (1),
    [is_public]        BIT                  NULL CONSTRAINT [DF_aq_web_custom_reports_public] DEFAULT (1),
    [created_by]       VARCHAR(20)          NULL,
    [created_at]       DATETIME             NULL,
    [last_executed]    DATETIME             NULL,
    [execution_count]  INT                  NULL CONSTRAINT [DF_aq_web_custom_reports_count] DEFAULT (0),
    CONSTRAINT [PK_aq_web_custom_reports] PRIMARY KEY CLUSTERED ([id])
)
GO

CREATE INDEX [IX_aq_web_custom_reports_active]
    ON [dbo].[aq_web_custom_reports] ([is_active], [category])
GO
