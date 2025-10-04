package com.booking.web.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
    @NotBlank
    String name
) {

}