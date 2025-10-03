package com.booking.entity;

import com.booking.entity.enums.AccommodationType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "units")
public class Unit {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "unit_gen")
  @SequenceGenerator(
      name = "unit_gen",
      sequenceName = "unit_seq",
      allocationSize = 1
  )
  private Long id;

  private Integer numberOfRooms;

  private Integer floor;

  @Enumerated(EnumType.STRING)
  private AccommodationType accommodationType;

  private String description;

  private BigDecimal basePrice;

  private BigDecimal markupPrice;

  @ManyToOne
  @JoinColumn(name = "created_by")
  private User createdBy;

}
