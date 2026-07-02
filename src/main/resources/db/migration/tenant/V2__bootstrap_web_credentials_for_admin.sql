-- ════════════════════════════════════════════════════════════════════════
--  Aquarius web — TENANT DB — V2: bootstrap credenziali web
--
--  Crea una entry in aq_web_user_credentials per l'operatore legacy 'admin'
--  (se esiste in res_oper). Sostituisce la vecchia logica del DataSeeder.
--
--  È un'operazione di bootstrap, idempotente:
--  - se l'utente legacy 'admin' non esiste → non fa nulla
--  - se le credenziali web esistono già → non fa nulla
--
--  Password: 'admin' (BCrypt rounds=10).
--  must_reset_password=1 → al primo login l'utente DEVE cambiare password.
--
--  Per disattivare questo bootstrap su un nuovo deployment (es. quando hai
--  già imposterai gli utenti web tramite UI super-admin), rinomina questo
--  file da .sql a .sql.disabled prima del primo avvio.
-- ════════════════════════════════════════════════════════════════════════

IF EXISTS (SELECT 1 FROM dbo.res_oper WHERE UPPER(LTRIM(RTRIM(CODICE))) = 'ADMIN')
   AND NOT EXISTS (SELECT 1 FROM dbo.aq_web_user_credentials WHERE operator_code = 'admin')
BEGIN
    INSERT INTO dbo.aq_web_user_credentials
        (operator_code, password_hash, must_reset_password, created_at)
    VALUES
        ('admin',
         '$2a$10$RyW4OANBJusf.UgKZctGXOQHeprPnfQf9HXbeJHIQibBr60VPpBNa',
         1,
         GETDATE());
    PRINT '+ Credenziali web create per operatore legacy admin (password: admin, deve cambiarla al primo login)';
END
ELSE
BEGIN
    PRINT '= Bootstrap web credentials saltato (utente admin assente o già provisioned)';
END
GO
