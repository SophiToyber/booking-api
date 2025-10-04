package com.booking.entity;

import com.booking.entity.enums.AccommodationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

  @Column(name = "number_of_rooms")
  private Integer numberOfRooms;

  @Column(name = "floor")
  private Integer floor;

  @Enumerated(EnumType.STRING)
  @Column(name = "accommodation_type", length = 255)
  private AccommodationType accommodationType;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "base_price", precision = 10, scale = 2)
  private BigDecimal basePrice;

  @Column(name = "markup_price", precision = 10, scale = 2)
  private BigDecimal markupPrice;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Unit unit)) {
      return false;
    }
    return id != null && id.equals(unit.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
