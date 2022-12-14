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

import java.io.IOException;

import com.carrot.cache.util.CacheConfig;
import com.carrot.cache.util.TestUtils;

public class TestFileIOEngineMultithreaded extends TestIOEngineMultithreadedBase {

  @Override
  public void setUp() throws IOException {
    super.setUp();
    this.numRecords = 100000;
    this.numThreads = 4;
  }
  
  @Override
  protected IOEngine getIOEngine() throws IOException {
    int segmentSize = 4 * 1024 * 1024;
    long cacheSize = 100 * segmentSize;
    CacheConfig conf = TestUtils.mockConfigForTests(segmentSize, cacheSize);
    this.engine = new FileIOEngine(conf);
    return this.engine;
  }
  
}
