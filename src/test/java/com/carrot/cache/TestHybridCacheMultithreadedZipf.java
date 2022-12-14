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
package com.carrot.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;

import com.carrot.cache.controllers.AdmissionController;
import com.carrot.cache.controllers.BaseAdmissionController;
import com.carrot.cache.controllers.LRCRecyclingSelector;
import com.carrot.cache.controllers.RecyclingSelector;
import com.carrot.cache.eviction.EvictionPolicy;
import com.carrot.cache.eviction.FIFOEvictionPolicy;
import com.carrot.cache.util.TestUtils;

public class TestHybridCacheMultithreadedZipf extends TestOffheapCacheMultithreadedZipf {
  
  int victim_segmentSize = 16 * 1024 * 1024;
  
  long victim_maxCacheSize = 1000L * victim_segmentSize;
  
  double victim_minActiveRatio = 0.5;
  
  int victim_scavengerInterval = 10; // seconds
  
  double victim_scavDumpBelowRatio = 0.5;
  
  boolean victim_promoteOnHit = true;
    
  protected Class<? extends EvictionPolicy> victim_epClz = FIFOEvictionPolicy.class;
  
  protected Class<? extends RecyclingSelector> victim_rsClz = LRCRecyclingSelector.class;
  
  protected Class<? extends AdmissionController> victim_acClz = BaseAdmissionController.class;
    
  protected Path victim_dataDirPath;
  
  protected Path victim_snapshotDirPath;
  
  @Override
  public void setUp() {
    // Parent cache
    this.offheap = true;
    this.numRecords = 1000000;
    this.numIterations = 2 * this.numRecords;
    this.numThreads = 4;
    this.minActiveRatio = 0.9;
    this.maxCacheSize = 100L * this.segmentSize;
    // victim cache
    this.victim_segmentSize = 4 * 1024 * 1024;
    this.victim_maxCacheSize = 1000L * this.victim_segmentSize;
    this.victim_minActiveRatio = 0.5;
    this.victim_scavDumpBelowRatio = 0.5;
    this.victim_scavengerInterval = 10;
    this.victim_promoteOnHit = true;
    this.victim_epClz = FIFOEvictionPolicy.class;
    this.victim_rsClz = LRCRecyclingSelector.class;
    this.victim_acClz = BaseAdmissionController.class;
    
  }
  
  @After  
  public void tearDown() throws IOException {
    Cache victim = cache.getVictimCache();
    System.out.printf("main cache: size=%d hit rate=%f items=%d\n", 
      cache.getStorageAllocated(), cache.getHitRate(), cache.size());
    
    System.out.printf("victim cache: size=%d hit rate=%f items=%d\n", 
      victim.getStorageAllocated(), victim.getHitRate(), victim.size());
    
    super.tearDown();
    // Delete temp data
    TestUtils.deleteDir(victim_dataDirPath);
    TestUtils.deleteDir(victim_snapshotDirPath);
  }
  
  protected  Cache createCache() throws IOException{
    Cache parent = super.createCache();
        
    String cacheName = "victim";
    // Data directory
    victim_dataDirPath = Files.createTempDirectory(null);
    String dataDir = victim_dataDirPath.toFile().getAbsolutePath();
    
    victim_snapshotDirPath = Files.createTempDirectory(null);
    String snapshotDir = victim_snapshotDirPath.toFile().getAbsolutePath();
    
    Cache.Builder builder = new Cache.Builder(cacheName);
    
    builder
      .withCacheDataSegmentSize(victim_segmentSize)
      .withCacheMaximumSize(victim_maxCacheSize)
      .withScavengerRunInterval(victim_scavengerInterval)
      .withScavengerDumpEntryBelowStart(victim_scavDumpBelowRatio)
      .withCacheEvictionPolicy(victim_epClz.getName())
      .withRecyclingSelector(victim_rsClz.getName())
      .withSnapshotDir(snapshotDir)
      .withDataDir(dataDir)
      .withMinimumActiveDatasetRatio(victim_minActiveRatio)
      .withVictimCachePromoteOnHit(victim_promoteOnHit)
      .withAdmissionController(victim_acClz.getName());
    
    Cache victim = builder.buildDiskCache();
    parent.setVictimCache(victim);

    return parent;
  }

}
