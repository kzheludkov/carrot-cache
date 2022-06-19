/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.carrot.cache.io;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.carrot.cache.Cache;

public class FileIOEngine extends IOEngine {
  /** Logger */
  private static final Logger LOG = LogManager.getLogger(FileIOEngine.class);
  /**
   * Maps data segment Id to a disk file
   */
  Map<Integer, RandomAccessFile> dataFiles = new HashMap<Integer, RandomAccessFile>();
  
  protected DataReader fileDataReader;
  /**
   * Constructor
   * @param numSegments
   * @param segmentSize
   */
  public FileIOEngine(Cache parent) {
    super(parent);
    try {
      this.fileDataReader = this.config.getFileDataReader(this.cacheName);
      this.fileDataReader.init(this.cacheName);
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      LOG.fatal(e);
      throw new RuntimeException(e);
    }
  }
 
  /**
   * IOEngine subclass can override this method
   *
   * @param data data segment
   * @throws FileNotFoundException 
   */
  protected void saveInternal(Segment data) throws IOException {
    Runnable r = () -> {
      data.seal();
      int id = data.getId();
      try {
        OutputStream os = getOSFor(id);
        // Save to file
        data.save(os);
        // Close stream
        os.close();
        // Remove from RAM buffers
        removeFromRAMBuffers(data);
      } catch (IOException e) {
        LOG.error(e);
      }
    };
    new Thread(r).start();
  }
  
  @Override
  protected int getInternal(int sid, long offset, int size, byte[] key, 
      int keyOffset, int keySize, byte[] buffer, int bufOffset)  {
    try {
      return this.fileDataReader.read(this, key, keyOffset, keySize, sid, offset, size, buffer, bufOffset);
    } catch (IOException e) {
      LOG.error(e);
      // THIS IS FOR TESTING
      return NOT_FOUND;
    }
  }

  @Override
  protected int getInternal(int sid, long offset, int size, byte[] key, 
      int keyOffset, int keySize, ByteBuffer buffer) throws IOException {
    try {
      return this.fileDataReader.read(this, key, keyOffset, keySize, sid, offset, size, buffer);
    } catch (IOException e) {
      LOG.error(e);
      // THIS IS FOR TESTING
      return NOT_FOUND;
    }
  }

  @Override
  protected int getInternal(int sid, long offset, int size, long keyPtr, 
      int keySize, byte[] buffer, int bufOffset)  {
    try {
      return this.fileDataReader.read(this, keyPtr, keySize, sid, offset, size, buffer, bufOffset);
    } catch (IOException e) {
      LOG.error(e);
      // THIS IS FOR TESTING
      return NOT_FOUND;
    }
 }

  @Override
  protected int getInternal(int sid, long offset, int size, long keyPtr, 
      int keySize, ByteBuffer buffer) throws IOException {
    try {
      return this.fileDataReader.read(this, keyPtr, keySize, sid, offset, size, buffer);
    } catch (IOException e) {
      LOG.error(e);
      // THIS IS FOR TESTING
      return NOT_FOUND;
    }
  }
  
  
  OutputStream getOSFor(int id) throws FileNotFoundException {
    Path p = getPathForDataSegment(id);
    FileOutputStream fos = new FileOutputStream(p.toFile());
    return fos;
  }
  
  RandomAccessFile getOrCreateFileFor(int id) throws FileNotFoundException {
    RandomAccessFile file = dataFiles.get(id);
    if (file == null) {
      // open
      Path p = getPathForDataSegment(id);
      file = new RandomAccessFile(p.toFile(), "rw");
      dataFiles.put(id, file);
    }
    return file;
  }
  
  /**
   * Get file for by segment id
   * @param id segment id
   * @return file
   * @throws FileNotFoundException
   */
  public RandomAccessFile getFileFor(int id) {
    RandomAccessFile file = dataFiles.get(id);
    return file;
  }
  
  @Override
  public synchronized void releaseSegmentId(Segment data) {
    //TODO: is it a good idea to lock on file I/O?
    super.releaseSegmentId(data);
    // close and delete file
    RandomAccessFile f = dataFiles.get(data.getId());
    if (f != null) {
      try {
        f.close();
        Files.deleteIfExists(getPathForDataSegment(data.getId()));
      } catch(IOException e) {
        //TODO
      }
    }
  }
  
  /**
   * Get file prefetch buffer size
   * @return prefetch buffer size
   */
  public final int getFilePrefetchBufferSize() {
    String cacheName = this.parent.getName();
    return this.config.getFilePrefetchBufferSize(cacheName);
  }
  /**
   * Get file path for a data segment
   * @param id data segment id
   * @return path to a file
   */
  private Path getPathForDataSegment(int id) {
    return Paths.get(dataDir, getSegmentFileName(id));
  }

  @Override
  public SegmentScanner getScanner(Segment s) throws IOException {
    return this.fileDataReader.getSegmentScanner(this, s);
  }

  @Override
  public void save(OutputStream os) throws IOException {
    super.save(os);
  }

  @Override
  public void load(InputStream is) throws IOException {
    super.load(is);
  }
  
}
