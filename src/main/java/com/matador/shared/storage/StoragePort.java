package com.matador.shared.storage;

/** Abstraction over object storage (Cloudflare R2 / S3) for presigned client uploads. */
public interface StoragePort {

    /** Generates a presigned PUT URL for {@code key} and the eventual public URL. */
    PresignedUpload presignUpload(String key, String contentType);

    record PresignedUpload(String key, String uploadUrl, String publicUrl) {}
}
