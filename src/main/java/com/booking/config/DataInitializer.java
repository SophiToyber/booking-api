package com.booking.config;

import com.booking.entity.Unit;
import com.booking.entity.User;
import com.booking.entity.enums.AccommodationType;
import com.booking.entity.enums.EventType;
import com.booking.repository.UnitRepository;
import com.booking.repository.UserRepository;
import com.booking.service.EventService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

  private final UnitRepository unitRepository;
  private final UserRepository userRepository;
  private final EventService eventService;
  private final Random random = new Random();

  private static final String[] DESCRIPTIONS = {
      "Modern apartment with excellent city views",
      "Spacious flat near public transport",
      "Cozy home in quiet neighborhood",
      "Luxury penthouse with premium amenities",
      "Budget-friendly studio in downtown",
      "Family-friendly house with backyard",
      "Newly renovated apartments",
      "Charming flat in historic district",
      "Contemporary home with smart features",
      "Elegant apartment near shopping center",
      "Comfortable living space with parking",
      "Bright and airy flat with balcony",
      "Exclusive residence in prime location",
      "Affordable housing near universities",
      "Premium apartment with gym access",
      "Traditional home with garden",
      "Stylish loft in trendy area",
      "Peaceful retreat away from city",
      "Central location with easy access",
      "Renovated space with modern design"
  };

  @Override
  @Transactional
  public void run(String... args) {
    long existingCount = unitRepository.count();

    if (existingCount >= 100) {
      log.info("Database already contains {} units, skipping random data generation",
          existingCount);
      return;
    }

    log.info("Starting generation of 90 random units...");

    List<User> users = userRepository.findAll();
    if (users.isEmpty()) {
      log.error("No users found in database. Cannot create units without users.");
      return;
    }

    List<Unit> randomUnits = new ArrayList<>();

    for (int i = 0; i < 90; i++) {
      Unit unit = generateRandomUnit(users);
      randomUnits.add(unit);
    }

    List<Unit> savedUnits = unitRepository.saveAll(randomUnits);

    savedUnits.forEach(unit ->
        eventService.logEvent(
            EventType.UNIT_CREATED,
            "Unit",
            unit.getId(),
            "Randomly generated unit on application startup: " + unit.getDescription()
        )
    );

    log.info("Successfully created {} random units. Total units in database: {}",
        savedUnits.size(), unitRepository.count());
  }

  private Unit generateRandomUnit(List<User> users) {
    int numberOfRooms = random.nextInt(6) + 1;

    AccommodationType[] types = AccommodationType.values();
    AccommodationType type = types[random.nextInt(types.length)];

    int floor = random.nextInt(21);

    BigDecimal basePrice = BigDecimal.valueOf(50 + random.nextDouble() * 450)
        .setScale(2, RoundingMode.HALF_UP);

    BigDecimal markupPrice = basePrice.multiply(new BigDecimal("1.15"))
        .setScale(2, RoundingMode.HALF_UP);

    String description = DESCRIPTIONS[random.nextInt(DESCRIPTIONS.length)] +
        " - " + numberOfRooms + " room(s), floor " + floor;

    User owner = users.get(random.nextInt(users.size()));

    return Unit.builder()
        .numberOfRooms(numberOfRooms)
        .accommodationType(type)
        .floor(floor)
        .basePrice(basePrice)
        .markupPrice(markupPrice)
        .description(description)
        .createdBy(owner)
        .build();
  }
}