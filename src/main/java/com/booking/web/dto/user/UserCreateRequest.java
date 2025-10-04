package com.booking.web.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UserCreateRequest(
    @NotBlank
    String name
) {

}
