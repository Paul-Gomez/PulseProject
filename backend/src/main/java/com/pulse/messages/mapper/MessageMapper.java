package com.pulse.messages.mapper;

import com.pulse.messages.dto.MessageResponse;
import com.pulse.messages.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    // Attachments and reactions are filled in by MessageService, which loads them for a whole page at once.
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "reactions", ignore = true)
    MessageResponse toResponse(Message message);
}
