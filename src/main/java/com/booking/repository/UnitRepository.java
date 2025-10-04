package com.booking.repository;

import com.booking.entity.Unit;
import com.booking.entity.enums.AccommodationType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnitRepository extends JpaRepository<Unit, Long> {

  @Query("""
      SELECT DISTINCT u FROM Unit u
      WHERE (:numberOfRooms IS NULL OR u.numberOfRooms = :numberOfRooms)
        AND (:accommodationType IS NULL OR u.accommodationType = :accommodationType)
        AND (:floor IS NULL OR u.floor = :floor)
        AND (:minPrice IS NULL OR u.markupPrice >= :minPrice)
        AND (:maxPrice IS NULL OR u.markupPrice <= :maxPrice)
        AND NOT EXISTS (
          SELECT 1 FROM Booking b
          WHERE b.unit = u
            AND b.status IN ('PENDING', 'PAID')
            AND NOT (:endDate < b.startDate OR :startDate > b.endDate)
        )
      """)
  Page<Unit> findAvailableUnits(
      @Param("numberOfRooms") Integer numberOfRooms,
      @Param("accommodationType") AccommodationType accommodationType,
      @Param("floor") Integer floor,
      @Param("minPrice") BigDecimal minPrice,
      @Param("maxPrice") BigDecimal maxPrice,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      Pageable pageable
  );

  @Query("""
      SELECT COUNT(DISTINCT u.id) FROM Unit u
      WHERE NOT EXISTS (
        SELECT 1 FROM Booking b
        WHERE b.unit = u
          AND b.status IN ('PENDING', 'PAID')
          AND NOT (:endDate < b.startDate OR :startDate > b.endDate)
      )
      """)
  long countAvailableUnits(
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate
  );
}