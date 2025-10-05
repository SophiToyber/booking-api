# Booking System API

REST API for accommodation booking with automatic expiration and caching.

## Features

- **Unit Management**: Create and manage accommodation units (HOME/FLAT/APARTMENTS) with automatic 15% markup
- **Booking System**: Book units for date ranges with availability validation
- **Auto-Expiration**: Bookings auto-cancel after 15 minutes if not paid
- **Payment Processing**: Complete payments with automatic price calculation
- **Statistics**: Cached available units count (Redis)
- **Event Audit**: Complete operation history

## Tech Stack

Java 21 • Spring Boot 3.3.5 • PostgreSQL 15 • Redis 7 • Liquibase • Swagger/OpenAPI

## Quick Start

```bash
# Start database and cache
docker-compose up -d

# Run application
./gradlew bootRun
```

**Swagger UI:** http://localhost:8080/swagger-ui.html

## Testing

**HTTP files** (IntelliJ IDEA): `src/test/resources/http/`
- `user.http` - user operations
- `unit.http` - accommodation units
- `booking.http` - bookings workflow
- `payment.http` - payment processing
- `statistic.http` - statistics

**Tests:**
```bash
./gradlew test
```

## Initial Data

- 10 seed units (Liquibase)
- 90 random units (auto-generated on startup)