package com.pulse.messages.mapper;

import com.pulse.messages.dto.MessageResponse;
import com.pulse.messages.entity.Message;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    MessageResponse toResponse(Message message);
}
