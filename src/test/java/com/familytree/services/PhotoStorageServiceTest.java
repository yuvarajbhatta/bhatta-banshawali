package com.familytree.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhotoStorageServiceTest {

    @TempDir
    private Path storageDir;

    @Test
    void storeWritesBytesUnderAFreshServerGeneratedKeyEndingInJpg() {
        PhotoStorageService service = new PhotoStorageService(storageDir.toString());

        String key = service.store(new byte[] {1, 2, 3});

        assertThat(key).endsWith(".jpg");
        assertThat(storageDir.resolve(key)).exists();
    }

    @Test
    void readReturnsExactlyWhatWasStored() {
        PhotoStorageService service = new PhotoStorageService(storageDir.toString());
        String key = service.store(new byte[] {5, 6, 7});

        assertThat(service.read(key)).containsExactly(5, 6, 7);
    }

    @Test
    void sizeOfMatchesTheStoredByteLength() {
        PhotoStorageService service = new PhotoStorageService(storageDir.toString());
        String key = service.store(new byte[] {1, 2, 3, 4, 5});

        assertThat(service.sizeOf(key)).isEqualTo(5);
    }

    @Test
    void deleteRemovesTheFileFromDisk() {
        PhotoStorageService service = new PhotoStorageService(storageDir.toString());
        String key = service.store(new byte[] {1});

        service.delete(key);

        assertThat(storageDir.resolve(key)).doesNotExist();
    }

    @Test
    void deleteOfAnAlreadyMissingKeyIsANoOp() {
        PhotoStorageService service = new PhotoStorageService(storageDir.toString());

        service.delete("never-existed.jpg");
    }

    @Test
    void readOfAMissingKeyThrowsNotFound() {
        PhotoStorageService service = new PhotoStorageService(storageDir.toString());

        assertThatThrownBy(() -> service.read("missing.jpg"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void rejectsAStorageKeyThatWouldEscapeTheUploadsDirectory() {
        PhotoStorageService service = new PhotoStorageService(storageDir.toString());

        assertThatThrownBy(() -> service.read("../outside.jpg"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createsTheUploadsDirectoryOnFirstStoreIfItDoesNotExistYet() throws Exception {
        Path notYetCreated = storageDir.resolve("nested/uploads");
        PhotoStorageService service = new PhotoStorageService(notYetCreated.toString());

        String key = service.store(new byte[] {1});

        assertThat(Files.exists(notYetCreated.resolve(key))).isTrue();
    }
}
