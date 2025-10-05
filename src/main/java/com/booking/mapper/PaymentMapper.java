package com.booking.mapper;

import com.booking.entity.Payment;
import com.booking.web.dto.payment.PaymentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

  @Mapping(source = "booking.id", target = "bookingId")
  PaymentResponse toDto(Payment payment);
}