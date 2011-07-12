package com.tropo.server.recording;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.tropo.core.recording.StorageService;

/**
 * Dummy storage service. It will just return an URI to the actual file
 * 
 * @author martin
 *
 */
public class DefaultStorageService implements StorageService {

	@Override
	public URI store(File file) throws IOException {
		
		return file.toURI();
	}
}