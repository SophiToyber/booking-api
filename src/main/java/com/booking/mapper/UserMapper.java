package com.booking.mapper;

import com.booking.entity.User;
import com.booking.web.dto.user.UserCreateRequest;
import com.booking.web.dto.user.UserResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {

  User toEntity(UserCreateRequest req);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void update(@MappingTarget User user, UserCreateRequest req);

  UserResponse toDto(User user);
}
