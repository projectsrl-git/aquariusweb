package com.aquarius.repository.tenant;

import com.aquarius.entity.tenant.CustomReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomReportRepository extends JpaRepository<CustomReport, Long> {

    List<CustomReport> findByIsActiveTrueOrderByCategoryAscNameAsc();

    List<CustomReport> findByCategoryAndIsActiveTrueOrderByNameAsc(String category);

    List<CustomReport> findByCreatedByOrderByNameAsc(String createdBy);
}
