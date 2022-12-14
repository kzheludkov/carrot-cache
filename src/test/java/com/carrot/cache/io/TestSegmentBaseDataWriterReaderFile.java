/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.carrot.cache.io;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.carrot.cache.index.MemoryIndex;
import com.carrot.cache.index.MemoryIndex.Type;
import com.carrot.cache.util.TestUtils;

public class TestSegmentBaseDataWriterReaderFile extends IOTestBase{
  
  @Before
  public void setUp() {
    this.index = new MemoryIndex("default", Type.MQ);
    this.segmentSize = 4 * 1024 * 1024;
    this.numRecords = 10000;
    this.r = new Random();
    long seed = System.currentTimeMillis();
    r.setSeed(seed);
    /*DEBUG*/ System.out.println("r.seed="+ seed);
    segment = Segment.newSegment(this.segmentSize, 1, 1);
    segment.init("default");
    prepareData(this.numRecords);
    segment.setDataWriter(new BaseDataWriter());
  }
  
  @After
  public void tearDown() {
    super.tearDown();
    this.segment.dispose();
  }
  
  @Test
  public void testWritesBytes() throws IOException {
    int count = loadBytes();
    long expire = expires[count - 1];
    assertEquals(expire, segment.getInfo().getMaxExpireAt());
    verifyBytes(count);
    
    RandomAccessFile file = TestUtils.saveToFile(segment);
    
    DataReader reader = new BaseFileDataReader();
    FileIOEngine engine  = Mockito.mock(FileIOEngine.class);
    Mockito.when(engine.getSegmentById(Mockito.anyInt())).thenReturn(segment);
    Mockito.when(engine.getFileFor(Mockito.anyInt())).thenReturn(file);
    verifyBytesWithReader(count, reader, engine);
    verifyBytesWithReaderByteBuffer(count, reader, engine);

  }
  
  @Test
  public void testWritesMemory() throws IOException {
    int count = loadMemory(); 
    long expire = expires[count - 1];
    assertEquals(expire, segment.getInfo().getMaxExpireAt());
    verifyMemory(count);
    
    RandomAccessFile file = TestUtils.saveToFile(segment);
    
    DataReader reader = new BaseFileDataReader();
    FileIOEngine engine  = Mockito.mock(FileIOEngine.class);
    Mockito.when(engine.getSegmentById(Mockito.anyInt())).thenReturn(segment);
    Mockito.when(engine.getFileFor(Mockito.anyInt())).thenReturn(file);
    verifyMemoryWithReader(count, reader, engine);
    verifyMemoryWithReaderByteBuffer(count, reader, engine);

  }
  
  
  @Test
  public void testSegmentScanner() throws IOException {
    int count = loadBytes();
    RandomAccessFile file = TestUtils.saveToFile(segment);

    DataReader reader = new BaseFileDataReader();
    FileIOEngine engine  = Mockito.mock(FileIOEngine.class);
    Mockito.when(engine.getSegmentById(Mockito.anyInt())).thenReturn(segment);
    Mockito.when(engine.getFileFor(Mockito.anyInt())).thenReturn(file);
    Mockito.when(engine.getFilePrefetchBufferSize()).thenReturn(1024 * 1024);
    // Seal the segment
    segment.seal();

    SegmentScanner scanner = reader.getSegmentScanner(engine, segment);
    verifyScannerFile(scanner, count);
  }
  
}
