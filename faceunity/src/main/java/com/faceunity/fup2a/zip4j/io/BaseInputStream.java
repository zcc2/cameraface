package com.faceunity.fup2a.zip4j.io;

import com.faceunity.fup2a.zip4j.unzip.UnzipEngine;

import java.io.IOException;
import java.io.InputStream;


public abstract class BaseInputStream extends InputStream {

	public int read() throws IOException {
		return 0;
	}
	
	public void seek(long pos) throws IOException {
	}
	
	public int available() throws IOException {
		return 0;
	}
	
	public UnzipEngine getUnzipEngine() {
		return null;
	}

}
