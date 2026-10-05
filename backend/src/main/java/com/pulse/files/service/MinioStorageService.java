package com.pulse.files.service;

import com.pulse.files.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class MinioStorageService implements StorageService {

    private final MinioClient client;
    private final String bucket;
    private volatile boolean bucketReady;

    public MinioStorageService(MinioClient client, MinioProperties properties) {
        this.client = client;
        this.bucket = properties.bucket();
    }

    @Override
    public void store(String key, byte[] content, String contentType) {
        ensureBucket();
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            throw new StorageException("Could not store file " + key, ex);
        }
    }

    @Override
    public InputStream load(String key) {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception ex) {
            throw new StorageException("Could not read file " + key, ex);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception ex) {
            throw new StorageException("Could not delete file " + key, ex);
        }
    }

    // The bucket is created on first use, so the app can start even if MinIO is still booting.
    private synchronized void ensureBucket() {
        if (bucketReady) {
            return;
        }
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            bucketReady = true;
        } catch (Exception ex) {
            throw new StorageException("Could not prepare bucket " + bucket, ex);
        }
    }
}
