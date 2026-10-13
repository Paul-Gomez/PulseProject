package com.pulse.channels.mapper;

import com.pulse.channels.dto.ChannelResponse;
import com.pulse.channels.entity.Channel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChannelMapper {

    // Lombok names the getter of "boolean isPrivate" isPrivate(), which MapStruct reads as a property called "private".
    @Mapping(target = "isPrivate", source = "private")
    ChannelResponse toResponse(Channel channel);
}
