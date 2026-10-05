package com.pulse.files.mapper;

import com.pulse.files.dto.AttachmentResponse;
import com.pulse.files.entity.Attachment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {

    AttachmentResponse toResponse(Attachment attachment);
}
