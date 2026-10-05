package com.pulse.files.service;

import java.io.InputStream;

/** Where file contents live. The rest of the app only knows this interface, not MinIO. */
public interface StorageService {

    void store(String key, byte[] content, String contentType);

    InputStream load(String key);

    void delete(String key);
}
