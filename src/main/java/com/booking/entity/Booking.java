package com.booking.entity;

import com.booking.entity.enums.BookingStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "bookings")
@Data
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "booking_gen")
  @SequenceGenerator(
      name = "booking_gen",
      sequenceName = "booking_seq",
      allocationSize = 1
  )
  private Long id;

  @ManyToOne
  @JoinColumn(name = "unit_id")
  private Unit unit;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  private LocalDate startDate;

  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  private BookingStatus status;

  private LocalDateTime createdAt;

  private LocalDateTime expiresAt;
}
