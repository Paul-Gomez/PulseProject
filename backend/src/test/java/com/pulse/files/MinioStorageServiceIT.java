package com.pulse.files;

import com.pulse.files.config.MinioProperties;
import com.pulse.files.service.MinioStorageService;
import com.pulse.files.service.StorageException;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
class MinioStorageServiceIT {

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>("rustfs/rustfs:latest")
            
            .withEnv("RUSTFS_ACCESS_KEY", "pulse")
            .withEnv("RUSTFS_SECRET_KEY", "pulsesecret")
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/health").forPort(9000));

    private static MinioStorageService storage;

    @BeforeAll
    static void createStorage() {
        String endpoint = "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
        MinioClient client = MinioClient.builder().endpoint(endpoint).credentials("pulse", "pulsesecret").build();
        storage = new MinioStorageService(client, new MinioProperties(endpoint, "pulse", "pulsesecret", "test-bucket"));
    }

    @Test
    void storeThenLoad_returnsTheSameBytes() throws Exception {
        byte[] content = "contenido de prueba".getBytes(StandardCharsets.UTF_8);

        storage.store("ws/channel/file-1", content, "text/plain");

        assertArrayEquals(content, storage.load("ws/channel/file-1").readAllBytes());
    }

    @Test
    void loadAfterDelete_fails() {
        storage.store("ws/channel/file-2", "adios".getBytes(StandardCharsets.UTF_8), "text/plain");

        storage.delete("ws/channel/file-2");

        assertThrows(StorageException.class, () -> storage.load("ws/channel/file-2").readAllBytes());
    }
}
