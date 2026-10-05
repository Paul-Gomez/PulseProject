package com.pulse.files.dto;

import java.io.InputStream;

public record AttachmentDownload(String fileName, String mimeType, long sizeBytes, InputStream content) {
}
