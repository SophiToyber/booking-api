package com.booking.scheduler;

import com.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingScheduler {

  private final BookingService bookingService;


  @Scheduled(cron = "0 * * * * *")
  public void cancelExpiredBookings() {
    log.trace("Running expired bookings check");
    bookingService.cancelExpiredBookings();
  }
}