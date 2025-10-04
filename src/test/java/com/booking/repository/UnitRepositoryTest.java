package com.booking.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.booking.entity.Unit;
import com.booking.entity.User;
import com.booking.entity.enums.AccommodationType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UnitRepository Tests")
class UnitRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private UnitRepository unitRepository;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .name("Test Owner")
        .build();
    testUser = entityManager.persistAndFlush(testUser);
  }

  @Test
  @DisplayName("Should save and find unit by id")
  void save_ShouldPersistUnit() {
    // Given
    Unit unit = Unit.builder()
        .numberOfRooms(2)
        .accommodationType(AccommodationType.FLAT)
        .floor(5)
        .basePrice(new BigDecimal("100.00"))
        .markupPrice(new BigDecimal("115.00"))
        .description("Modern flat in downtown")
        .createdBy(testUser)
        .build();

    // When
    Unit saved = unitRepository.save(unit);
    entityManager.flush();
    entityManager.clear();

    // Then
    Optional<Unit> found = unitRepository.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getNumberOfRooms()).isEqualTo(2);
    assertThat(found.get().getAccommodationType()).isEqualTo(AccommodationType.FLAT);
    assertThat(found.get().getFloor()).isEqualTo(5);
    assertThat(found.get().getDescription()).isEqualTo("Modern flat in downtown");
    assertThat(found.get().getBasePrice()).isEqualByComparingTo(new BigDecimal("100.00"));
    assertThat(found.get().getMarkupPrice()).isEqualByComparingTo(new BigDecimal("115.00"));
    assertThat(found.get().getCreatedBy().getId()).isEqualTo(testUser.getId());
  }

  @Test
  @DisplayName("Should return empty when unit not found")
  void findById_WhenNotExists_ShouldReturnEmpty() {
    // When
    Optional<Unit> found = unitRepository.findById(999L);

    // Then
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should find all units with pagination")
  void findAll_WithPageable_ShouldReturnPage() {
    // Given - create multiple units with different types
    createUnit(2, AccommodationType.FLAT, 3, "100.00", "115.00", "Flat 1");
    createUnit(3, AccommodationType.APARTMENTS, 2, "80.00", "92.00", "Apartments 1");
    createUnit(5, AccommodationType.HOME, 1, "250.00", "287.50", "Home 1");
    createUnit(2, AccommodationType.FLAT, 4, "120.00", "138.00", "Flat 2");
    createUnit(4, AccommodationType.HOME, 2, "200.00", "230.00", "Home 2");

    entityManager.flush();

    // When
    Page<Unit> page = unitRepository.findAll(PageRequest.of(0, 3));

    // Then
    assertThat(page.getContent()).hasSize(3);
    assertThat(page.getTotalElements()).isEqualTo(5);
    assertThat(page.getTotalPages()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should update unit")
  void update_ShouldModifyUnit() {
    // Given
    Unit unit = Unit.builder()
        .numberOfRooms(2)
        .accommodationType(AccommodationType.FLAT)
        .floor(5)
        .basePrice(new BigDecimal("100.00"))
        .markupPrice(new BigDecimal("115.00"))
        .description("Original description")
        .createdBy(testUser)
        .build();
    unit = entityManager.persistAndFlush(unit);
    entityManager.clear();

    // When
    Unit found = unitRepository.findById(unit.getId()).orElseThrow();
    found.setDescription("Updated description");
    found.setNumberOfRooms(3);
    found.setAccommodationType(AccommodationType.APARTMENTS);
    found.setBasePrice(new BigDecimal("150.00"));
    found.setMarkupPrice(new BigDecimal("172.50"));
    unitRepository.save(found);
    entityManager.flush();
    entityManager.clear();

    // Then
    Unit updated = unitRepository.findById(unit.getId()).orElseThrow();
    assertThat(updated.getDescription()).isEqualTo("Updated description");
    assertThat(updated.getNumberOfRooms()).isEqualTo(3);
    assertThat(updated.getAccommodationType()).isEqualTo(AccommodationType.APARTMENTS);
    assertThat(updated.getBasePrice()).isEqualByComparingTo(new BigDecimal("150.00"));
    assertThat(updated.getMarkupPrice()).isEqualByComparingTo(new BigDecimal("172.50"));
  }

  @Test
  @DisplayName("Should delete unit")
  void delete_ShouldRemoveUnit() {
    // Given
    Unit unit = Unit.builder()
        .numberOfRooms(2)
        .accommodationType(AccommodationType.FLAT)
        .floor(5)
        .basePrice(new BigDecimal("100.00"))
        .markupPrice(new BigDecimal("115.00"))
        .description("To be deleted")
        .createdBy(testUser)
        .build();
    unit = entityManager.persistAndFlush(unit);
    Long unitId = unit.getId();
    entityManager.clear();

    // When
    unitRepository.deleteById(unitId);
    entityManager.flush();

    // Then
    Optional<Unit> found = unitRepository.findById(unitId);
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should maintain relationship with User")
  void findById_ShouldLoadUserRelationship() {
    // Given
    Unit unit = Unit.builder()
        .numberOfRooms(2)
        .accommodationType(AccommodationType.FLAT)
        .floor(5)
        .basePrice(new BigDecimal("100.00"))
        .markupPrice(new BigDecimal("115.00"))
        .description("Test unit")
        .createdBy(testUser)
        .build();
    unit = entityManager.persistAndFlush(unit);
    entityManager.clear();

    // When
    Unit found = unitRepository.findById(unit.getId()).orElseThrow();

    // Then
    assertThat(found.getCreatedBy()).isNotNull();
    assertThat(found.getCreatedBy().getId()).isEqualTo(testUser.getId());
    assertThat(found.getCreatedBy().getName()).isEqualTo("Test Owner");
  }

  @Test
  @DisplayName("Should save all accommodation types correctly")
  void save_AllAccommodationTypes_ShouldWork() {
    // Given & When
    Unit home = createUnit(5, AccommodationType.HOME, 1, "250.00", "287.50", "Family home");
    Unit flat = createUnit(2, AccommodationType.FLAT, 3, "100.00", "115.00", "City flat");
    Unit apartments = createUnit(3, AccommodationType.APARTMENTS, 5, "150.00", "172.50",
        "Apartments");

    entityManager.flush();
    entityManager.clear();

    // Then
    assertThat(unitRepository.findById(home.getId()))
        .isPresent()
        .get()
        .extracting(Unit::getAccommodationType)
        .isEqualTo(AccommodationType.HOME);

    assertThat(unitRepository.findById(flat.getId()))
        .isPresent()
        .get()
        .extracting(Unit::getAccommodationType)
        .isEqualTo(AccommodationType.FLAT);

    assertThat(unitRepository.findById(apartments.getId()))
        .isPresent()
        .get()
        .extracting(Unit::getAccommodationType)
        .isEqualTo(AccommodationType.APARTMENTS);
  }

  @Test
  @DisplayName("Should handle decimal prices correctly")
  void save_DecimalPrices_ShouldPersistCorrectly() {
    // Given
    Unit unit = Unit.builder()
        .numberOfRooms(1)
        .accommodationType(AccommodationType.FLAT)
        .floor(1)
        .basePrice(new BigDecimal("123.45"))
        .markupPrice(new BigDecimal("141.97"))
        .description("Flat with decimal price")
        .createdBy(testUser)
        .build();

    // When
    Unit saved = unitRepository.save(unit);
    entityManager.flush();
    entityManager.clear();

    // Then
    Unit found = unitRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getBasePrice()).isEqualByComparingTo(new BigDecimal("123.45"));
    assertThat(found.getMarkupPrice()).isEqualByComparingTo(new BigDecimal("141.97"));
  }

  @Test
  @DisplayName("Should save unit with floor 0 (ground floor)")
  void save_GroundFloor_ShouldWork() {
    // Given
    Unit unit = Unit.builder()
        .numberOfRooms(2)
        .accommodationType(AccommodationType.FLAT)
        .floor(0)  // Ground floor
        .basePrice(new BigDecimal("100.00"))
        .markupPrice(new BigDecimal("115.00"))
        .description("Ground floor flat")
        .createdBy(testUser)
        .build();

    // When
    Unit saved = unitRepository.save(unit);
    entityManager.flush();
    entityManager.clear();

    // Then
    Unit found = unitRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getFloor()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should save unit with long description")
  void save_LongDescription_ShouldWork() {
    // Given
    String longDescription = "A".repeat(1000);  // TEXT column can handle this
    Unit unit = Unit.builder()
        .numberOfRooms(2)
        .accommodationType(AccommodationType.FLAT)
        .floor(5)
        .basePrice(new BigDecimal("100.00"))
        .markupPrice(new BigDecimal("115.00"))
        .description(longDescription)
        .createdBy(testUser)
        .build();

    // When
    Unit saved = unitRepository.save(unit);
    entityManager.flush();
    entityManager.clear();

    // Then
    Unit found = unitRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getDescription()).hasSize(1000);
    assertThat(found.getDescription()).isEqualTo(longDescription);
  }

  @Test
  @DisplayName("Should find units by pagination - second page")
  void findAll_SecondPage_ShouldReturnCorrectUnits() {
    // Given - create 10 units
    for (int i = 0; i < 10; i++) {
      AccommodationType type = switch (i % 3) {
        case 0 -> AccommodationType.HOME;
        case 1 -> AccommodationType.FLAT;
        default -> AccommodationType.APARTMENTS;
      };
      createUnit(2, type, i, "100.00", "115.00", "Unit " + i);
    }
    entityManager.flush();

    // When - request second page with size 3
    Page<Unit> page = unitRepository.findAll(PageRequest.of(1, 3));

    // Then
    assertThat(page.getNumber()).isEqualTo(1);  // Page number
    assertThat(page.getSize()).isEqualTo(3);    // Page size
    assertThat(page.getContent()).hasSize(3);   // Content size
    assertThat(page.getTotalElements()).isEqualTo(10);
    assertThat(page.getTotalPages()).isEqualTo(4);  // 10 elements / 3 per page = 4 pages
    assertThat(page.hasNext()).isTrue();
    assertThat(page.hasPrevious()).isTrue();
  }

  @Test
  @DisplayName("Should handle multiple units for same user")
  void save_MultipleUnitsForSameUser_ShouldWork() {
    // Given & When
    createUnit(2, AccommodationType.FLAT, 3, "100.00", "115.00", "Unit 1");
    createUnit(3, AccommodationType.APARTMENTS, 5, "150.00", "172.50", "Unit 2");
    createUnit(5, AccommodationType.HOME, 1, "250.00", "287.50", "Unit 3");

    entityManager.flush();
    entityManager.clear();

    // Then
    List<Unit> allUnits = unitRepository.findAll();
    assertThat(allUnits).hasSize(3);
    assertThat(allUnits)
        .extracting(Unit::getCreatedBy)
        .extracting(User::getId)
        .containsOnly(testUser.getId());
  }

  @Test
  @DisplayName("Should save HOME type with many rooms")
  void save_HomeWithManyRooms_ShouldWork() {
    // Given
    Unit home = Unit.builder()
        .numberOfRooms(10)
        .accommodationType(AccommodationType.HOME)
        .floor(2)
        .basePrice(new BigDecimal("500.00"))
        .markupPrice(new BigDecimal("575.00"))
        .description("Large family home")
        .createdBy(testUser)
        .build();

    // When
    Unit saved = unitRepository.save(home);
    entityManager.flush();
    entityManager.clear();

    // Then
    Unit found = unitRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getNumberOfRooms()).isEqualTo(10);
    assertThat(found.getAccommodationType()).isEqualTo(AccommodationType.HOME);
  }

  @Test
  @DisplayName("Should handle changing accommodation type")
  void update_ChangeAccommodationType_ShouldWork() {
    // Given
    Unit unit = createUnit(2, AccommodationType.FLAT, 3, "100.00", "115.00", "Original flat");
    entityManager.flush();
    entityManager.clear();

    // When - change FLAT to HOME
    Unit found = unitRepository.findById(unit.getId()).orElseThrow();
    found.setAccommodationType(AccommodationType.HOME);
    found.setNumberOfRooms(5);
    unitRepository.save(found);
    entityManager.flush();
    entityManager.clear();

    // Then
    Unit updated = unitRepository.findById(unit.getId()).orElseThrow();
    assertThat(updated.getAccommodationType()).isEqualTo(AccommodationType.HOME);
    assertThat(updated.getNumberOfRooms()).isEqualTo(5);
  }

  private Unit createUnit(int rooms, AccommodationType type, int floor,
      String basePrice, String markupPrice, String description) {
    Unit unit = Unit.builder()
        .numberOfRooms(rooms)
        .accommodationType(type)
        .floor(floor)
        .basePrice(new BigDecimal(basePrice))
        .markupPrice(new BigDecimal(markupPrice))
        .description(description)
        .createdBy(testUser)
        .build();
    return entityManager.persist(unit);
  }
}