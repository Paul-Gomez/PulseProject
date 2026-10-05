package com.pulse.notifications.mapper;

import com.pulse.notifications.dto.NotificationResponse;
import com.pulse.notifications.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}
