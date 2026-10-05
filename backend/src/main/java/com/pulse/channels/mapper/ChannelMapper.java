package com.pulse.channels.mapper;

import com.pulse.channels.dto.ChannelResponse;
import com.pulse.channels.entity.Channel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChannelMapper {

    ChannelResponse toResponse(Channel channel);
}
