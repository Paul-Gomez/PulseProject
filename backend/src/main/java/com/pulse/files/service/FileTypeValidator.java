package com.pulse.files.service;

import com.pulse.common.exception.ApiException;
import com.pulse.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Decides the real type of an upload from its first bytes. The type the client declares is only accepted
 * if it matches, so renaming an .exe to .png does not get it past the whitelist.
 */
@Component
public class FileTypeValidator {

    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String ZIP = "application/zip";
    private static final Set<String> OFFICE_TYPES = Set.of(DOCX, XLSX);

    public String resolveMimeType(byte[] content, String declaredType) {
        String declared = declaredType == null ? "" : declaredType.split(";")[0].trim().toLowerCase();
        String detected = detect(content);

        if (detected == null) {
            throw notAllowed();
        }
        if (ZIP.equals(detected)) {
            // docx and xlsx are zip files, so the declared type decides between those two and nothing else.
            if (OFFICE_TYPES.contains(declared)) {
                return declared;
            }
            throw notAllowed();
        }
        if (!detected.equals(declared)) {
            throw notAllowed();
        }
        return detected;
    }

    private String detect(byte[] c) {
        if (startsWith(c, 0x89, 'P', 'N', 'G')) {
            return "image/png";
        }
        if (startsWith(c, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (startsWith(c, 'G', 'I', 'F', '8')) {
            return "image/gif";
        }
        if (c.length >= 12 && startsWith(c, 'R', 'I', 'F', 'F') && c[8] == 'W' && c[9] == 'E' && c[10] == 'B' && c[11] == 'P') {
            return "image/webp";
        }
        if (startsWith(c, '%', 'P', 'D', 'F')) {
            return "application/pdf";
        }
        if (startsWith(c, 'P', 'K', 0x03, 0x04)) {
            return ZIP;
        }
        return looksLikeText(c) ? "text/plain" : null;
    }

    private boolean startsWith(byte[] content, int... signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((content[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean looksLikeText(byte[] content) {
        int checked = Math.min(content.length, 1024);
        for (int i = 0; i < checked; i++) {
            if (content[i] == 0) {
                return false;
            }
        }
        return content.length > 0;
    }

    private ApiException notAllowed() {
        return new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                "File type not allowed. Allowed: png, jpeg, gif, webp, pdf, txt, docx, xlsx");
    }
}
