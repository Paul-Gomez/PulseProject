package com.pulse.conversations.mapper;

import com.pulse.conversations.dto.DirectMessageResponse;
import com.pulse.conversations.entity.DirectMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DirectMessageMapper {

    DirectMessageResponse toResponse(DirectMessage message);
}
