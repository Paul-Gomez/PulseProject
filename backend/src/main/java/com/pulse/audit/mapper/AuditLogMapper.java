package com.pulse.audit.mapper;

import com.pulse.audit.dto.AuditLogResponse;
import com.pulse.audit.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLog log);
}
