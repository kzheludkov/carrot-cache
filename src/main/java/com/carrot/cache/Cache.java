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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.carrot.cache.controllers.AdmissionController;
import com.carrot.cache.controllers.ThroughputController;
import com.carrot.cache.eviction.EvictionListener;
import com.carrot.cache.index.IndexFormat;
import com.carrot.cache.index.MemoryIndex;
import com.carrot.cache.io.IOEngine;
import com.carrot.cache.io.IOEngine.IOEngineEvent;
import com.carrot.cache.io.OffheapIOEngine;
import com.carrot.cache.io.Segment;
import com.carrot.cache.util.CacheConfig;
import com.carrot.cache.util.Epoch;
import com.carrot.cache.util.UnsafeAccess;
import com.carrot.cache.util.Utils;

/**
 * Main entry for off-heap/on-disk cache
 *
 * <p>Memory Index is a dynamic hash table, which implements smart incremental rehashing technique
 * to avoid large pauses during operation.
 *
 * <p>Size of a table is always power of 2'. It starts with size = 64K (configurable)
 *
 * <p>1. Addressing
 *
 * <p>1.1 Each key is hashed into 8 bytes value 
 * <p>1.2 First N bits (2**N is a size of a table) are
 * used to identify a slot number. 
 * <p>1.3 Each slot is 8 bytes and keeps address to an index buffer IB
 * (dynamically sized between 256 bytes and 4KB) 
 * <p>1.4 Each index buffer keeps cached item indexes.
 * First 2 bytes keeps # of indexes
 *
 * <p>0 -> IB(0) 1 -> IB(1)
 *
 * <p>...
 *
 * <p>64535 -> IB (65535)
 *
 * <p>IB(x) -> number indexes (2 bytes) index1 (16 bytes) index2 (16 bytes) index3(16 bytes)
 *
 * <p>1.5 Index is 16 bytes, first 8 bytes is hashed key, second 8 bytes is address of a cached item
 * in a special format: 
 * <p>1.5.1 first 2 bytes - reserved for future eviction algorithms 
 * <p>1.5.2 next 2 bytes - Memory buffer ID (total maximum number of buffers is 64K) 
 * <p>1.5.3 last 4 bytes is offset in
 * a memory buffer (maximum memory buffer size is 4GB)
 *
 * <p>Memory buffer is where cached data is stored. Each cached item has the following format:
 *
 * <p>expiration (8 bytes) key (variable) item (variable)
 *
 * <p>2. Incremental rehashing
 *
 * <p>When some index buffer is filled up (size is 4KB and keeps 255 indexes each 16 bytes long) and
 * no maximum memory limit is reached yet, rehashing process starts. 
 * <p>2.1 N -> N+1 we increment size
 * of a table by factor of 2 
 * <p>2.2 We rehash full slots one - by one once slot reaches its capacity
 * <p>2.3 Slot #K is rehashed into 2 slots in a new hash table: 'K0' and 'K1'
 *
 * <p>Example: let suppose that N = 8 and we rehash slot with ID = 255 (11111111) This slot will be
 * rehashed into 2 slots in a new table: 111111110 and 111111111. ALl keys for these two slots come
 * from a slot 255 from and old table 
 * <p>2.4 System set 'rehashInProgress' and two tables now co-exists
 * <p>2.5 We keep track on how many slots have been rehashed already and based on this number we set
 * the priority on probing both tables as following: old -> new when number of rehashed slots <
 * size(old); new -> old - otherwise 
 * <p>2.6 when rehash finishes, we set old_table = new_table and set
 * rehashInProgress to 'false' 2.6 If during rehashing some slot in a new table reaches maximum
 * capacity - all cache operations are put on hold until old table rehash is finished. This is
 * highly unlikely event, but in theory is possible
 *
 * <p>3. Eviction algorithms
 *
 * <p>3.1 CSLRU - Concurrent Segmented LRU
 *
 * <p>SLOT X: Item1, Item2, ...Item100, ... ItemX (X <= 255)
 *
 * <p>3.1.1 Eviction and item promotion (on hit) happens in a particular table's slot. 
 * <p>3.1.2 Because we have at least 64K slots - there are literally no contention on both: read and insert. We use
 * write locking of a slot. Multiple concurrent threads can read/write index at the same time 
 * <p>3.1.3 When item is hit, his position in a slot memory buffer is changed - it is moved closer to the
 * head of a buffer, how far depends on how many virtual segments in a slot we have. Default is 8.
 * So. if item is located in a segment 6, it will be moved to a head of a segment 5. Item in a
 * segment 1 will be moved to a head of a segment 1.
 *
 * <p>Example:
 *
 * <p>A. We found ITEM in a Slot Y. ITEM has position 100, total number of items in a slot - 128.
 * Therefore virtual segment size is 128/8 = 16. Item is located in segment 7, it will be promoted
 * to a head of segment 6 and its position will change from 100 to 80
 *
 * <p>3.2 CSLRU-WP (with weighted promotion)
 *
 * <p>We take into account accumulated 'importance' of a cached entry when making decision on how
 * large will item promotion be. 2 bytes in a 8 byte index entry are access counters. Every time
 * item is accessed we increment counter. On saturation (if it will ever happen) we reset counter to
 * 0 (?)
 *
 * <p>All cached items are divided by groups: VERY HOT, HOT, WARM, COLD based on a values of their
 * counters. For VERY HOT item promotion on access is going to be larger than for COLD item.
 *
 * <p>This info can be used during item eviction from RAM cache to SSD cache. For example, during
 * eviction COLD items from RAM will be discarded completely, all others will be evicted to SSD
 * cache.
 *
 * <p>4. Insertion point To prevent cache from trashing on scan-like workload, we insert every new
 * item into the head of a segment 7, which is approximately 25% (quarter) distance from a memory
 * buffer tail.
 *
 * <p>5. Handling TTL and freeing the space
 *
 * <p>5.1 Special Scavenger thread is running periodically to clean TTL-expired items and remove
 * cold items to free space for a new items 
 * <p>5.2 All memory buffers are ordered BUF(0) -> BUF(1) ->
 * ... -> BUF(K) in a circular buffer. For the sake of simplicity, let BUF(0) be a head, where new
 * items are inserted into right now. 
 * <p>5.3 Scavenger scans buffers starting from BUF(1) (the least
 * recent ones). It skips TTL-expired items and inserts other ones into BUF(0) ONLY if they are
 * popular enough (if you remember, we separated ALL cached items into 8 segments, where Segment 1
 * is most popular and Segment 8 is the least popular). By default we dump all items which belongs
 * to segments 7 and 8. 
 * <p>5.4 There are two configuration parameters which control Scavenger :
 * minimum_start_capacity (95% by default) and stop_capacity (90%). Scavengers starts running when
 * cache reaches minimum_start_capacity and stops when cache size gets down to stop_capacity. 
 * <p>5.5 During scavenger run, the special rate limiter controls incoming data rate and sets its limit to
 * 90% of a Scavenger cleaning data rate to guarantee that we won't exceed cache maximum capacity
 */
public class Cache implements IOEngine.Listener, EvictionListener {

  /** Logger */
  private static final Logger LOG = LogManager.getLogger(Cache.class);
  
  public static enum Type {
    MEMORY, DISK
  }

  /*
   * Total allocated memory (bytes)
   */
  private AtomicLong allocatedMemory = new AtomicLong(0);

  /** Total used memory (bytes) */
  private AtomicLong usedMemory = new AtomicLong(0);

  /* Total number of accesses (GET)*/
  private AtomicLong totalGets = new AtomicLong(0);
  
  /* Total hits */
  private AtomicLong totalHits = new AtomicLong(0);
  
  /* Total writes */
  private AtomicLong totalWrites = new AtomicLong(0);
  
  /* Total rejected writes */
  private AtomicLong totalRejectedWrites = new AtomicLong(0);
  
  /* Cache name */
  String cacheName;
  
  /** Cache configuration */
  CacheConfig conf;

  /** Maximum memory limit in bytes */
  long maximumCacheSize;

  /** Cache scavenger */
  Scavenger scavenger;
  
  /* IOEngine */
  IOEngine engine;
    
  /* Admission Controller - optional */
  AdmissionController admissionController;
  
  /* Throughput controller - optional */
  ThroughputController throughputController;
  
  /* Periodic task runner */
  Timer timer;
  
  /* Victim cache */
  Cache victimCache;
  
  /* Parent cache */
  Cache parentCache;
  
  /* Threshold to reject writes */
  double writeRejectionThreshold;
  
  Epoch epoch;
    
  /* Throughput controller enabled */
  boolean tcEnabled;
  /**
   * Constructor with configuration
   * 
   * @param conf configuration
   * @throws IOException 
   */
  Cache(String name) throws IOException {
    this.cacheName = name;
    this.conf = CacheConfig.getInstance();
    this.engine = IOEngine.getEngineForCache(this);
    // set engine listener
    this.engine.setListener(this);
    //TODO: do we need this?
    this.writeRejectionThreshold = this.conf.getCacheWriteRejectionThreshold(this.cacheName);
    updateMaxCacheSize();
    initAll();
  }

  private void initAll() throws IOException {
    initAdmissionController();
    initThroughputController();

    initScavenger();
  }

  private void initScavenger() {
    long interval = this.conf.getScavengerRunInterval(this.cacheName) * 1000;
    LOG.info("Started Scavenger, interval=%d sec", interval /1000);
    TimerTask task = new TimerTask() {
      public void run() {
        if (Cache.this.scavenger != null && Cache.this.scavenger.isAlive()) {
          return;
        }
        Cache.this.scavenger = new Scavenger(Cache.this);
        Cache.this.scavenger.start();
      }
    };
    this.timer.scheduleAtFixedRate(task, interval, interval);
  }

  private void adjustThroughput() {
    boolean result = this.throughputController.adjustParameters();
    LOG.info("Adjusted throughput controller =" + result);
    this.throughputController.printStats();
  }
  
  private void reportThroughputController(long bytes) {
    if (!this.tcEnabled  || this.throughputController == null) {
      return;
    }
    this.throughputController.record(bytes);
  }
  
  /**
   * Initialize admission controller
   * @throws IOException
   */
  private void initAdmissionController() throws IOException {
    try {
      this.admissionController = this.conf.getAdmissionController(cacheName);
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      LOG.error(e);
      throw new RuntimeException(e);
    }
    if (this.admissionController == null) {
      return;
    }
    this.admissionController.setCache(this);
    LOG.info("Started Admission Controller [%s]", this.admissionController.getClass().getName());

  }

  /**
   * Initialize throughput controller
   *
   * @throws IOException
   */
  private void initThroughputController() throws IOException {
    try {
      this.throughputController = this.conf.getThroughputController(cacheName);
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      LOG.error(e);
      throw new RuntimeException(e);
    }

    if (this.throughputController == null) {
      return;
    }
    this.throughputController.setCache(this);
    TimerTask task =
        new TimerTask() {
          public void run() {
            adjustThroughput();
          }
        };
    this.timer = new Timer();
    long interval = this.conf.getThroughputCheckInterval(this.cacheName);
    this.timer.scheduleAtFixedRate(task, interval, interval);
    LOG.info("Started throughput controller, interval=%d sec", interval /1000);
  }

  private void updateMaxCacheSize() {
    this.maximumCacheSize = this.engine.getMaximumStorageSize();
  }

  /**
   * Get cache type
   * @return cache type
   */
  public Type getCacheType() {
    if (this.engine instanceof OffheapIOEngine) {
      return Type.MEMORY;
    } else {
      return Type.DISK;
    }
  }
  
  /**
   * Get cache name
   * @return cache name
   */
  public String getName() {
    return this.cacheName;
  }

  /**
   * Get cache configuration object
   * @return cache configuration object
   */
  public CacheConfig getCacheConfig() {
    return this.conf;
  }
    
  /**
   * Get IOEngine
   * @return engine
   */
  public IOEngine getEngine() {
    return this.engine;
  }
  
  /**
   * Report used memory (can be negative if free())
   *
   * @param size allocated memory size
   */
  public long reportUsed(long size) {
    
    reportThroughputController(size);
    return this.usedMemory.addAndGet(size);
  }

  /**
   * Report used memory (can be negative if free())
   * @param keySize key size
   * @param valSize value size
   * @return used memory size
   */
  public long reportUsed(int keySize, int valSize) {
    return reportUsed(Utils.kvSize(keySize, valSize));
  }
  
  /**
   * Get total used memory
   *
   * @return used memory
   */
  public long getUsedMemory() {
    return usedMemory.get();
  }

  /**
   * Report allocated memory (can be negative if free())
   *
   * @param size allocated memory size
   */
  public long reportAllocation(long size) {
    return this.allocatedMemory.addAndGet(size);
  }

  /**
   * Get total allocated memory
   *
   * @return
   */
  public long getAllocatedMemory() {
    return allocatedMemory.get();
  }

  /**
   * Gets memory limit
   * @return memory limit in bytes
   */
  public long getMaximumCacheSize() {
    return this.maximumCacheSize;
  }
  
  /**
   * Get memory used as a fraction of memoryLimit
   *
   * @return memory used fraction
   */
  public double getMemoryUsedPct() {
    return ((double) getUsedMemory()) / maximumCacheSize;
  }

  /**
   * Get admission controller
   * @return admission controller
   */
  public AdmissionController getAdmissionController() {
    return this.admissionController;
  }
  
  /**
   * Sets admission controller
   * @param ac admission controller
   */
  public void setAdmissionController(AdmissionController ac) {
    this.admissionController = ac;
  }
  
  /**
   * Get total gets
   * @return total gets
   */
  public long getTotalGets() {
    return this.totalGets.get();
  }
  
  /**
   * Get total hits
   * @return total hits
   */
  public long getTotalHits() {
    return this.totalHits.get();
  }
  
  /**
   * Get total writes
   * @return total writes
   */
  public long getTotalWrites() {
    return this.totalWrites.get();
  }
  
  /**
   * Get total rejected writes
   * @return total rejected writes
   */
  public long getTotalRejectedWrites() {
    return this.totalRejectedWrites.get();
  }
  
  /**
   * Get cache hit rate
   * @return cache hit rate
   */
  public double getHitRate() {
    return (double) totalHits.get() / totalGets.get();
  }
  
  private void access() {
    this.totalGets.incrementAndGet();
  }
  
  private void hit() {
    this.totalHits.incrementAndGet();
  }
  
  /***********************************************************
   * Cache API
   */

  /* Put API */

  private boolean rejectWrite() {
    double d = getMemoryUsedPct();
    return d >= this.writeRejectionThreshold;
  }
  
  /**
   * Put item into the cache
   *
   * @param keyPtr key address
   * @param keySize key size
   * @param valPtr value address
   * @param valSize value size
   * @param expire - expiration (0 - no expire)
   */
  public void put(long keyPtr, int keySize, long valPtr, int valSize, long expire)
    throws IOException {    
    int rank = getDefaultRankToInsert();//.getSLRUInsertionPoint(this.cacheName);
    put(keyPtr, keySize, valPtr, valSize, expire, rank, false);
  }

  private boolean shouldAdmitToMainQueue(long keyPtr, int keySize, boolean force) {
    if (!force && this.admissionController != null) {
      return this.admissionController.admit(keyPtr, keySize);
    }
    return true;
  }
  
  /**
   * Put item into the cache - API for new items
   *
   * @param keyPtr key address
   * @param keySize key size
   * @param valPtr value address
   * @param valSize value size
   * @param expire expiration (0 - no expire)
   * @param rank rank of the item
   * @param force if true - bypass admission controller
   */
  public void put(
      long keyPtr, int keySize, long valPtr, int valSize, long expire, int rank, boolean force)
      throws IOException {

    if (rejectWrite()) {
      this.totalRejectedWrites.incrementAndGet();
      return;
    }
    // Check rank
    checkRank(rank);
    if (!shouldAdmitToMainQueue(keyPtr, keySize, force)) {
      return;
    }
    this.totalWrites.incrementAndGet();

    // Adjust rank taking into account item's expiration time
    rank = adjustRank(rank, expire);
    expire = adjustExpirationTime(expire);

    // Add to the cache
    engine.put(keyPtr, keySize, valPtr, valSize, expire, rank);
    // TODO: update stats
    reportUsed(keySize, valSize);
  }

  private void checkRank(int rank) {
    int maxRank = this.engine.getNumberOfRanks();
    if (rank < 0 || rank >= maxRank) {
      throw new IllegalArgumentException(String.format("Items rank %d is illegal"));
    }
  }

  private boolean shouldAdmitToMainQueue(byte[] key, int keyOffset, int keySize, boolean force) {
    if (!force && this.admissionController != null) {
      return this.admissionController.admit(key, keyOffset, keySize);
    }
    return true;
  }
  
  private int adjustRank(int rank, long expire) {
    if (this.admissionController != null) {
      // Adjust rank taking into account item's expiration time
      rank = this.admissionController.adjustRank(rank, expire);
    }
    return rank;
  }
  
  private long adjustExpirationTime(long expire) {
    if (this.admissionController != null) {
      expire = this.admissionController.adjustExpirationTime(expire);
    }
    return expire;
  }
  
  /**
   * Put item into the cache
   *
   * @param key key
   * @param keyOffset key offset
   * @param keySize key size
   * @param value value
   * @param valOffset value offset
   * @param valSize value size
   * @param expire - expiration (0 - no expire)
   * @param force if true - bypass admission controller
   */
  public void put(
      byte[] key,
      int keyOffset,
      int keySize,
      byte[] value,
      int valOffset,
      int valSize,
      long expire,
      int rank,
      boolean force)
      throws IOException {
    if (rejectWrite()) {
      this.totalRejectedWrites.incrementAndGet();
      return;
    }
    if (!shouldAdmitToMainQueue(key, keyOffset, keySize, force)) {
      return;
    }
    this.totalWrites.incrementAndGet();
    // Check rank
    checkRank(rank);
      // Adjust rank
    rank = adjustRank(rank, expire);
    expire = adjustExpirationTime(expire);
    // Add to the cache
    engine.put(key, keyOffset, keySize, value, valOffset, valSize, expire, rank);
    // TODO: update stats
    reportUsed(keySize, valSize);
  }

  private void put (byte[] buf, int off, long expire) throws IOException {
    int rank = getDefaultRankToInsert();
    int keySize = Utils.readUVInt(buf, off);
    int kSizeSize = Utils.sizeUVInt(keySize);
    int valueSize = Utils.readUVInt(buf, off + kSizeSize);
    int vSizeSize = Utils.sizeUVInt(valueSize);
    put(buf, off + kSizeSize + vSizeSize, keySize, buf, 
      off + kSizeSize + vSizeSize + keySize, valueSize, expire, rank, true);
  }
  
  private void put(long bufPtr, long expire) throws IOException {
    int rank = getDefaultRankToInsert();
    int keySize = Utils.readUVInt(bufPtr);
    int kSizeSize = Utils.sizeUVInt(keySize);
    int valueSize = Utils.readUVInt(bufPtr + kSizeSize);
    int vSizeSize = Utils.sizeUVInt(valueSize);
    put(bufPtr + kSizeSize + vSizeSize, keySize, bufPtr 
      + kSizeSize + vSizeSize + keySize, valueSize, expire, rank, true);
  }
  
  private void put(ByteBuffer buf, long expire) throws IOException {
    if (buf.hasArray()) {
      byte[] buffer = buf.array();
      int bufOffset = buf.position();
      put(buffer, bufOffset, expire);
    } else {
      long ptr = UnsafeAccess.address(buf);
      int off = buf.position();
      put(ptr + off, expire);
    }
  }
  
  /**
   * Put item into the cache
   *
   * @param key key
   * @param value value
   * @param expire - expiration (0 - no expire)
   */
  public void put(byte[] key, byte[] value, long expire) throws IOException {
    int rank = getDefaultRankToInsert();
    put(key, 0, key.length, value, 0, value.length, expire, rank, false);
  }

  /* Get API*/
  /**
   * Get cached item (if any)
   *
   * @param keyPtr key address
   * @param keySize key size
   * @param hit if true - its a hit
   * @param buffer buffer for item
   * @param bufOffset buffer offset
   * @return size of an item (-1 - not found), if is greater than bufSize - retry with a properly
   *     adjusted buffer
   * @throws IOException 
   */
  public long get(long keyPtr, int keySize, boolean hit, byte[] buffer, int bufOffset) throws IOException {
    long result = engine.get(keyPtr, keySize, buffer, bufOffset);
    if (result <= buffer.length - bufOffset) {
      access();
      if (result >= 0) {
        hit();
      }
    }
    if (result >= 0 && result <= buffer.length - bufOffset) {
      if (this.admissionController != null) {
        this.admissionController.access(keyPtr, keySize);
      }
    }
    if(result < 0 && this.victimCache != null) {
      result = this.victimCache.get(keyPtr, keySize, hit, buffer, bufOffset);
      if (result >=0 && result <= buffer.length - bufOffset) {
        // put k-v into this cache, remove it from the victim cache
        MemoryIndex mi = this.victimCache.getEngine().getMemoryIndex();
        long expire = mi.getExpire(keyPtr, keySize);
        put(buffer, bufOffset, expire);
        this.victimCache.delete(keyPtr, keySize);
      } 
    }
    return result;
  }

  /**
   * Get cached item (if any)
   *
   * @param key key buffer
   * @param keyOfset key offset
   * @param keySize key size
   * @param hit if true - its a hit
   * @param buffer buffer for item
   * @param bufSize buffer offset
   * @return size of an item (-1 - not found), if is greater than bufSize - retry with a properly
   *     adjusted buffer
   * @throws IOException 
   */
  public long get(byte[] key, int keyOffset, int keySize, boolean hit, byte[] buffer, int bufOffset) 
      throws IOException {
    long result = engine.get(key, keyOffset, keySize, buffer, bufOffset);
    if (result <= buffer.length - bufOffset) {
      access();
      if (result >= 0) {
        hit();
      }
    }
    if (result >=0 && result <= buffer.length - bufOffset) {
      if (this.admissionController != null) {
        this.admissionController.access(key, keyOffset, keySize);
      }
    }
    if(result < 0 && this.victimCache != null) {
      result = this.victimCache.get(key, keyOffset, keySize, hit, buffer, bufOffset);
      if (result >=0 && result <= buffer.length - bufOffset) {
        // put k-v into this cache, remove it from the victim cache
        MemoryIndex mi = this.victimCache.getEngine().getMemoryIndex();
        long expire = mi.getExpire(key, bufOffset, keySize);
        put(buffer, bufOffset, expire);
        this.victimCache.delete(key, keyOffset, keySize);
      } 
    }
    return result;
  }

  /**
   * Get cached item (if any)
   *
   * @param key key buffer
   * @param keyOff key offset
   * @param keySize key size
   * @param hit if true - its a hit
   * @param buffer byte buffer for item
   * @return size of an item (-1 - not found), if is greater than bufSize - retry with a properly
   *     adjusted buffer
   * @throws IOException 
   */
  public long get(byte[] key, int keyOff, int keySize, boolean hit, ByteBuffer buffer) 
      throws IOException {
    int rem = buffer.remaining();
    long result = this.engine.get(key, keyOff, keySize,  buffer);
    if (result <= rem) {
      access();
      if (result >= 0) {
        hit();
      }
    }
    if (result >= 0 && result <= rem) {
      if (this.admissionController != null) {
        this.admissionController.access(key, keyOff, keySize);
      }
    }
    if(result < 0 && this.victimCache != null) {
      result = this.victimCache.get(key, keyOff, keySize, hit, buffer);
      if (result >= 0 && result <= rem) {
        // put k-v into this cache, remove it from the victim cache
        MemoryIndex mi = this.victimCache.getEngine().getMemoryIndex();
        long expire = mi.getExpire(key, keyOff, keySize);
        put(buffer, expire);
        this.victimCache.delete(key, keyOff, keySize);
      } 
    }
    return result;
  }

  /**
   * Get cached item (if any)
   *
   * @param key key buffer
   * @param hit if true - its a hit
   * @param buffer byte buffer for item
   * @return size of an item (-1 - not found), if is greater than bufSize - retry with a properly
   *     adjusted buffer
   * @throws IOException 
   */
  public long get(long keyPtr, int keySize, boolean hit, ByteBuffer buffer) 
      throws IOException {
    int rem = buffer.remaining();
    long result = this.engine.get(keyPtr, keySize,  buffer);
    if (result <= rem) {
      access();
      if (result >= 0) {
        hit();
      }
    }
    if (result >= 0 && result <= rem) {
      if (this.admissionController != null) {
        this.admissionController.access(keyPtr, keySize);
      }
    }
    if(result < 0 && this.victimCache != null) {
      result = this.victimCache.get(keyPtr, keySize, hit, buffer);
      if (result >=0 && result <= rem) {
        // put k-v into this cache, remove it from the victim cache
        MemoryIndex mi = this.victimCache.getEngine().getMemoryIndex();
        long expire = mi.getExpire(keyPtr, keySize);
        put(buffer, expire);
        this.victimCache.delete(keyPtr, keySize);
      } 
    }
    return result;
  }

  /* Delete API*/

  /**
   * Delete cached item
   *
   * @param keyPtr key address
   * @param keySize key size
   * @return true - success, false - does not exist
   */
  public boolean delete(long keyPtr, int keySize) {
    boolean result = engine.getMemoryIndex().delete(keyPtr, keySize);
    if (!result && this.victimCache != null) {
      return this.victimCache.delete(keyPtr, keySize);
    }
    return result;
  }

  /**
   * Delete cached item
   *
   * @param key key
   * @param keyOffset key offset
   * @param keySize key size
   * @return true - success, false - does not exist
   */
  public boolean delete(byte[] key, int keyOffset, int keySize) {
    boolean result = engine.getMemoryIndex().delete(key, keyOffset, keySize);
    if (!result && this.victimCache != null) {
      return this.victimCache.delete(key, keyOffset, keySize);
    }
    return result;
  }

  /**
   * Delete cached item
   *
   * @param key key
   * @param keyOffset key offset
   * @param keyLength key size
   * @return true - success, false - does not exist
   */
  public boolean delete(byte[] key) {
    return delete(key, 0, key.length);
  }
  
  /**
   * Expire cached item
   *
   * @param keyPtr key address
   * @param keySize key size
   * @return true - success, false - does not exist
   */
  public boolean expire(long keyPtr, int keySize) {
    return delete(keyPtr, keySize);
  }

  /**
   * Expire cached item
   *
   * @param key key
   * @param keyOffset key offset
   * @param keySize key size
   * @return true - success, false - does not exist
   */
  public boolean expire(byte[] key, int keyOffset, int keySize) {
    return delete(key, keyOffset, keySize);
  }

  /**
   * Expire cached item
   *
   * @param key key
   * @param keyOffset key offset
   * @param keyLength key size
   * @return true - success, false - does not exist
   */
  public boolean expire(byte[] key) {
    return delete(key, 0, key.length);
  }
  
  /**
   * Get victim cache
   * @return victim cache or null
   */
  public Cache getVictimCache() {
    return this.victimCache;
  }
  
  /**
   * Sets victim cache
   * @param c victim cache
   */
  public void setVictimCache(Cache c) {
    if (getCacheType() == Type.DISK) {
      throw new IllegalArgumentException("Victim cache is not supported for DISK type cache");
    }
    this.victimCache = c;
  }
  
  /**
   * Sets parent cache
   * @param parent cache
   */
  public void setParentCache(Cache parent) {
    this.parentCache = parent;
  }
  
  /**
   * Gets parent cache
   * @return parent cache
   */
  public Cache getParentCache() {
    return this.parentCache;
  }

  private boolean processPromotion(long ptr, long $ptr) {
    if (this.parentCache == null) {
     return false;
    }

    IndexFormat indexFormat = this.engine.getMemoryIndex().getIndexFormat();
    int size = indexFormat.fullEntrySize($ptr);
    try {
      // Check embedded mode
      if (this.conf.isIndexDataEmbeddedSupported()) {
        if (size <= this.conf.getIndexDataEmbeddedSize()) {
          transferEmbeddedToCache(this.parentCache, ptr, $ptr);
          return true;
        }
      }
      // else - not embedded
      // transfer item to victim cache
      transferToCache(this.parentCache, ptr, $ptr);
    } catch (IOException e) {
      //TODO: 
      LOG.error(e);
    }
    return true;
  }

  private void processEviction(long ptr, long $ptr) {
    if (this.victimCache == null) {
     return;
    }
    if (this.admissionController != null && 
        !this.admissionController.shouldEvictToVictimCache(ptr, $ptr)) {
      return;
    }
    IndexFormat indexFormat = this.engine.getMemoryIndex().getIndexFormat();
    int size = indexFormat.fullEntrySize($ptr);
    try {
      // Check embedded mode
      //FIXME: this calls are expensive 
      if (this.conf.isIndexDataEmbeddedSupported()) {
        if (size <= this.conf.getIndexDataEmbeddedSize()) {
          transferEmbeddedToCache(this.victimCache, ptr, $ptr);
          return;
        }
      }
      // else - not embedded
      // transfer item to victim cache
      transferToCache(this.victimCache, ptr, $ptr);
    } catch (IOException e) {
      LOG.error(e);
    }
  }
  /**
   * Transfer cached item to a victim cache
   * @param ibPtr index block pointer
   * @param indexPtr item pointer
   * @throws IOException 
   */
  public void transferToCache (Cache c, long ibPtr, long indexPtr) throws IOException {
    if (getCacheType() == Type.DISK) {
      LOG.error("Attempt to transfer cached item from cache type = DISK");
      throw new IllegalArgumentException("Victim cache is not supported for DISK type cache");
    }
    if (this.victimCache == null) {
      LOG.error("Attempt to transfer cached item when victim cache is null");
      return;
    }

    // Cache is off-heap 
    IndexFormat format = this.engine.getMemoryIndex().getIndexFormat(); 
    long expire = format.getExpire(ibPtr, indexPtr);
    int rank = this.victimCache.getDefaultRankToInsert();
    int sid = (int) format.getSegmentId(indexPtr);
    long offset = format.getOffset(indexPtr); 
    
    Segment s = this.engine.getSegmentById(sid);
    //TODO : check segment
    try {
      s.readLock();
      if (s.isOffheap()) {
        long ptr = s.getAddress();
        ptr += offset;
        int keySize = Utils.readUVInt(ptr);
        int kSizeSize = Utils.sizeUVInt(keySize);
        ptr += kSizeSize;
        int valueSize = Utils.readUVInt(ptr);
        int vSizeSize = Utils.sizeUVInt(valueSize);
        ptr += vSizeSize;
        this.victimCache.put(ptr, keySize, ptr + keySize, valueSize, expire, rank, true);
      } else {
        // not supported yet
      }
    } finally {
      s.readUnlock();
    }
  }
  
  private int getDefaultRankToInsert() {
    return this.engine.getMemoryIndex().getEvictionPolicy().getDefaultRankForInsert();
  }
  /**
   * Transfer cached item to a victim cache
   *
   * @param ibPtr index block pointer
   * @param indexPtr item pointer
   * @throws IOException
   */
  public void transferEmbeddedToCache(Cache c, long ibPtr, long indexPtr) throws IOException {
    if (getCacheType() == Type.DISK) {
      LOG.error("Attempt to transfer cached item from cache type = DISK");
      throw new IllegalArgumentException("Victim cache is not supported for DISK type cache");
    }
    if (this.victimCache == null) {
      LOG.error("Attempt to transfer cached item when victim cache is null");
      return;
    }
    // Cache is off-heap
    IndexFormat format = this.engine.getMemoryIndex().getIndexFormat();
    long expire = format.getExpire(ibPtr, indexPtr);
    int rank = this.victimCache.getDefaultRankToInsert();

    int off = format.getEmbeddedOffset();
    indexPtr += off;
    int kSize = Utils.readUVInt(indexPtr);
    int kSizeSize = Utils.sizeUVInt(kSize);
    indexPtr += kSizeSize;
    int vSize = Utils.readUVInt(indexPtr);
    int vSizeSize = Utils.sizeUVInt(vSize);
    indexPtr += vSizeSize;
    
    c.put(indexPtr, kSize, indexPtr + kSize, vSize, expire, rank, true);
  }

  // IOEngine.Listener
  @Override
  public void onEvent(IOEngine e, IOEngineEvent evt) {
//    if (evt == IOEngineEvent.DATA_SIZE_CHANGED) {
//      double used = getMemoryUsedPct();
//      //TODO: performance
//      double max = this.conf.getScavengerStartMemoryRatio(this.cacheName);
//      double min = this.conf.getScavengerStopMemoryRatio(this.cacheName);
//      if (used >= max && (this.scavenger == null || !this.scavenger.isAlive())) {
//        this.scavenger = new Scavenger(this);
//        this.scavenger.start();
//        this.engine.setEvictionEnabled(true);
//        this.tcEnabled = true;
//      } else if (used < min) {
//        // Disable eviction
//        this.engine.setEvictionEnabled(false);
//      }
//    }
  }
  
  // Persistence section
  
  /**
   * Loads cache meta data
   * @throws IOException
   */
  private void loadCache() throws IOException {
    String snapshotDir = this.conf.getSnapshotDir(this.cacheName);
    String file = CacheConfig.CACHE_SNAPSHOT_NAME;
    Path p = Paths.get(snapshotDir, file);
    if (Files.exists(p)) {
      FileInputStream fis = new FileInputStream(p.toFile());
      DataInputStream dis = new DataInputStream(fis);
      this.allocatedMemory.set(dis.readLong());
      this.usedMemory.set(dis.readLong());
      this.totalGets.set(dis.readLong());
      this.totalHits.set(dis.readLong());
      this.totalWrites.set(dis.readLong());
      this.totalRejectedWrites.set(dis.readLong());
      Epoch.setEpochStartTime(dis.readLong());
      this.tcEnabled = dis.readBoolean();
      dis.close();
    }
  }
  
  /**
   * Saves cache meta data
   * @throws IOException
   */
  private void saveCache() throws IOException {
    String snapshotDir = this.conf.getSnapshotDir(this.cacheName);
    String file = CacheConfig.CACHE_SNAPSHOT_NAME;
    Path p = Paths.get(snapshotDir, file);
    FileOutputStream fos = new FileOutputStream(p.toFile());
    DataOutputStream dos = new DataOutputStream(fos);
    dos.writeLong(allocatedMemory.get());
    dos.writeLong(usedMemory.get());
    dos.writeLong(totalGets.get());
    dos.writeLong(totalHits.get());
    dos.writeLong(totalWrites.get());
    dos.writeLong(totalRejectedWrites.get());
    dos.writeLong(Epoch.getEpochStartTime());
    dos.writeBoolean(this.tcEnabled);
    dos.close();
  }
    
  /**
   * Loads admission controller data
   * @throws IOException
   */
  private void loadAdmissionControlller() throws IOException {
    if (this.admissionController == null) {
      return;
    }
    String snapshotDir = this.conf.getSnapshotDir(this.cacheName);
    String file = CacheConfig.ADMISSION_CONTROLLER_SNAPSHOT_NAME;
    Path p = Paths.get(snapshotDir, file);
    if (Files.exists(p) && Files.size(p) > 0) {
      FileInputStream fis = new FileInputStream(p.toFile());
      DataInputStream dis = new DataInputStream(fis);
      this.admissionController.load(dis);
      dis.close();
    }
  }
  
  /**
   * Saves admission controller data
   * @throws IOException
   */
  private void saveAdmissionController() throws IOException {
    if (this.admissionController == null) {
      return;
    }
    String snapshotDir = this.conf.getSnapshotDir(this.cacheName);
    String file = CacheConfig.ADMISSION_CONTROLLER_SNAPSHOT_NAME;
    Path p = Paths.get(snapshotDir, file);
    FileOutputStream fos = new FileOutputStream(p.toFile());
    DataOutputStream dos = new DataOutputStream(fos);
    this.admissionController.save(dos);
    dos.close();
  }
  
  /**
   * Loads throughput controller data
   * @throws IOException
   */
  private void loadThroughputControlller() throws IOException {
    if (this.throughputController == null) {
      return;
    }
    String snapshotDir = this.conf.getSnapshotDir(this.cacheName);
    String file = CacheConfig.THROUGHPUT_CONTROLLER_SNAPSHOT_NAME;
    Path p = Paths.get(snapshotDir, file);
    if (Files.exists(p) && Files.size(p) > 0) {
      FileInputStream fis = new FileInputStream(p.toFile());
      DataInputStream dis = new DataInputStream(fis);
      this.throughputController.load(dis);
      dis.close();
    }
  }
  
  /**
   * Saves throughput controller data
   * @throws IOException
   */
  private void saveThroughputController() throws IOException {
    if (this.throughputController == null) {
      return;
    }
    String snapshotDir = this.conf.getSnapshotDir(this.cacheName);
    String file = CacheConfig.THROUGHPUT_CONTROLLER_SNAPSHOT_NAME;
    Path p = Paths.get(snapshotDir, file);
    FileOutputStream fos = new FileOutputStream(p.toFile());
    DataOutputStream dos = new DataOutputStream(fos);
    this.throughputController.save(dos);
    dos.close();
  }
  
  /**
   * Loads scavenger statistics data
   * @throws IOException
   */
  private void loadScavengerStats() throws IOException {
    String snapshotDir = this.conf.getSnapshotDir(this.cacheName);
    String file = CacheConfig.SCAVENGER_STATS_SNAPSHOT_NAME;
    Path p = Paths.get(snapshotDir, file);
    if (Files.exists(p) && Files.size(p) > 0) {
      FileInputStream fis = new FileInputStream(p.toFile());
      DataInputStream dis = new DataInputStream(fis);
      Scavenger.stats.load(dis);
      dis.close();
    }
  }
  
  /**
   * Saves scavenger statistics data
   * @throws IOException
   */
  private void saveScavengerStats() throws IOException {
    String snapshotDir = this.conf.getSnapshotDir(this.cacheName);
    String file = CacheConfig.SCAVENGER_STATS_SNAPSHOT_NAME;
    Path p = Paths.get(snapshotDir, file);
    FileOutputStream fos = new FileOutputStream(p.toFile());
    DataOutputStream dos = new DataOutputStream(fos);
    Scavenger.stats.save(dos);
    dos.close();
  }
  
  /**
   * Loads engine data
   * @throws IOException
   */
  private void loadEngine() throws IOException {
    String snapshotDir = this.conf.getSnapshotDir(this.cacheName);
    String file = CacheConfig.CACHE_ENGINE_SNAPSHOT_NAME;
    Path p = Paths.get(snapshotDir, file);
    if (Files.exists(p) && Files.size(p) > 0) {
      FileInputStream fis = new FileInputStream(p.toFile());
      DataInputStream dis = new DataInputStream(fis);
      this.engine.load(dis);
      dis.close();
    }
  }
  
  /**
   * Saves engine data
   * @throws IOException
   */
  private void saveEngine() throws IOException {
    String snapshotDir = this.conf.getSnapshotDir(this.cacheName);
    String file = CacheConfig.CACHE_ENGINE_SNAPSHOT_NAME;
    Path p = Paths.get(snapshotDir, file);
    FileOutputStream fos = new FileOutputStream(p.toFile());
    DataOutputStream dos = new DataOutputStream(fos);
    this.engine.save(dos);
    dos.close();
  }
  
  /**
   * Save cache data and meta-data
   * @throws IOException
   */
  public void save() throws IOException {
    LOG.info("Started saving cache ...");
    long startTime = System.currentTimeMillis();
    saveCache();
    saveAdmissionController();
    saveThroughputController();
    saveEngine();
    saveScavengerStats();
    long endTime = System.currentTimeMillis();
    LOG.info("Cache saved in {}ms", endTime - startTime);
  }
  
  /**
   * Load cache data and meta-data from a file system
   * @throws IOException
   */
  public void load() throws IOException {
    LOG.info("Started loading cache ...");
    long startTime = System.currentTimeMillis();
    loadCache();
    loadAdmissionControlller();
    loadThroughputControlller();
    loadEngine();
    loadScavengerStats();
    long endTime = System.currentTimeMillis();
    LOG.info("Cache loaded in {}ms", endTime - startTime);
  }

  // EvictionListener
  @Override
  public void onEviction(long ibPtr, long ptr) {
    processEviction(ibPtr, ptr);
  }
  
  @Override
  public boolean onPromotion(long ibPtr, long ptr) {
    return processPromotion(ibPtr, ptr);
  }

}
