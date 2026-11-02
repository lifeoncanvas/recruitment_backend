package com.licht.recruitment.common;

import java.io.IOException;
import java.io.InputStream;

/**
 * Where uploaded CVs and employer documents physically live.
 * Keys are relative paths such as "cv/uuid_name.pdf"; callers never see disk paths or bucket URLs.
 */
public interface FileStore {

    void save(String key, InputStream content, long size, String contentType) throws IOException;

    /** Opens the stored file, or returns null when nothing is stored under the key. */
    StoredFile open(String key) throws IOException;

    record StoredFile(InputStream content, long size) {}
}
