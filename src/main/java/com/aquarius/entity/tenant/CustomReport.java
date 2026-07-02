package com.aquarius.entity.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Custom report — definizione di una query SQL parametrica definita dall'utente.
 *
 * Porting da CReaM ({@code com.cream.entity.CustomReport}) adattato a:
 *  - multi-tenant: la tabella vive nel DB del tenant (le query lavorano sui
 *    dati di quel tenant, quindi le definizioni sono lì)
 *  - strategia 1.3: tabella NUOVA con prefisso aq_web_, nessun impatto sul legacy
 *  - link a operatore: stringa (operator_code) invece di FK a User entity
 *
 * Sicurezza: il service che esegue la query whitelista solo SELECT
 * (vedi {@link com.aquarius.service.CustomReportService}).
 */
@Entity
@Table(name = "aq_web_custom_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    /** Query SQL parametrica. Parametri nominali nella forma :paramName */
    @Column(name = "sql_query", nullable = false, columnDefinition = "varchar(max)")
    private String sqlQuery;

    /** JSON array con la definizione dei parametri attesi. */
    @Column(columnDefinition = "varchar(max)")
    private String parameters;

    /** Categoria libera (es. VENDITE, MAGAZZINO, CONTABILITA, PRODUZIONE). */
    @Column(length = 50)
    private String category;

    /** TABLE / CHART / PIVOT */
    @Column(name = "output_format", length = 20)
    private String outputFormat;

    /** BAR / LINE / PIE (rilevante se outputFormat=CHART) */
    @Column(name = "chart_type", length = 20)
    private String chartType;

    /** Colore esadecimale per la card del report. */
    @Column(length = 7)
    private String color;

    /** Classe Bootstrap Icons (es. "bi-bar-chart"). */
    @Column(length = 50)
    private String icon;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

    /** Se true → visibile a tutti gli operatori del tenant. */
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = Boolean.TRUE;

    /** Codice operatore che ha creato il report (= res_oper.CODICE). */
    @Column(name = "created_by", length = 20)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_executed")
    private LocalDateTime lastExecuted;

    @Column(name = "execution_count")
    @Builder.Default
    private Integer executionCount = 0;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (executionCount == null) executionCount = 0;
        if (isActive == null) isActive = true;
        if (isPublic == null) isPublic = true;
    }

    /** Incrementa il contatore e registra l'ultima esecuzione. */
    public void recordExecution() {
        this.lastExecuted = LocalDateTime.now();
        this.executionCount = (this.executionCount == null ? 0 : this.executionCount) + 1;
    }
}
