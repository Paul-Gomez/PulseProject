package com.pulse.messages.mapper;

import com.pulse.messages.dto.MessageResponse;
import com.pulse.messages.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    // Attachments live in the files module; MessageService fills them in with MessageResponse#withAttachments.
    @Mapping(target = "attachments", ignore = true)
    MessageResponse toResponse(Message message);
}
