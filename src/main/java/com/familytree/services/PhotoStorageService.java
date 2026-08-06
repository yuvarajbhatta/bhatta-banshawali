package com.familytree.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Plain disk storage for re-encoded photo bytes, shared by
 * PersonPhotoService (the Picture Album) and SignupService (a signup
 * applicant's profile photo, attached to a real Person only once
 * approved -- see VerificationReviewService). Every storageKey is
 * server-generated (UUID + ".jpg"), never derived from a client-
 * supplied filename -- callers are expected to have already run bytes
 * through ImageReencodeService before writing them here.
 */
@Service
public class PhotoStorageService {

    private final Path uploadsDirectory;

    public PhotoStorageService(@Value("${app.uploads.directory}") String uploadsDirectory) {
        this.uploadsDirectory = Path.of(uploadsDirectory).normalize();
    }

    /** Writes bytes under a fresh, server-generated key and returns it. */
    public String store(byte[] bytes) {
        String storageKey = UUID.randomUUID() + ".jpg";
        try {
            Files.createDirectories(uploadsDirectory);
            Files.write(resolveSafely(storageKey), bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the uploaded photo.", e);
        }
        return storageKey;
    }

    public byte[] read(String storageKey) {
        try {
            return Files.readAllBytes(resolveSafely(storageKey));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file is missing.");
        }
    }

    public long sizeOf(String storageKey) {
        try {
            return Files.size(resolveSafely(storageKey));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file is missing.");
        }
    }

    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveSafely(storageKey));
        } catch (IOException e) {
            // Best-effort -- an orphaned file left on disk is a cleanup
            // nuisance, not a reason to fail the delete the caller
            // actually asked for (the database row, which every query
            // respects).
        }
    }

    // storageKey is always server-generated, never client-controlled, so
    // this is defense-in-depth rather than a reachable path today --
    // cheap enough to keep given it resolves a database-stored value.
    private Path resolveSafely(String storageKey) {
        Path resolved = uploadsDirectory.resolve(storageKey).normalize();
        if (!resolved.startsWith(uploadsDirectory)) {
            throw new IllegalArgumentException("Invalid storage key.");
        }
        return resolved;
    }
}
