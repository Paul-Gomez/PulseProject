package com.pulse.users.mapper;

import com.pulse.users.dto.UserResponse;
import com.pulse.users.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
