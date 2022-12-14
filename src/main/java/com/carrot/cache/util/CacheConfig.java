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
package com.carrot.cache.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import com.carrot.cache.controllers.AdmissionController;
import com.carrot.cache.controllers.RecyclingSelector;
import com.carrot.cache.controllers.ThroughputController;
import com.carrot.cache.eviction.EvictionPolicy;
import com.carrot.cache.eviction.SLRUEvictionPolicy;
import com.carrot.cache.expire.ExpireSupport;
import com.carrot.cache.index.AQIndexFormat;
import com.carrot.cache.index.IndexFormat;
import com.carrot.cache.index.BaseIndexFormat;
import com.carrot.cache.io.DataReader;
import com.carrot.cache.io.DataWriter;

@SuppressWarnings("deprecation")
public class CacheConfig {

  /* List of all caches logical names, comma-separated*/
  public final static String CACHES_NAME_LIST_KEY = "caches.name.list";
  
  /* By default we have only one cache */
  public final static String DEFAULT_CACHES_NAME_LIST ="cache"; // only one cache
  
  /* Caches types ('offheap', 'file' only supported), comma-separated */
  public final static String CACHES_TYPES_LIST_KEY ="caches.types.list";
  
  /* By default cache type is offheap */
  public final static String DEFAULT_CACHES_TYPES_LIST = "offheap";
  
  /* 
   * Cache victim name. If cache name is C1, then to lookup for its victim name 
   * we must request 'C1.victim.name' property value 
   */
  public final static String CACHE_VICTIM_NAME_KEY = "victim.name";
  
  
  /* File name for cache snapshot data */
  public final static String CACHE_SNAPSHOT_NAME = "cache.data";
  
  /* File name for admission controller snapshot data */
  public final static String ADMISSION_CONTROLLER_SNAPSHOT_NAME = "ac.data";
  
  /* File name for throughput controller snapshot data */
  public final static String THROUGHPUT_CONTROLLER_SNAPSHOT_NAME = "tc.data";
  
  /* File name for recycling selector snapshot data */
  public final static String RECYCLING_SELECTOR_SNAPSHOT_NAME = "rc.data";
  
  /* File name for admission queue snapshot data */
  public final static String ADMISSION_QUEUE_SNAPSHOT_NAME = "aq.data";

  /* File name for scavenger statistics snapshot data */
  public final static String SCAVENGER_STATS_SNAPSHOT_NAME = "scav.data";
  
  /* File name for cache engine snapshot data */
  public final static String CACHE_ENGINE_SNAPSHOT_NAME = "engine.data";
  
  /* Default cache configuration file name */
  public final static String DEFAULT_CACHE_CONFIG_FILE_NAME = "cache.conf";
  
  /* Default cache configuration directory name - the only directory for all caches */
  public final static String DEFAULT_CACHE_CONFIG_DIR_NAME = "conf";
  
  /* Cache snapshot directory - where to save index and statistics info */
  public final static String CACHE_SNAPSHOT_DIR_NAME_KEY = "snapshot.dir.name";
  
  /* Default cache snapshot directory name */
  public final static String DEFAULT_CACHE_SNAPSHOT_DIR_NAME = "snapshot";
  
  /* Cache data directory - where to save cached data */
  public final static String CACHE_DATA_DIR_NAME_KEY = "data.dir.name";
  
  /* Default cache data directory name */
  public final static String DEFAULT_CACHE_DATA_DIR_NAME = "data";
  
  //TODO - location for data and system snapshots
  
  /* Data segment size */
  public static final String CACHE_SEGMENT_SIZE_KEY = "cache.data.segment.size";
  
  /* Maximum storage limit to use for cache */
  public static final String CACHE_MAXIMUM_SIZE_KEY = "cache.data.max.size";
  
  /* When to start GC (garbage collection) - size of the cache as a fraction of the maximum cache size */
  public static final String SCAVENGER_START_RUN_RATIO_KEY = "scavenger.start.ratio";

  /* When to stop GC (garbage collection) - size of the cache as a fraction of the maximum cache size */
  public static final String SCAVENGER_STOP_RUN_RATIO_KEY = "scavenger.stop.ratio";
  
  /* Discard cached entry if it in this lower percentile - start value */
  public static final String SCAVENGER_DUMP_ENTRY_BELOW_START_KEY = "scavenger.dump.entry.below.start";

  /* Discard cached entry if it in this lower percentile - stop value (maximum) */
  public static final String SCAVENGER_DUMP_ENTRY_BELOW_STOP_KEY = "scavenger.dump.entry.below.stop";
  
  /* Adjustment step for scavenger */
  public static final String SCAVENGER_DUMP_ENTRY_BELOW_STEP_KEY = "scavenger.dump.entry.below.step";
  
  /* Scavenger number of segment processed before stall mode activated*/
  public static final String SCAVENGER_MAX_SEGMENTS_BEFORE_STALL_KEY = "scavenger.max.segments.before.stall";
    
  /* Number of popularity ranks ( default - 8) */
  public static final String CACHE_POPULARITY_NUMBER_RANKS_KEY = "cache.popularity.number.ranks";
  
  /** Keep active data set fraction above this threshold */
  public static final String CACHE_MINIMUM_ACTIVE_DATA_SET_RATIO_KEY = 
      "cache.minimum.active.dataset.ratio"; 
  /** IO storage pool size */
  public static final String CACHE_IO_STORAGE_POOL_SIZE_KEY = "cache.storage.pool.size";
  
  /* New item insertion point for SLRU (segment number 1- based)*/
  public static final String SLRU_CACHE_INSERT_POINT_KEY = "eviction.slru.insert.point";

  /* Number of segments in SLRU eviction policy */
  public static final String SLRU_NUMBER_SEGMENTS_KEY = "eviction.slru.number.segments";
  
  /* Admission Queue start size in fraction of a full cache size */
  public static final String ADMISSION_QUEUE_START_SIZE_RATIO_KEY = "admission.queue.start.size";
  
  /* Admission Queue minimum size in fraction of a full cache size */
  public static final String ADMISSION_QUEUE_MIN_SIZE_RATIO_KEY = "admission.queue.min.size";
  
  /* Admission Queue maximum size in fraction of a full cache size */
  public static final String ADMISSION_QUEUE_MAX_SIZE_RATIO_KEY = "admission.queue.max.size";
  
  
  /* Readmission evicted item to AQ minimum hit count threshold */
  public static final String READMISSION_HIT_COUNT_MIN_KEY = "cache.readmission.hit.count.min";
  
  /* Cumulative average write rate limit  (bytes/sec) */
  public static final String CACHE_WRITE_RATE_LIMIT_KEY = "cache.write.avg.rate.limit";
  
  /** Promotion on hit from victim to main cache */
  public static final String CACHE_VICTIM_PROMOTION_ON_HIT_KEY = "cache.victim.promotion.on.hit";
  
  /*
   * Some file systems : ext4, xfs, APFS etc supports sparse files and so called 
   * "hole punching" - discarding  regions of files. We use different algorithm of compaction when file system 
   *  supports these features. Default: false.
   */
  public static final String SPARSE_FILES_SUPPORT_KEY = "sparse.files.support";
  
  /*
   * Index starting number of slots power of 2 - L ( N = 2**L) N - number of slots 
   */
  public static final String START_INDEX_NUMBER_OF_SLOTS_POWER_KEY = "index.slots.power";
  
  /*
   * Cache write throughput check interval key  
   */
  public static final String THROUGHPUT_CHECK_INTERVAL_SEC_KEY = "throughput.check.interval.sec"; 
  
  /*
   * Scavenger run interval key (seconds)  
   */
  public static final String SCAVENGER_RUN_INTERVAL_SEC_KEY = "scavenger.run.interval.sec"; 
  
  /*
   * Cache write throughput controller tolerance limit
   */
  public static final String THROUGHPUT_CONTROLLER_TOLERANCE_LIMIT_KEY = "throughput.tolerance.limit";
  
  /*
   *  Throughput controller number of adjustment steps
   */
  public static final String THROUGHPUT_CONTROLLER_ADJUSTMENT_STEPS_KEY = "throughput.adjustment.steps";
  
  /**
   * Cache write maximum waiting time in milliseconds
   */
  
  public static final String CACHE_WRITES_MAX_WAIT_TIME_MS_KEY = "cache.writes.max.wait.time.ms";
  
  /**
   * Does index support memory embedding
   */
  public static final String INDEX_DATA_EMBEDDED_KEY = "index.data.embedded";
  
  /**
   * Maximum data size to embed   
   **/
  public static final String INDEX_DATA_EMBEDDED_SIZE_KEY = "index.data.embedded.size";
  
  /* Class name for main queue index format implementation */
  public static final String INDEX_FORMAT_MAIN_QUEUE_IMPL_KEY = "index.format.main.queue.impl";
  
  /* Class name for admission queue index format implementation */
  public static final String INDEX_FORMAT_ADMISSION_QUEUE_IMPL_KEY = "index.format.admission.queue.impl";

  /* Class name for cache eviction policy implementation */
  public static final String CACHE_EVICTION_POLICY_IMPL_KEY = "cache.eviction.policy.impl";
  
  /* Class name for cache admission controller implementation */
  public static final String CACHE_ADMISSION_CONTROLLER_IMPL_KEY = "cache.admission.controller.impl";
  
  /* Class name for cache throughput controller implementation */
  public static final String CACHE_THROUGHPUT_CONTROLLER_IMPL_KEY = "cache.throughput.controller.impl";
  
  /* Class name for cache recycling controller implementation */
  public static final String CACHE_RECYCLING_SELECTOR_IMPL_KEY = "cache.recycling.selector.impl";
  
  /* Class name for cache data appender implementation */
  public static final String CACHE_DATA_WRITER_IMPL_KEY = "cache.data.writer.impl";
  
  /* Class name for cache data reader implementation (RAM)*/
  public static final String CACHE_MEMORY_DATA_READER_IMPL_KEY = "cache.memory.data.reader.impl";
  
  /* Class name for cache data reader implementation (File)*/
  public static final String CACHE_FILE_DATA_READER_IMPL_KEY = "cache.file.data.reader.impl";
  
  /* Block writer block size key */
  public static final String CACHE_BLOCK_WRITER_BLOCK_SIZE_KEY = "cache.block.writer.block.size";
  
  /* File prefetch buffer size */
  public static final String FILE_PREFETCH_BUFFER_SIZE_KEY = "file.prefetch.buffer.size";
  
  /* Cache expiration support implementation key */
  public static final String CACHE_EXPIRE_SUPPORT_IMPL_KEY = "cache.expire.support.impl";
  
  /* Random admission controller ratio start key */
  public static final String CACHE_RANDOM_ADMISSION_RATIO_START_KEY = "cache.random.admission.ratio.start";
  
  /* Random admission controller ratio key */
  public static final String CACHE_RANDOM_ADMISSION_RATIO_STOP_KEY = "cache.random.admission.ratio.stop";
  
  /* For expiration  based admission controller */
  public static final String CACHE_EXPIRATION_BIN_START_VALUE_KEY = "cache.expire.start.bin.value";
  
  /* Bin value multiplier */
  public static final String CACHE_EXPIRATION_MULTIPLIER_VALUE_KEY = "cache.expire.multiplier.value";
  
  /* Eviction disabled mode */
  public static final String CACHE_EVICTION_DISABLED_MODE_KEY = "cache.eviction.disabled.mode";
  
  /* Rolling Window Counter number of bins*/
  public static final String CACHE_ROLLING_WINDOW_COUNTER_BINS_KEY = "cache.rwc.bins";
  
  /* Rolling Window Counter window duration in seconds */
  public static final String CACHE_ROLLING_WINDOW_COUNTER_DURATION_KEY = "cache.rwc.window";
  
  
  /* Defaults section */
  
  public static final long DEFAULT_CACHE_SEGMENT_SIZE = 4 * 1024 * 1024;

  public static final long DEFAULT_CACHE_MAXIMUM_SIZE = 0; // Unlimited
  
  public static final double DEFAULT_SCAVENGER_START_RUN_RATIO = 0.95;

  public static final double DEFAULT_SCAVENGER_STOP_RUN_RATIO =
      0.9; // Set it to 0 for continuous scavenger run

  /**
   * During scavenger run, all expired entries as well as entries with popularity below this config
   * value will be dumped, all others will be rewritten back to a cache. THis is the start value
   */
  public static final double DEFAULT_SCAVENGER_DUMP_ENTRY_BELOW_START = 0.1;
  
  /**
   * During scavenger run, all expired entries as well as entries with popularity below this config
   * value will be dumped, all others will be rewritten back to a cache. This is the stop value
   */
  public static final double DEFAULT_SCAVENGER_DUMP_ENTRY_BELOW_STOP = 0.5;
  
  /**
   * Default value for scavenger adjustment step
   */
  public static final double DEFAULT_SCAVENGER_DUMP_ENTRY_BELOW_STEP = 0.1;
  
  /**
   * 
   * Default value for scavenger max segments before stall mode activated
   * 
   * */
  public static final int DEFAULT_SCAVENGER_MAX_SEGMENTS_BEFORE_STALL = 10;
  /**
   * Limits write speed during scavenger run to 0.9 of scavenger cleaning memory rate Suppose
   * Scavenger frees memory with a rate of 900MB/sec. Incoming cache write requests will be limited
   * to a 900*0.9 MS/sec = 810MB/sec. This is necessary to guarantee that we won't get OOM errors
   * (or swapping)
   */
  public static final double DEFAULT_WRITES_LIMIT_RATIO = 0.9;

  /** Default number of popularity ranks for data items in the cache */
  public static final int DEFAULT_CACHE_POPULARITY_NUMBER_RANKS = 8;
  
  /** Number of segments in segmented-LRU cache */
  public static final int DEFAULT_SLRU_NUMBER_SEGMENTS = 8;
  
  /** Default insertion point for SLRU - head of nth segments - 4*/
  public static final int DEFAULT_SLRU_CACHE_INSERT_POINT = 4;
  
  /** Default segment size */
  public static final long DEFAULT_CACHE_DISK_SEGMENT_SIZE = 256 * 1024 * 1024; // 64MB

  /* Default re-admission hit count min*/
  public final static int DEFAULT_READMISSION_HIT_COUNT_MIN = 1;
  
  /* Default sparse files support */
  public final static boolean DEFAULT_SPARSE_FILES_SUPPORT = false;
  
  /* Default AQ (admission queue) start size a fraction of a cache size */
  public final static double DEFAULT_ADMISSION_QUEUE_START_SIZE_RATIO = 0.5;
  
  /* Default AQ (admission queue) minimum size a fraction of a cache size */
  public final static double DEFAULT_ADMISSION_QUEUE_MIN_SIZE_RATIO = 0.1;
  
  /* Default AQ (admission queue) maximum size a fraction of a cache size */
  public final static double DEFAULT_ADMISSION_QUEUE_MAX_SIZE_RATIO = 0.5;

  /* Default disk limit - 0 unlimited*/
  public static final long DEFAULT_DISK_LIMIT = 0; // Unlimited
  
  /* Default size as a power indicator of 2 for index N = 2**10 - start */ 
  public static final int DEFAULT_START_INDEX_NUMBER_OF_SLOTS_POWER = 10;
  
  /* Default incremental index rehashing */
  public static final boolean DEFAULT_INCREMENTRAL_INDEX_REHASHING = false;

  /* Default throughput check interval - 1 hour*/
  public static final long DEFAULT_THROUGHPUT_CHECK_INTERVAL_SEC = 3600;// 1h
  
  /* Default Scavenger run interval - 1 min*/
  public static final long DEFAULT_SCAVENGER_RUN_INTERVAL_SEC = 60;// 1min
  
  /* Default throughput controller tolerance limit*/
  public static final double DEFAULT_THROUGHPUT_CONTROLLER_TOLERANCE_LIMIT = 0.05;
  
  /* Cache write maximum wait time in milliseconds */
  public static final long DEFAULT_CACHE_WRITES_MAX_WAIT_TIME_MS = 10;
  
  /* Default cache write rate limit */
  public static final long DEFAULT_CACHE_WRITE_RATE_LIMIT = 50 * 1024 * 1024;
  
  /* Default number of adjustment steps */
  public static final int DEFAULT_THROUGHPUT_CONTROLLER_ADJUSTMENT_STEPS = 10;
  
  /* Default for index data embedding */
  public final static boolean DEFAULT_INDEX_DATA_EMBEDDED = false;
  
  /* Default index data embedding maximum size */
  public final static int DEFAULT_INDEX_DATA_EMBEDDED_SIZE = 100;
  
  /** Default implementation class for recycling selector */
  public final static String DEFAULT_CACHE_RECYCLING_SELECTOR_IMPL = 
      "com.carrot.cache.controllers.MinAliveRecyclingSelector";
  /** Default */
  public final static String DEFAULT_CACHE_DATA_WRITER_IMPL = 
      "com.carrot.cache.io.BaseDataWriter";
  
  /** Default reader for RAM segments */
  public final static String DEFAULT_CACHE_MEMORY_DATA_READER_IMPL = 
      "com.carrot.cache.io.BaseMemoryDataReader";
  
  /** Default reader for file segments */
  public final static String DEFAULT_CACHE_FILE_DATA_READER_IMPL = 
      "com.carrot.cache.io.BaseFileDataReader";
  
  /** Default expire support implementation */
  public final static String DEFAULT_CACHE_EXPIRE_SUPPORT_IMPL = 
      "com.carrot.cache.expire.ExpireSupportSecondsMinutes";
  
  /** Default block writer block size */
  public final static int DEFAULT_CACHE_BLOCK_WRITER_BLOCK_SIZE = 4096;
  
  /** Increase this size if you want to cache items larger*/
  public final static int DEFAULT_FILE_PREFETCH_BUFFER_SIZE = 4 * 1024 * 1024;
  
  /** Default value for random admission adjustment start */
  public static final double DEFAULT_CACHE_RANDOM_ADMISSION_RATIO_START = 1.d;
  
  /** Default value for random admission adjustment stop */
  public static final double DEFAULT_CACHE_RANDOM_ADMISSION_RATIO_STOP = 0.d;
  
  /** Default value for start bin limit in seconds */
  public static final long DEFAULT_CACHE_EXPIRATION_BIN_START_VALUE = 60;// in seconds
  
  public static final double DEFAULT_CACHE_EXPIRATION_MULTIPLIER_VALUE = 2;
  
  /* Default minimum active data set ratio */
  public final static double DEFAULT_CACHE_MINIMUM_ACTIVE_DATA_SET_RATIO = 0.9;
  
  /* Default IO pool size */
  public final static int DEFAULT_CACHE_IO_STORAGE_POOL_SIZE = 32;
  
  /* Default cache disabled mode */
  public final static boolean DEFAULT_CACHE_EVICTION_DISABLED_MODE = false;
  
  /* Rolling window counter bins default */
  public final static int DEFAULT_CACHE_ROLLING_WINDOW_COUNTER_BINS = 60;
  
  /* Rolling window counter window size in seconds default */
  public final static int DEFAULT_CACHE_ROLLING_WINDOW_COUNTER_DURATION = 3600;
  
  /* Victim cache promotion on hit default value*/
  public final static boolean DEFAULT_CACHE_VICTIM_PROMOTION_ON_HIT = true;
  
  // Statics
  static CacheConfig instance;

  public static CacheConfig getInstance() {
    if (instance != null) {
      return instance;
    }
    synchronized (CacheConfig.class) {
      if (instance != null) {
        return instance;
      }
      instance = new CacheConfig();
    }
    return instance;
  }

  public static CacheConfig getInstance(String file) throws IOException {
    if (instance != null) {
      return instance;
    }
    synchronized (CacheConfig.class) {
      if (instance != null) {
        return instance;
      }
      instance = new CacheConfig(file);
    }
    return instance;
  }

  // Instance
  Properties props = new Properties();

  /** Default constructor */
  private CacheConfig() {
  }

  /**
   * Mockito - friendly
   */
  public void init() {
    this.props = new Properties();
  }
  
  /**
   * Constructor from file
   *
   * @param file configuration file
   * @throws IOException
   */
  private CacheConfig(String file) throws IOException {
    this();
    if (file != null) {
      FileInputStream fis = new FileInputStream(file);
      props.load(fis);
      fis.close();
    }
  }

  /**
   * Constructor from properties
   *
   * @param props
   */
  CacheConfig(Properties props) {
    this.props = props;
  }

  /**
   * Load configuration from an input stream
   * @param is input stream
   * @return cache configuration object
   * @throws IOException
   */
  public static CacheConfig load(InputStream is) throws IOException {
    Properties props = new Properties();
    props.load(is);
    return new CacheConfig(props);
  }
  
  /**
   * Save configuration
   * @param os output stream
   * @throws IOException
   */
  public void save(OutputStream os) throws IOException {
    this.props.store(os, "Cache configuration properties");
  }
  
  public double getDoubleProperty(String name, double defValue) {
    String value = props.getProperty(name);
    if (value == null) return defValue;
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      // TODO log error
      e.printStackTrace();
    }
    return defValue;
  }

  public long getLongProperty(String name, long defValue) {
    String value = props.getProperty(name);
    if (value == null) return defValue;
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      // TODO log error
      e.printStackTrace();
    }
    return defValue;
  }

  public String getProperty(String name, String defValue) {
    String value = props.getProperty(name);
    if (value == null) return defValue;
    return value;
  }
  
  public boolean getBooleanProperty(String name, boolean defValue) {
    String value = props.getProperty(name);
    if (value == null) return defValue;
    try {
      return Boolean.parseBoolean(value);
    } catch (NumberFormatException e) {
      // TODO log error
      e.printStackTrace();
    }
    return defValue;
  }
  
  /****************************************************************
   * GETTERS SECTION
   */

  /**
   * Get maximum size for a cache
   * @param cacheName cache name
   * @return memory limit (0 - no limit)
   */
  public long getCacheMaximumSize(String cacheName) {
    String value = props.getProperty(cacheName + "."+ CACHE_MAXIMUM_SIZE_KEY);
    if (value != null) {
      return Long.parseLong(value);
    }
    return getLongProperty(CACHE_MAXIMUM_SIZE_KEY, DEFAULT_CACHE_MAXIMUM_SIZE);
  }

  /**
   * Set cache maximum size
   * @param cacheName cache name
   * @param size maximum size in bytes
   */
  public void setCacheMaximumSize(String cacheName, long size) {
    props.setProperty(cacheName + "."+ CACHE_MAXIMUM_SIZE_KEY, Long.toString(size));
  }
  
  /**
   * Get segment size for a cache
   * @param cacheName cache name
   * @return segment size
   */
  public long getCacheSegmentSize(String cacheName) {
    String value = props.getProperty(cacheName + "."+ CACHE_SEGMENT_SIZE_KEY);
    if (value != null) {
      return Long.parseLong(value);
    }
    return getLongProperty(CACHE_SEGMENT_SIZE_KEY, DEFAULT_CACHE_SEGMENT_SIZE);
  }

  /**
   * Set cache segment size
   * @param cacheName cache name
   * @param size segment size in bytes
   */
  public void setCacheSegmentSize(String cacheName, long size) {
    props.setProperty(cacheName + "."+ CACHE_SEGMENT_SIZE_KEY, Long.toString(size));
  }
  
  /**
   * Gets scavenger start memory ratio for a cache
   * @param cache name
   * @return start ratio
   */
  public double getScavengerStartMemoryRatio(String cacheName) {
    String value = props.getProperty(cacheName + "."+ SCAVENGER_START_RUN_RATIO_KEY);
    if (value != null) {
      return Double.parseDouble(value);
    }
    return getDoubleProperty(SCAVENGER_START_RUN_RATIO_KEY, DEFAULT_SCAVENGER_START_RUN_RATIO);
  }

  /**
   * Set scavenger start memory ratio
   * @param cacheName cache name
   * @param ratio memory ration relative to a maximum cache size
   */
  public void setScavengerStartMemoryRatio(String cacheName, double ratio) {
    props.setProperty(cacheName+ "."+ SCAVENGER_START_RUN_RATIO_KEY, Double.toString(ratio));
  }
  
  /**
   * Gets scavenger stop memory ratio
   * @param cacheName cache name
   * @return stop ratio
   */
  public double getScavengerStopMemoryRatio(String cacheName) {
    String value = props.getProperty(cacheName + "."+ SCAVENGER_STOP_RUN_RATIO_KEY);
    if (value != null) {
      return Double.parseDouble(value);
    }
    return getDoubleProperty(SCAVENGER_STOP_RUN_RATIO_KEY, DEFAULT_SCAVENGER_STOP_RUN_RATIO);
  }

  /**
   * Set scavenger stop memory ratio
   * @param cacheName cache name
   * @param ratio memory ration relative to a maximum cache size
   */
  public void setScavengerStopMemoryRatio(String cacheName, double ratio) {
    props.setProperty(cacheName+ "."+ SCAVENGER_STOP_RUN_RATIO_KEY, Double.toString(ratio));
  }
  
  /**
   * Gets scavenger dump entry below - start
   * @param cacheName cache name
   * @return dump entry below ratio
   */
  public double getScavengerDumpEntryBelowAdjStep(String cacheName) {
    String value = props.getProperty(cacheName + "."+ SCAVENGER_DUMP_ENTRY_BELOW_STEP_KEY);
    if (value != null) {
      return Double.parseDouble(value);
    }
    return getDoubleProperty(SCAVENGER_DUMP_ENTRY_BELOW_STEP_KEY, DEFAULT_SCAVENGER_DUMP_ENTRY_BELOW_STEP);
  }

  /**
   * Sets scavenger dump entry below adjustment step
   * @param cacheName cache name
   * @param dump entry below adjustment step
   */
  public void setScavengerDumpEntryBelowAdjStep(String cacheName, double step) {
    props.setProperty(cacheName + "."+ SCAVENGER_DUMP_ENTRY_BELOW_STEP_KEY, Double.toString(step));
  }

  
  /**
   * Gets scavenger dump entry below - start
   * @param cacheName cache name
   * @return dump entry below ratio
   */
  public double getScavengerDumpEntryBelowStart(String cacheName) {
    String value = props.getProperty(cacheName + "."+ SCAVENGER_DUMP_ENTRY_BELOW_START_KEY);
    if (value != null) {
      return Double.parseDouble(value);
    }
    return getDoubleProperty(SCAVENGER_DUMP_ENTRY_BELOW_START_KEY, DEFAULT_SCAVENGER_DUMP_ENTRY_BELOW_START);
  }

  /**
   * Sets scavenger dump entry below - start
   * @param cacheName cache name
   * @param dump entry below ratio
   */
  public void setScavengerDumpEntryBelowStart(String cacheName, double ratio) {
    props.setProperty(cacheName + "."+ SCAVENGER_DUMP_ENTRY_BELOW_START_KEY, Double.toString(ratio));
  }
  
  /**
   * Gets scavenger dump entry below - stop
   * @param cacheName cache name
   * @return dump entry below ratio
   */
  public double getScavengerDumpEntryBelowStop(String cacheName) {
    String value = props.getProperty(cacheName + "."+ SCAVENGER_DUMP_ENTRY_BELOW_STOP_KEY);
    if (value != null) {
      return Double.parseDouble(value);
    }
    return getDoubleProperty(SCAVENGER_DUMP_ENTRY_BELOW_STOP_KEY, DEFAULT_SCAVENGER_DUMP_ENTRY_BELOW_STOP);
  }
  
  /**
   * Sets scavenger dump entry below - stop
   * @param cacheName cache name
   * @param dump entry below ratio
   */
  public void setScavengerDumpEntryBelowStop(String cacheName, double ratio) {
    props.setProperty(cacheName + "."+ SCAVENGER_DUMP_ENTRY_BELOW_STOP_KEY, Double.toString(ratio));
  }
  
  /**
   * Get random admission controller start ratio
   * @param cacheName cache name
   * @return start ratio
   */
  public double getRandomAdmissionControllerStartRatio(String cacheName) {
    String value = props.getProperty(cacheName + "."+ CACHE_RANDOM_ADMISSION_RATIO_START_KEY);
    if (value != null) {
      return Double.parseDouble(value);
    }
    return getDoubleProperty(CACHE_RANDOM_ADMISSION_RATIO_START_KEY, DEFAULT_CACHE_RANDOM_ADMISSION_RATIO_START);
  }
  
  /**
   * Set random admission controller start ratio
   * @param cacheName cache name
   * @param start ratio
   */
  public void setRandomAdmissionControllerStartRatio(String cacheName, double ratio) {
    props.setProperty(cacheName + "."+ CACHE_RANDOM_ADMISSION_RATIO_START_KEY, Double.toString(ratio));
  }
  
  /**
   * Get random admission controller stop ratio
   * @param cacheName cache name
   * @return stop ratio
   */
  public double getRandomAdmissionControllerStopRatio(String cacheName) {
    String value = props.getProperty(cacheName + "."+ CACHE_RANDOM_ADMISSION_RATIO_STOP_KEY);
    if (value != null) {
      return Double.parseDouble(value);
    }
    return getDoubleProperty(CACHE_RANDOM_ADMISSION_RATIO_STOP_KEY, DEFAULT_CACHE_RANDOM_ADMISSION_RATIO_STOP);
  }
  
  /**
   * Set random admission controller stop ratio
   * @param cacheName cache name
   * @param stop ratio
   */
  public void setRandomAdmissionControllerStopRatio(String cacheName, double ratio) {
    props.setProperty(cacheName + "."+ CACHE_RANDOM_ADMISSION_RATIO_STOP_KEY, Double.toString(ratio));
  }
  
  /**
   * Get number of item popularity ranks (bins) for a cache
   * @param cacheName cache name
   * @return number of popularity ranks
   */
  public int getNumberOfPopularityRanks(String cacheName) {
    String value = props.getProperty(cacheName + "."+ CACHE_POPULARITY_NUMBER_RANKS_KEY);
    if (value != null) {
      return (int) Long.parseLong(value);
    }
    return (int) getLongProperty(CACHE_POPULARITY_NUMBER_RANKS_KEY, DEFAULT_CACHE_POPULARITY_NUMBER_RANKS);
  }
  
  /**
   * Set number of item popularity ranks for a cache
   * @param cacheName cache name
   * @param number of popularity ranks
   */
  public void setNumberOfPopularityRanks(String cacheName, int num) {
    props.setProperty(cacheName + "."+ CACHE_POPULARITY_NUMBER_RANKS_KEY, Integer.toString(num));
  }
  
  /**
   * Get SLRU  insertion point (segment number to insert into the head of)
   * @param cacheName cache name
   * @return SLRU insertion point (segment number between 0 and getSLRUNumberOfSegments)
   */
  public int getSLRUInsertionPoint(String cacheName) {
    String value = props.getProperty(cacheName + "."+ SLRU_CACHE_INSERT_POINT_KEY);
    if (value != null) {
      return (int) Long.parseLong(value);
    }
    return (int) getLongProperty(SLRU_CACHE_INSERT_POINT_KEY, DEFAULT_SLRU_CACHE_INSERT_POINT);
  }
  
  /**
   * Set SLRU  insertion point (segment number to insert into the head of)
   * @param cacheName cache name
   * @param SLRU insertion point (segment number between 0 and getSLRUNumberOfSegments)
   */
  public void setSLRUInsertionPoint(String cacheName, int n) {
    props.setProperty(cacheName + "."+ SLRU_CACHE_INSERT_POINT_KEY, Integer.toString(n));
  }
  
  
  /**
   * Get SLRU  number of segments 
   * @param cacheName cache name
   * @return SLRU number of segments
   */
  public int getSLRUNumberOfSegments(String cacheName) {
    String value = props.getProperty(cacheName + "."+ SLRU_NUMBER_SEGMENTS_KEY);
    if (value != null) {
      return (int) Long.parseLong(value);
    }
    return (int) getLongProperty(SLRU_NUMBER_SEGMENTS_KEY, DEFAULT_SLRU_NUMBER_SEGMENTS);
  }
  
  /**
   * Set SLRU  number of segments 
   * @param cacheName cache name
   * @param SLRU number of segments
   */
  public void setSLRUNumberOfSegments(String cacheName, int n) {
    props.setProperty(cacheName + "."+ SLRU_NUMBER_SEGMENTS_KEY, Integer.toString(n));
  }
  
  /**
   * Get sparse files support for a cache (only for 'file' type caches)
   * @param cacheName cache name
   * @return true / false
   */
  public boolean getSparseFilesSupport(String cacheName) {
    String value = props.getProperty(cacheName + "."+ SPARSE_FILES_SUPPORT_KEY);
    if (value != null) {
      return Boolean.parseBoolean(value);
    }
    return getBooleanProperty(SPARSE_FILES_SUPPORT_KEY, DEFAULT_SPARSE_FILES_SUPPORT);
  }
  
  /**
   * Set sparse files support for a cache (only for 'file' type caches)
   * @param cacheName cache name
   * @param true / false
   */
  public void setSparseFilesSupport(String cacheName, boolean v) {
    props.setProperty(cacheName + "."+ SPARSE_FILES_SUPPORT_KEY, Boolean.toString(v));
  }
  
 
  /**
   * Get start index size for a cache
   * @param cacheName cache name
   * @return start index size for a given cache name
   */
  public int getStartIndexNumberOfSlotsPower (String cacheName) {
    String value = props.getProperty(cacheName + "."+ START_INDEX_NUMBER_OF_SLOTS_POWER_KEY);
    if (value != null) {
      return (int) Long.parseLong(value);
    }
    return (int) getLongProperty(START_INDEX_NUMBER_OF_SLOTS_POWER_KEY, 
      DEFAULT_START_INDEX_NUMBER_OF_SLOTS_POWER);
  }
   
  /**
   * Set start index size for a cache
   * @param cacheName cache name
   * @param start index size for a given cache name
   */
  public void setStartIndexNumberOfSlotsPower (String cacheName, int v) {
    props.setProperty(cacheName + "."+ START_INDEX_NUMBER_OF_SLOTS_POWER_KEY, Integer.toString(v));
  }
  
  /**
   * Get snapshot directory location for a cache
   * @param cacheName cache name
   * @return snapshot directory name for a given cache name
   */
  public String getSnapshotDir(String cacheName) {
    String value = props.getProperty(cacheName + "."+ CACHE_SNAPSHOT_DIR_NAME_KEY);
    if (value != null) {
      return value;
    }
    return props.getProperty(CACHE_SNAPSHOT_DIR_NAME_KEY, DEFAULT_CACHE_SNAPSHOT_DIR_NAME);
  }
  
  /**
   * Set snapshot directory location for a cache
   * @param cacheName cache name
   * @param snapshot directory name for a given cache name
   */
  public void setSnapshotDir(String cacheName, String dir) {
    props.setProperty(cacheName + "."+ CACHE_SNAPSHOT_DIR_NAME_KEY, dir);
  }
  
  /**
   * Get data directory location for a cache
   * @param cacheName cache name
   * @return data directory name for a given cache  name
   */
  public String getDataDir(String cacheName) {
    String value = props.getProperty(cacheName + "."+ CACHE_DATA_DIR_NAME_KEY);
    if (value != null) {
      return value;
    }
    return props.getProperty(CACHE_DATA_DIR_NAME_KEY, DEFAULT_CACHE_DATA_DIR_NAME);
  }
  
  /**
   * Set data directory location for a cache
   * @param cacheName cache name
   * @param data directory name for a given cache  name
   */
  public void setDataDir(String cacheName, String dir) {
    props.setProperty(cacheName + "."+ CACHE_DATA_DIR_NAME_KEY, dir);
  }
  
  /**
   * Get admission queue start size ratio for a given cache name
   * @param cacheName cache name
   * @return AQ start size ratio relative to the maximum cache size
   */
  public double getAdmissionQueueStartSizeRatio(String cacheName) {
    String value = props.getProperty(cacheName + "."+ ADMISSION_QUEUE_START_SIZE_RATIO_KEY);
    if (value != null) {
      return Double.parseDouble(value);
    }
    return getDoubleProperty(ADMISSION_QUEUE_START_SIZE_RATIO_KEY, DEFAULT_ADMISSION_QUEUE_START_SIZE_RATIO);
  }
  
  /**
   * Set admission queue start size ratio for a given cache name
   * @param cacheName cache name
   * @param AQ start size ratio relative to the maximum cache size
   */
  public void setAdmissionQueueStartSizeRatio(String cacheName, double ratio) {
    props.setProperty(cacheName + "."+ ADMISSION_QUEUE_START_SIZE_RATIO_KEY, Double.toString(ratio));
  }
  
  /**
   * Get admission queue minimum size ratio for a given cache name
   * @param cacheName cache name
   * @return AQ minimum size ratio relative to the maximum cache size
   */
  public double getAdmissionQueueMinSizeRatio(String cacheName) {
    String value = props.getProperty(cacheName + "."+ ADMISSION_QUEUE_MIN_SIZE_RATIO_KEY);
    if (value != null) {
      return Double.parseDouble(value);
    }
    return getDoubleProperty(ADMISSION_QUEUE_MIN_SIZE_RATIO_KEY, DEFAULT_ADMISSION_QUEUE_MIN_SIZE_RATIO);
  }
  
  /**
   * Set admission queue minimum size ratio for a given cache name
   * @param cacheName cache name
   * @param AQ minimum size ratio relative to the maximum cache size
   */
  public void setAdmissionQueueMinSizeRatio(String cacheName, double ratio ) {
    props.setProperty(cacheName + "."+ ADMISSION_QUEUE_MIN_SIZE_RATIO_KEY, Double.toString(ratio));
  }
  
  /**
   * Get admission queue maximum size ratio for a given cache name
   * @param cacheName cache name
   * @return AQ maximum size ratio relative to the maximum cache size
   */
  public double getAdmissionQueueMaxSizeRatio(String cacheName) {
    String value = props.getProperty(cacheName + "."+ ADMISSION_QUEUE_MAX_SIZE_RATIO_KEY);
    if (value != null) {
      return Double.parseDouble(value);
    }
    return getDoubleProperty(ADMISSION_QUEUE_MAX_SIZE_RATIO_KEY, DEFAULT_ADMISSION_QUEUE_MAX_SIZE_RATIO);
  }
  
  /**
   * Set admission queue maximum size ratio for a given cache name
   * @param cacheName cache name
   * @param AQ maximum size ratio relative to the maximum cache size
   */
  public void setAdmissionQueueMaxSizeRatio(String cacheName, double ratio) {
    props.setProperty(cacheName + "."+ ADMISSION_QUEUE_MAX_SIZE_RATIO_KEY, Double.toString(ratio));
  }
  
  /**
   * Get throughput check interval for a given cache name
   * @param cacheName cache name
   * @return interval in ms for a given cache name
   */
  public long getThroughputCheckInterval(String cacheName) {
    String value = props.getProperty(cacheName + "."+ THROUGHPUT_CHECK_INTERVAL_SEC_KEY);
    if (value != null) {
      return (int) Long.parseLong(value) * 1000;
    }
    return getLongProperty(THROUGHPUT_CHECK_INTERVAL_SEC_KEY, DEFAULT_THROUGHPUT_CHECK_INTERVAL_SEC) * 1000;
  }
  
  /**
   * Set throughput check interval for a given cache name
   * @param cacheName cache name
   * @param interval in sec for a given cache name
   */
  public void setThroughputCheckInterval(String cacheName, int interval) {
    props.setProperty(cacheName + "."+ THROUGHPUT_CHECK_INTERVAL_SEC_KEY, Long.toString(interval));
  }
  
  /**
   * Get Scavenger run interval for a given cache name
   * @param cacheName cache name
   * @return interval in seconds for a given cache name
   */
  public long getScavengerRunInterval(String cacheName) {
    String value = props.getProperty(cacheName + "."+ SCAVENGER_RUN_INTERVAL_SEC_KEY);
    if (value != null) {
      return (int) Long.parseLong(value);
    }
    return getLongProperty(SCAVENGER_RUN_INTERVAL_SEC_KEY, DEFAULT_SCAVENGER_RUN_INTERVAL_SEC);
  }
  
  /**
   * Set Scavenger run interval for a given cache name
   * @param cacheName cache name
   * @param interval in seconds for a given cache name
   */
  public void setScavengerRunInterval(String cacheName, int interval) {
    props.setProperty(cacheName + "."+ SCAVENGER_RUN_INTERVAL_SEC_KEY, Long.toString(interval));
  }
  
  /**
   * Get throughput controller tolerance limit for a given cache name
   * @param cacheName cache name
   * @return limit for a given cache name
   */
  public double getThroughputToleranceLimit(String cacheName) {
    String value = props.getProperty(cacheName + "."+ THROUGHPUT_CONTROLLER_TOLERANCE_LIMIT_KEY);
    if (value != null) {
      return Double.parseDouble(value);
    }
    return getDoubleProperty(THROUGHPUT_CONTROLLER_TOLERANCE_LIMIT_KEY, DEFAULT_THROUGHPUT_CONTROLLER_TOLERANCE_LIMIT);
  }
  
  /**
   * Set throughput controller tolerance limit for a given cache name
   * @param cacheName cache name
   * @param limit for a given cache name
   */
  public void setThroughputToleranceLimit(String cacheName, double limit) {
    props.setProperty(cacheName + "."+ THROUGHPUT_CONTROLLER_TOLERANCE_LIMIT_KEY, Double.toString(limit));
  }
  
  /**
   * Get cache write maximum wait time
   * @param cacheName cache name
   * @return maximum wait time for writes for a given cache name
   */
  public long getCacheWritesMaxWaitTime(String cacheName) {
    String value = props.getProperty(cacheName + "."+ CACHE_WRITES_MAX_WAIT_TIME_MS_KEY);
    if (value != null) {
      return Long.parseLong(value);
    }
    return getLongProperty(CACHE_WRITES_MAX_WAIT_TIME_MS_KEY, DEFAULT_CACHE_WRITES_MAX_WAIT_TIME_MS);
  }
  
  /**
   * Set cache write maximum wait time
   * @param cacheName cache name
   * @param max writes wait time for a given cache name
   */
  public void setCacheWritesMaxWaitTime(String cacheName, long max) {
    props.setProperty(cacheName + "."+ CACHE_WRITES_MAX_WAIT_TIME_MS_KEY, Long.toString(max));
  }
  
  /**
   * Get cache write rate limit for a given cache name
   * @param cacheName cache name
   * @return cache write rate limit for a given cache name
   */
  public long getCacheWriteLimit(String cacheName) {
    String value = props.getProperty(cacheName + "."+ CACHE_WRITE_RATE_LIMIT_KEY);
    if (value != null) {
      return Long.parseLong(value);
    }
    return getLongProperty(CACHE_WRITE_RATE_LIMIT_KEY, DEFAULT_CACHE_WRITE_RATE_LIMIT);
  }
  
  /**
   * Set cache write rate limit for a given cache name
   * @param cacheName cache name
   * @param limit cache write rate limit for a given cache name
   */
  public void setCacheWriteLimit(String cacheName, long limit) {
    props.setProperty(cacheName + "."+ CACHE_WRITE_RATE_LIMIT_KEY, Long.toString(limit));
  }
  
  /**
   * Get cache names list
   * @return cache names list
   */
  public String[] getCacheNames() {
    String s = props.getProperty(CACHES_NAME_LIST_KEY, DEFAULT_CACHES_NAME_LIST);
    return s.split(",");
  }
  
  /**
   * Get cache types list
   * @return cache types list
   */
  public String[] getCacheTypes() {
    String s = props.getProperty(CACHES_TYPES_LIST_KEY, DEFAULT_CACHES_TYPES_LIST);
    return s.split(",");
  }
  
  /**
   * Get victim cache name for cache
   * @param cacheName cache name
   * @return victim cache name
   */
  public String getVictimCacheName(String cacheName) {
    return props.getProperty(cacheName + "."+ CACHE_VICTIM_NAME_KEY);
  }
  
  /**
   * Get cache write rate limit for a given cache name
   * @param cacheName cache name
   * @return cache write rate limit for a given cache name
   */
  public int getThrougputControllerNumberOfAdjustmentSteps(String cacheName) {
    String value = props.getProperty(cacheName + "."+ THROUGHPUT_CONTROLLER_ADJUSTMENT_STEPS_KEY);
    if (value != null) {
      return (int) Long.parseLong(value);
    }
    return (int) getLongProperty(THROUGHPUT_CONTROLLER_ADJUSTMENT_STEPS_KEY, DEFAULT_THROUGHPUT_CONTROLLER_ADJUSTMENT_STEPS);
  }
  
  /**
   * Set cache write rate limit for a given cache name
   * @param cacheName cache name
   * @param cache write rate limit for a given cache name
   */
  public void setThrougputControllerNumberOfAdjustmentSteps(String cacheName, int n) {
    props.setProperty(cacheName + "."+ THROUGHPUT_CONTROLLER_ADJUSTMENT_STEPS_KEY, Long.toString(n));
  }
  
  /**
   * Is index data embedding supported
   * @param cacheName cache name
   * @return true or false
   */
  public boolean isIndexDataEmbeddedSupported(String cacheName) {
    return getBooleanProperty(cacheName + "." + INDEX_DATA_EMBEDDED_KEY, DEFAULT_INDEX_DATA_EMBEDDED);
  }
  
  /**
   * Set index data embedding supported
   * @param cacheName cache name
   * @param true or false
   */
  public  void setIndexDataEmbeddedSupported(String cacheName, boolean v) {
     props.setProperty(cacheName + "." + INDEX_DATA_EMBEDDED_KEY, Boolean.toString(v));
  }
  
  /**
   * Get data embedded size
   * @param cacheName cache name
   * @return data embedded size
   */
  public int getIndexDataEmbeddedSize(String cacheName) {
    return (int) getLongProperty(cacheName + "." + INDEX_DATA_EMBEDDED_SIZE_KEY, 
      DEFAULT_INDEX_DATA_EMBEDDED_SIZE);
  }

  /**
   * Set data embedded size
   * @param cacheName cache name
   * @param data embedded size
   */
  public void setIndexDataEmbeddedSize(String cacheName, int v) {
    props.setProperty(cacheName + "." + INDEX_DATA_EMBEDDED_SIZE_KEY, 
      Integer.toString(v));
  }
  
  /**
   * Get admission queue index format implementation
   * @param cacheName cache name
   * @return index format
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  public IndexFormat getAdmissionQueueIndexFormat(String cacheName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    String value = props.getProperty(cacheName + "." + INDEX_FORMAT_ADMISSION_QUEUE_IMPL_KEY);
    if (value == null) {
      value = props.getProperty(INDEX_FORMAT_ADMISSION_QUEUE_IMPL_KEY);
    }
    if (value == null) {
      // default implementation;
      return new AQIndexFormat();
    }
    @SuppressWarnings("unchecked")
    Class<IndexFormat> clz = (Class<IndexFormat>) Class.forName(value);
    IndexFormat instance = clz.newInstance();
    instance.setCacheName(cacheName);
    return instance;
  }
  
  /**
   * Set admission queue index format implementation
   * @param cacheName cache name
   * @param className  index format class name
   * 
   */
  public void setAdmissionQueueIndexFormat(String cacheName, String className) {
    props.setProperty(cacheName + "." + INDEX_FORMAT_ADMISSION_QUEUE_IMPL_KEY, className);
  }
  
  /**
   * Get main queue index format implementation
   * @param cacheName cache name
   * @return index format
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  public IndexFormat getMainQueueIndexFormat(String cacheName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    String value = props.getProperty(cacheName + "." + INDEX_FORMAT_MAIN_QUEUE_IMPL_KEY);
    if (value == null) {
      value = props.getProperty(INDEX_FORMAT_MAIN_QUEUE_IMPL_KEY);
    }
    if (value == null) {
      // default implementation;
      return new BaseIndexFormat();
    }
    @SuppressWarnings("unchecked")
    Class<IndexFormat> clz = (Class<IndexFormat>) Class.forName(value);
    IndexFormat instance = clz.newInstance();
    instance.setCacheName(cacheName);
    return instance;
  }
  
  /**
   * Set main queue index format implementation
   * @param cacheName cache name
   * @param index format class name
   */
  public void setMainQueueIndexFormat(String cacheName, String className) {
    props.setProperty(cacheName + "." + INDEX_FORMAT_MAIN_QUEUE_IMPL_KEY, className);
  }
  
  /**
   * Get cache eviction policy implementation by cache name
   * @param cacheName cache name
   * @return eviction policy
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  public EvictionPolicy getCacheEvictionPolicy(String cacheName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    String value = props.getProperty(cacheName + "." + CACHE_EVICTION_POLICY_IMPL_KEY);
    if (value == null) {
      value = props.getProperty(CACHE_EVICTION_POLICY_IMPL_KEY);
    }
    if (value == null) {
      // default implementation;
      EvictionPolicy policy = new SLRUEvictionPolicy();
      policy.setCacheName(cacheName);
      return policy;
    }
    @SuppressWarnings("unchecked")
    Class<EvictionPolicy> clz = (Class<EvictionPolicy>) Class.forName(value);
    EvictionPolicy instance = clz.newInstance();
    instance.setCacheName(cacheName);
    return instance;
  }
  
  /**
   * Set cache eviction policy implementation by cache name
   * @param cacheName cache name
   * @param eviction policy
   */
  public void setCacheEvictionPolicy(String cacheName, String className) {
    props.setProperty(cacheName + "." + CACHE_EVICTION_POLICY_IMPL_KEY, className);
  }
  
  /**
   * Get cache admission controller implementation by cache name
   * 
   * @param cacheName cache name
   * @return admission controller
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  public AdmissionController getAdmissionController(String cacheName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    String value = props.getProperty(cacheName + "." + CACHE_ADMISSION_CONTROLLER_IMPL_KEY);
    if (value == null) {
      value = props.getProperty(CACHE_ADMISSION_CONTROLLER_IMPL_KEY);
    }
    if (value == null) {
      // default implementation;
      return null;
    }
    @SuppressWarnings("unchecked")
    Class<AdmissionController> clz = (Class<AdmissionController>) Class.forName(value);
    AdmissionController instance = clz.newInstance();
    return instance;
  }
  
  /**
   * Set cache admission controller implementation by cache name
   * 
   * @param cacheName cache name
   * @param admission controller class name
   */
  public void setAdmissionController(String cacheName, String className) {
    props.setProperty(cacheName + "." + CACHE_ADMISSION_CONTROLLER_IMPL_KEY, className);
  }
  
  /**
   * Get cache throughput controller implementation by cache name
   * @param cacheName cache name
   * @return throughput controller
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  public ThroughputController getThroughputController(String cacheName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    String value = props.getProperty(cacheName + "." + CACHE_THROUGHPUT_CONTROLLER_IMPL_KEY);
    if (value == null) {
      value = props.getProperty(CACHE_THROUGHPUT_CONTROLLER_IMPL_KEY);
    }
    if (value == null) {
      // No defaults
      return null;
    }
    @SuppressWarnings("unchecked")
    Class<ThroughputController> clz = (Class<ThroughputController>) Class.forName(value);
    ThroughputController instance = clz.newInstance();
    return instance;
  }
  
  /**
   * Set cache throughput controller implementation by cache name
   * @param cacheName cache name
   * @param throughput controller class name
   */
  public void setThroughputController(String cacheName, String className) {
    props.setProperty(cacheName + "." + CACHE_THROUGHPUT_CONTROLLER_IMPL_KEY, className);
  }
  
  /**
   * Get Scavenger recycling selector implementation by cache name
   * @param cacheName cache name
   * @return recycling selector
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  public RecyclingSelector getRecyclingSelector(String cacheName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    String value = props.getProperty(cacheName + "." + CACHE_RECYCLING_SELECTOR_IMPL_KEY);
    if (value == null) {
      value = props.getProperty(CACHE_RECYCLING_SELECTOR_IMPL_KEY, DEFAULT_CACHE_RECYCLING_SELECTOR_IMPL);
    }
    
    @SuppressWarnings("unchecked")
    Class<RecyclingSelector> clz = (Class<RecyclingSelector>) Class.forName(value);
    RecyclingSelector instance = clz.newInstance();
    return instance;
  }
  
  /**
   * Set Scavenger recycling selector implementation by cache name
   * @param cacheName cache name
   * @param recycling selector class name
   */
  public void setRecyclingSelector(String cacheName, String className) {
    props.setProperty(cacheName + "." + CACHE_RECYCLING_SELECTOR_IMPL_KEY, className);
  }
  
  /**
   * Get segment data writer (appender) implementation by cache name
   * 
   * @param cacheName cache name
   * @return data writer
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  public DataWriter getDataWriter(String cacheName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    String value = props.getProperty(cacheName + "." + CACHE_DATA_WRITER_IMPL_KEY);
    if (value == null) {
      value = props.getProperty(CACHE_DATA_WRITER_IMPL_KEY, DEFAULT_CACHE_DATA_WRITER_IMPL);
    }
    @SuppressWarnings("unchecked")
    Class<DataWriter> clz = (Class<DataWriter>) Class.forName(value);
    DataWriter instance = clz.newInstance();
    instance.init(cacheName);
    return instance;
  }
  
  /**
   * Set segment data writer (appender) implementation by cache name
   * 
   * @param cacheName cache name
   * @param data writer class name
   */
  public void setDataWriter(String cacheName, String className) {
    props.setProperty(cacheName + "." + CACHE_DATA_WRITER_IMPL_KEY, className);
  }
  
  /**
   * Get segment data reader implementation (Memory) by cache name
   * 
   * @param cacheName cache name
   * @return data reader
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  public DataReader getMemoryDataReader(String cacheName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    String value = props.getProperty(cacheName + "." + CACHE_MEMORY_DATA_READER_IMPL_KEY);
    if (value == null) {
      value = props.getProperty(CACHE_MEMORY_DATA_READER_IMPL_KEY, DEFAULT_CACHE_MEMORY_DATA_READER_IMPL);
    }
    @SuppressWarnings("unchecked")
    Class<DataReader> clz = (Class<DataReader>) Class.forName(value);
    DataReader instance = clz.newInstance();
    instance.init(cacheName);
    return instance;
  }
  
  /**
   * Set segment data reader implementation (Memory) by cache name
   * 
   * @param cacheName cache name
   * @param data reader class name
   */
  public void setMemoryDataReader(String cacheName, String className) {
    props.setProperty(cacheName + "." + CACHE_MEMORY_DATA_READER_IMPL_KEY, className);
  }
  
  /**
   * Get segment data reader implementation (File) by cache name
   *
   * @param cacheName cache name
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  public DataReader getFileDataReader(String cacheName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    String value = props.getProperty(cacheName + "." + CACHE_FILE_DATA_READER_IMPL_KEY);
    if (value == null) {
      value = props.getProperty(CACHE_FILE_DATA_READER_IMPL_KEY, DEFAULT_CACHE_FILE_DATA_READER_IMPL);
    }
    @SuppressWarnings("unchecked")
    Class<DataReader> clz = (Class<DataReader>) Class.forName(value);
    DataReader instance = clz.newInstance();
    instance.init(cacheName);
    return instance;
  }
  
  /**
   * Set segment data reader implementation (File) by cache name
   *
   * @param cacheName cache name
   */
  public void setFileDataReader(String cacheName, String className) {
    props.setProperty(cacheName + "." + CACHE_FILE_DATA_READER_IMPL_KEY, className);
  }
  
  /**
   * Get block writer block size by cache name
   * @param cacheName cache name
   * @return block size
   */
  public int getBlockWriterBlockSize(String cacheName) {
    String value = props.getProperty(cacheName + "." + CACHE_BLOCK_WRITER_BLOCK_SIZE_KEY);
    if (value == null) {
      return (int) getLongProperty(CACHE_BLOCK_WRITER_BLOCK_SIZE_KEY, DEFAULT_CACHE_BLOCK_WRITER_BLOCK_SIZE);
    } else {
      return Integer.parseInt(value);
    }
  }
  
  /**
   * Set block writer block size by cache name
   * @param cacheName cache name
   * @param block size
   */
  public void  setBlockWriterBlockSize(String cacheName, int size) {
    props.setProperty(cacheName + "." + CACHE_BLOCK_WRITER_BLOCK_SIZE_KEY, Integer.toString(size));
  }
  
  /**
   * Get file prefetch buffer by cache name
   * @param cacheName cache name
   * @return prefetch buffer size
   */
  public int getFilePrefetchBufferSize(String cacheName) {
    String value = props.getProperty(cacheName + "." + FILE_PREFETCH_BUFFER_SIZE_KEY);
    if (value == null) {
      return (int) getLongProperty(FILE_PREFETCH_BUFFER_SIZE_KEY, DEFAULT_FILE_PREFETCH_BUFFER_SIZE);
    } else {
      return Integer.parseInt(value);
    }
  }
   
  /**
   * Set file prefetch buffer by cache name
   * @param cacheName cache name
   * @param prefetch buffer size
   */
  public void setFilePrefetchBufferSize(String cacheName, int prefetch) {
    props.setProperty(cacheName + "." + FILE_PREFETCH_BUFFER_SIZE_KEY, Integer.toString(prefetch));
  }
  
  /**
   * Get expiration support implementation by cache name
   * @param cacheName cache name
   * @return expire support implementation
   */
  public ExpireSupport getExpireSupport(String cacheName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    String value = props.getProperty(cacheName + "." + CACHE_EXPIRE_SUPPORT_IMPL_KEY);
    if (value == null) {
      value = props.getProperty(CACHE_EXPIRE_SUPPORT_IMPL_KEY, DEFAULT_CACHE_EXPIRE_SUPPORT_IMPL);
    }
    
    @SuppressWarnings("unchecked")
    Class<ExpireSupport> clz = (Class<ExpireSupport>) Class.forName(value);
    ExpireSupport instance = clz.newInstance();
    return instance;
  }
  
  /**
   * Set expiration support implementation by cache name
   * @param cacheName cache name
   * @return expire support class name
   */
  public void setExpireSupport(String cacheName, String className) {
    props.setProperty(cacheName + "." + CACHE_EXPIRE_SUPPORT_IMPL_KEY, className);
  }
  
  /**
   * Get start bin value for expiration - based admission controller
   * @param cacheName cache name
   * @return value
   */
  public int getExpireStartBinValue(String cacheName) {
    String value = props.getProperty(cacheName + "." + CACHE_EXPIRATION_BIN_START_VALUE_KEY);
    if (value == null) {
      return (int) getLongProperty(CACHE_EXPIRATION_BIN_START_VALUE_KEY, DEFAULT_CACHE_EXPIRATION_BIN_START_VALUE);
    } else {
      return Integer.parseInt(value);
    }
  }

  /**
   * Set start bin value for expiration - based admission controller
   * @param cacheName cache name
   * @param value (in seconds)
   */
  public void setExpireStartBinValue(String cacheName, int value) {
    props.setProperty(cacheName + "." + CACHE_EXPIRATION_BIN_START_VALUE_KEY, Integer.toString(value));
  }
  
  /**
   * Get expiration bin multiplier
   * @param cacheName cache name
   * @return value
   */
  public double getExpireBinMultiplier (String cacheName) {
    String value = props.getProperty(cacheName + "." + CACHE_EXPIRATION_MULTIPLIER_VALUE_KEY);
    if (value == null) {
      return getDoubleProperty(CACHE_EXPIRATION_MULTIPLIER_VALUE_KEY, DEFAULT_CACHE_EXPIRATION_MULTIPLIER_VALUE);
    } else {
      return Double.parseDouble(value);
    }
  }
  
  /**
   * Set expiration bin multiplier
   * @param cacheName cache name
   * @param value multiplier
   */
  public void setExpireBinMultiplier (String cacheName, double multiplier) {
    props.getProperty(cacheName + "." + CACHE_EXPIRATION_MULTIPLIER_VALUE_KEY, Double.toString(multiplier));
  }
  
  /**
   * Get minimum cache active data set threshold
   * @param cacheName cache name
   * @return value
   */
  public double getMinimumActiveDatasetRatio (String cacheName) {
    String value = props.getProperty(cacheName + "." + CACHE_MINIMUM_ACTIVE_DATA_SET_RATIO_KEY);
    if (value == null) {
      return getDoubleProperty(CACHE_MINIMUM_ACTIVE_DATA_SET_RATIO_KEY, 
        DEFAULT_CACHE_MINIMUM_ACTIVE_DATA_SET_RATIO);
    } else {
      return Double.parseDouble(value);
    }
  }
  
  /**
   * Set minimum cache active data set threshold
   * @param cacheName cache name
   * @param value multiplier
   */
  public void setMinimumActiveDatasetRatio (String cacheName, double ratio) {
    props.getProperty(cacheName + "." + CACHE_MINIMUM_ACTIVE_DATA_SET_RATIO_KEY, 
      Double.toString(ratio));
  }
  
  /**
   * Get eviction disabled mode
   * @param cacheName cache name
   * @return true or false
   */
  public boolean getEvictionDisabledMode (String cacheName) {
    String value = props.getProperty(cacheName + "." + CACHE_EVICTION_DISABLED_MODE_KEY);
    if (value == null) {
      return getBooleanProperty(CACHE_EVICTION_DISABLED_MODE_KEY, 
        DEFAULT_CACHE_EVICTION_DISABLED_MODE);
    } else {
      return Boolean.parseBoolean(value);
    }
  }
  
  /**
   * Set eviction disabled mode
   * @param cacheName cache name
   * @param true or false
   */
  public void setEvictionDisabledMode (String cacheName, boolean mode) {
    props.setProperty(cacheName + "." + CACHE_EVICTION_DISABLED_MODE_KEY, Boolean.toString(mode));
  }
  
  /**
   * Get rolling window number of bins
   * @param cacheName cache name
   * @return true or false
   */
  public int getRollingWindowNumberBins (String cacheName) {
    String value = props.getProperty(cacheName + "." + CACHE_ROLLING_WINDOW_COUNTER_BINS_KEY);
    if (value == null) {
      return (int) getLongProperty(CACHE_ROLLING_WINDOW_COUNTER_BINS_KEY, 
        DEFAULT_CACHE_ROLLING_WINDOW_COUNTER_BINS);
    } else {
      return Integer.parseInt(value);
    }
  }
  
  /**
   * Set rolling window number of bins
   * @param cacheName cache name
   * @param number of bins
   */
  public void setRollingWindowNumberBins (String cacheName, int n) {
    props.setProperty(cacheName + "." + CACHE_ROLLING_WINDOW_COUNTER_BINS_KEY, Integer.toString(n));
  }
  
  /**
   * Get rolling window duration in seconds
   * @param cacheName cache name
   * @return window duration in seconds
   */
  public int getRollingWindowDuration (String cacheName) {
    String value = props.getProperty(cacheName + "." + CACHE_ROLLING_WINDOW_COUNTER_DURATION_KEY);
    if (value == null) {
      return (int) getLongProperty(CACHE_ROLLING_WINDOW_COUNTER_DURATION_KEY, 
        DEFAULT_CACHE_ROLLING_WINDOW_COUNTER_DURATION);
    } else {
      return Integer.parseInt(value);
    }
  }
  
  /**
   * Set rolling window duration in seconds
   * @param cacheName cache name
   * @param n seconds
   */
  public void setRollingWindowDuration (String cacheName, int n) {
    props.setProperty(cacheName + "." + DEFAULT_CACHE_ROLLING_WINDOW_COUNTER_DURATION, Integer.toString(n));
  }
  
  
  /**
   * Get I/O storage pool size
   * @param cacheName cache name
   * @return size
   */
  public int getIOStoragePoolSize (String cacheName) {
    String value = props.getProperty(cacheName + "." + CACHE_IO_STORAGE_POOL_SIZE_KEY);
    if (value == null) {
      return (int) getLongProperty(CACHE_IO_STORAGE_POOL_SIZE_KEY, 
        DEFAULT_CACHE_IO_STORAGE_POOL_SIZE);
    } else {
      return Integer.parseInt(value);
    }
  }
  
  /**
   * Set I/O storage pool size
   * @param cacheName cache name
   * @param n size
   */
  public void setIOStoragePoolSize (String cacheName, int n) {
    props.setProperty(cacheName + "." + CACHE_IO_STORAGE_POOL_SIZE_KEY, Integer.toString(n));
  }
  
  /**
   * Get promotion on hit for victim cache
   * @param cacheName cache name
   * @return true promote to main cache, false - do not
   */
  public boolean getVictimCachePromotionOnHit (String cacheName) {
    String value = props.getProperty(cacheName + "." + CACHE_VICTIM_PROMOTION_ON_HIT_KEY);
    if (value == null) {
      return getBooleanProperty(CACHE_VICTIM_PROMOTION_ON_HIT_KEY, 
        DEFAULT_CACHE_VICTIM_PROMOTION_ON_HIT);
    } else {
      return Boolean.parseBoolean(value);
    }
  }
  
  /**
   * Set promotion on hit for victim cache
   * @param cacheName cache name
   * @param v true or false
   */
  public void setVictimCachePromotionOnHit (String cacheName, boolean v) {
    props.setProperty(cacheName + "." + CACHE_VICTIM_PROMOTION_ON_HIT_KEY, Boolean.toString(v));
  }
  
  /**
   * Get maximum number of processed segments before activation of a stall mode
   * @param cacheName cache name
   * @return number of segments
   */
  public int getScavengerMaxSegmentsBeforeStall(String cacheName) {
    String value = props.getProperty(cacheName + "." + SCAVENGER_MAX_SEGMENTS_BEFORE_STALL_KEY);
    if (value == null) {
      return (int) getLongProperty(SCAVENGER_MAX_SEGMENTS_BEFORE_STALL_KEY, 
        DEFAULT_SCAVENGER_MAX_SEGMENTS_BEFORE_STALL);
    } else {
      return Integer.parseInt(value);
    }
  }
  
  /**
   * Set maximum number of processed segments before activation of a stall mode
   * @param cacheName cache name
   * @param n number of segments
   */
  public void setScavengerMaxSegmentsBeforeStall (String cacheName, int n) {
    props.setProperty(cacheName + "." + SCAVENGER_MAX_SEGMENTS_BEFORE_STALL_KEY, Integer.toString(n));
  }
}
