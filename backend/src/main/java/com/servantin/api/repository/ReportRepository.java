package com.servantin.api.repository;

import com.servantin.api.domain.entity.Report;
import com.servantin.api.domain.model.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

       /**
        * Find reports by status with pagination
        */
       Page<Report> findByStatus(ReportStatus status, Pageable pageable);

       /**
        * Find all reports for a specific reported user with pagination
        */
       Page<Report> findByReportedUser_Id(UUID reportedUserId, Pageable pageable);

       /**
        * Find all reports with full details (for admin dashboard)
        */
       @Query("SELECT r FROM Report r " +
                     "LEFT JOIN FETCH r.reporter " +
                     "LEFT JOIN FETCH r.reportedUser " +
                     "LEFT JOIN FETCH r.reportedBooking " +
                     "WHERE r.id = :id")
       Report findByIdWithDetails(UUID id);

       /**
        * Count pending reports for a specific user
        */
       long countByStatusAndReportedUser_Id(ReportStatus status, UUID reportedUserId);

       /**
        * Count total reports by reporter (to detect report abuse)
        */
       long countByReporter_Id(UUID reporterId);

       /**
        * Find reports where user is either reporter or reported
        */
       @Query("SELECT r FROM Report r WHERE r.reporter.id = :userId OR r.reportedUser.id = :userId")
       java.util.List<Report> findByReporterIdOrReportedUserId(UUID userId);

       /**
        * Find all reports with pagination (for admin)
        */
       @Query("SELECT r FROM Report r " +
                     "LEFT JOIN FETCH r.reporter " +
                     "LEFT JOIN FETCH r.reportedUser " +
                     "LEFT JOIN FETCH r.reportedBooking")
       Page<Report> findAllWithDetails(Pageable pageable);
}
