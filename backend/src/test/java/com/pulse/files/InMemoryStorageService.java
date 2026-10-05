package com.pulse.files;

import com.pulse.files.service.StorageException;
import com.pulse.files.service.StorageService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class InMemoryStorageService implements StorageService {

    private final Map<String, byte[]> files = new ConcurrentHashMap<>();

    @Override
    public void store(String key, byte[] content, String contentType) {
        files.put(key, content);
    }

    @Override
    public InputStream load(String key) {
        byte[] content = files.get(key);
        if (content == null) {
            throw new StorageException("File not found: " + key, null);
        }
        return new ByteArrayInputStream(content);
    }

    @Override
    public void delete(String key) {
        files.remove(key);
    }
}
