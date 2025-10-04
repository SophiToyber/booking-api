package com.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booking.entity.Unit;
import com.booking.entity.User;
import com.booking.entity.enums.AccommodationType;
import com.booking.mapper.UnitMapper;
import com.booking.repository.UnitRepository;
import com.booking.repository.UserRepository;
import com.booking.web.dto.unit.UnitCreateRequest;
import com.booking.web.dto.unit.UnitResponse;
import com.booking.web.dto.unit.UnitUpdateRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnitService Unit Tests")
class UnitServiceTest {

  @Mock
  private UnitRepository unitRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UnitMapper unitMapper;

  @InjectMocks
  private UnitService unitService;

  private User mockUser;
  private Unit mockUnit;
  private UnitResponse mockResponse;

  @BeforeEach
  void setUp() {
    mockUser = User.builder()
        .id(1L)
        .name("John Doe")
        .build();

    mockUnit = Unit.builder()
        .id(1L)
        .numberOfRooms(2)
        .accommodationType(AccommodationType.FLAT)
        .floor(3)
        .basePrice(new BigDecimal("100.00"))
        .markupPrice(new BigDecimal("115.00"))
        .description("Modern flat in city center")
        .createdBy(mockUser)
        .build();

    mockResponse = new UnitResponse(
        1L,
        2,
        AccommodationType.FLAT,
        3,
        "Modern flat in city center",
        new BigDecimal("100.00"),
        new BigDecimal("115.00"),
        1L
    );
  }

  @Test
  @DisplayName("Should create unit with correct markup calculation")
  void create_ShouldCreateUnitWithMarkup() {
    // Given
    UnitCreateRequest request = new UnitCreateRequest(
        2,
        AccommodationType.FLAT,
        3,
        new BigDecimal("100.00"),
        "Modern flat in city center",
        1L
    );

    when(userRepository.getReferenceById(1L)).thenReturn(mockUser);
    when(unitRepository.save(any(Unit.class))).thenReturn(mockUnit);
    when(unitMapper.toDto(mockUnit)).thenReturn(mockResponse);

    // When
    UnitResponse result = unitService.create(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.numberOfRooms()).isEqualTo(2);
    assertThat(result.accommodationType()).isEqualTo(AccommodationType.FLAT);
    assertThat(result.basePrice()).isEqualByComparingTo(new BigDecimal("100.00"));
    assertThat(result.markupPrice()).isEqualByComparingTo(new BigDecimal("115.00"));
    assertThat(result.createdById()).isEqualTo(1L);

    verify(userRepository).getReferenceById(1L);
    verify(unitRepository).save(any(Unit.class));
    verify(unitMapper).toDto(mockUnit);
  }

  @Test
  @DisplayName("Should apply 15% markup correctly")
  void create_ShouldApply15PercentMarkup() {
    // Given
    UnitCreateRequest request = new UnitCreateRequest(
        3,
        AccommodationType.APARTMENTS,
        5,
        new BigDecimal("200.00"),
        "Luxury apartments",
        1L
    );

    ArgumentCaptor<Unit> unitCaptor = ArgumentCaptor.forClass(Unit.class);

    when(userRepository.getReferenceById(1L)).thenReturn(mockUser);
    when(unitRepository.save(any(Unit.class))).thenReturn(mockUnit);
    when(unitMapper.toDto(any())).thenReturn(mockResponse);

    // When
    unitService.create(request);

    // Then
    verify(unitRepository).save(unitCaptor.capture());
    Unit savedUnit = unitCaptor.getValue();

    assertThat(savedUnit.getBasePrice()).isEqualByComparingTo(new BigDecimal("200.00"));
    assertThat(savedUnit.getMarkupPrice()).isEqualByComparingTo(new BigDecimal("230.00"));
    assertThat(savedUnit.getNumberOfRooms()).isEqualTo(3);
    assertThat(savedUnit.getAccommodationType()).isEqualTo(AccommodationType.APARTMENTS);
    assertThat(savedUnit.getFloor()).isEqualTo(5);
    assertThat(savedUnit.getDescription()).isEqualTo("Luxury apartments");
    assertThat(savedUnit.getCreatedBy()).isEqualTo(mockUser);
  }

  @Test
  @DisplayName("Should calculate markup correctly for different base prices")
  void create_WithDifferentBasePrices_ShouldCalculateCorrectMarkup() {
    // Given
    UnitCreateRequest request = new UnitCreateRequest(
        1,
        AccommodationType.HOME,
        1,
        new BigDecimal("50.50"),
        "Cozy home",
        1L
    );

    ArgumentCaptor<Unit> unitCaptor = ArgumentCaptor.forClass(Unit.class);
    when(userRepository.getReferenceById(1L)).thenReturn(mockUser);
    when(unitRepository.save(any(Unit.class))).thenReturn(mockUnit);
    when(unitMapper.toDto(any())).thenReturn(mockResponse);

    // When
    unitService.create(request);

    // Then
    verify(unitRepository).save(unitCaptor.capture());
    Unit savedUnit = unitCaptor.getValue();
    assertThat(savedUnit.getMarkupPrice()).isEqualByComparingTo(new BigDecimal("58.08"));
  }

  @Test
  @DisplayName("Should get unit by id successfully")
  void getById_WhenUnitExists_ShouldReturnUnit() {
    // Given
    when(unitRepository.findById(1L)).thenReturn(Optional.of(mockUnit));
    when(unitMapper.toDto(mockUnit)).thenReturn(mockResponse);

    // When
    UnitResponse result = unitService.getById(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.numberOfRooms()).isEqualTo(2);
    assertThat(result.accommodationType()).isEqualTo(AccommodationType.FLAT);
    assertThat(result.description()).isEqualTo("Modern flat in city center");

    verify(unitRepository).findById(1L);
    verify(unitMapper).toDto(mockUnit);
  }

  @Test
  @DisplayName("Should throw exception when unit not found")
  void getById_WhenUnitNotExists_ShouldThrowException() {
    // Given
    when(unitRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> unitService.getById(999L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unit 999 not found");

    verify(unitRepository).findById(999L);
    verify(unitMapper, never()).toDto(any());
  }

  @Test
  @DisplayName("Should list units with pagination")
  void list_ShouldReturnPagedUnits() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    Unit unit2 = Unit.builder()
        .id(2L)
        .numberOfRooms(3)
        .accommodationType(AccommodationType.APARTMENTS)
        .floor(2)
        .basePrice(new BigDecimal("80.00"))
        .markupPrice(new BigDecimal("92.00"))
        .description("Cozy apartments")
        .createdBy(mockUser)
        .build();

    Page<Unit> unitPage = new PageImpl<>(List.of(mockUnit, unit2));

    UnitResponse response2 = new UnitResponse(
        2L, 3, AccommodationType.APARTMENTS, 2, "Cozy apartments",
        new BigDecimal("80.00"), new BigDecimal("92.00"), 1L
    );

    when(unitRepository.findAll(pageable)).thenReturn(unitPage);
    when(unitMapper.toDto(mockUnit)).thenReturn(mockResponse);
    when(unitMapper.toDto(unit2)).thenReturn(response2);

    // When
    Page<UnitResponse> result = unitService.list(pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent().get(0).id()).isEqualTo(1L);
    assertThat(result.getContent().get(1).id()).isEqualTo(2L);

    verify(unitRepository).findAll(pageable);
    verify(unitMapper, times(2)).toDto(any(Unit.class));
  }

  @Test
  @DisplayName("Should update unit successfully")
  void update_WhenUnitExists_ShouldUpdateUnit() {
    // Given
    UnitUpdateRequest request = new UnitUpdateRequest(
        4,
        AccommodationType.HOME,
        10,
        new BigDecimal("500.00"),
        "Luxury home with amazing view"
    );

    when(unitRepository.findById(1L)).thenReturn(Optional.of(mockUnit));
    when(unitMapper.toDto(mockUnit)).thenReturn(mockResponse);

    // When
    UnitResponse result = unitService.update(1L, request);

    // Then
    assertThat(result).isNotNull();

    // Проверяем, что unit был обновлен
    assertThat(mockUnit.getNumberOfRooms()).isEqualTo(4);
    assertThat(mockUnit.getAccommodationType()).isEqualTo(AccommodationType.HOME);
    assertThat(mockUnit.getFloor()).isEqualTo(10);
    assertThat(mockUnit.getBasePrice()).isEqualByComparingTo(new BigDecimal("500.00"));
    assertThat(mockUnit.getMarkupPrice()).isEqualByComparingTo(new BigDecimal("575.00"));
    assertThat(mockUnit.getDescription()).isEqualTo("Luxury home with amazing view");

    verify(unitRepository).findById(1L);
    verify(unitMapper).toDto(mockUnit);
  }

  @Test
  @DisplayName("Should recalculate markup when updating base price")
  void update_WithNewBasePrice_ShouldRecalculateMarkup() {
    // Given
    UnitUpdateRequest request = new UnitUpdateRequest(
        2,
        AccommodationType.FLAT,
        3,
        new BigDecimal("300.00"),  // Новая базовая цена
        "Updated flat"
    );

    when(unitRepository.findById(1L)).thenReturn(Optional.of(mockUnit));
    when(unitMapper.toDto(mockUnit)).thenReturn(mockResponse);

    // When
    unitService.update(1L, request);

    // Then - markup должен быть пересчитан: 300 * 1.15 = 345.00
    assertThat(mockUnit.getBasePrice()).isEqualByComparingTo(new BigDecimal("300.00"));
    assertThat(mockUnit.getMarkupPrice()).isEqualByComparingTo(new BigDecimal("345.00"));
  }

  @Test
  @DisplayName("Should throw exception when updating non-existent unit")
  void update_WhenUnitNotExists_ShouldThrowException() {
    // Given
    UnitUpdateRequest request = new UnitUpdateRequest(
        3, AccommodationType.APARTMENTS, 5, new BigDecimal("150.00"), "Updated"
    );

    when(unitRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> unitService.update(999L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unit 999 not found");

    verify(unitRepository).findById(999L);
    verify(unitRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should delete unit successfully")
  void delete_WhenUnitExists_ShouldDeleteUnit() {
    // Given
    when(unitRepository.findById(1L)).thenReturn(Optional.of(mockUnit));

    // When
    unitService.delete(1L);

    // Then
    verify(unitRepository).findById(1L);
    verify(unitRepository).delete(mockUnit);
  }

  @Test
  @DisplayName("Should throw exception when deleting non-existent unit")
  void delete_WhenUnitNotExists_ShouldThrowException() {
    // Given
    when(unitRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> unitService.delete(999L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unit 999 not found");

    verify(unitRepository).findById(999L);
    verify(unitRepository, never()).delete(any());
  }

  @ParameterizedTest
  @EnumSource(AccommodationType.class)
  @DisplayName("Should create unit with all accommodation types")
  void create_AllAccommodationTypes_ShouldWork(AccommodationType type) {
    // Given
    UnitCreateRequest request = new UnitCreateRequest(
        2, type, 3,
        new BigDecimal("100.00"),
        "Test " + type.name(),
        1L
    );

    ArgumentCaptor<Unit> captor = ArgumentCaptor.forClass(Unit.class);
    when(userRepository.getReferenceById(1L)).thenReturn(mockUser);
    when(unitRepository.save(any())).thenReturn(mockUnit);
    when(unitMapper.toDto(any())).thenReturn(mockResponse);

    // When
    unitService.create(request);

    // Then
    verify(unitRepository).save(captor.capture());
    assertThat(captor.getValue().getAccommodationType()).isEqualTo(type);

    reset(unitRepository, userRepository, unitMapper);
  }
}