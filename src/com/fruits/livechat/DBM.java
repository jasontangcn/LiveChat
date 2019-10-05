package com.fruits.livechat;/*
 * $Id: DBM.java,v 2.1 2004/12/02 09:05:48 justin Exp $
 *
 * Copyright (c) 2000, 2001 Apusic Software Inc.
 * All rights reserved
 */


import java.io.*;
import java.util.*;

/**
 * A Java implementation of dbm(3) interface.
 * 
 * @preserve public
 */
public class DBM
{
  /* Parameters to open. */
  public static final int READER            = 0;         // READERS only.
  public static final int WRITER            = 1;         // READERS and WRITERS.
  public static final int NEWDB             = 2;         // ALWAYS create a new db. (WRITER)
  public static final int OPENMASK          = 7;         // Mask for the above.
  public static final int FAST              = 16;        // Write fast! => No syncs.

  /*
   * Parameters to store() for simple insertion or replacement in the case a key to store is already in the database.
   */
  public static final int INSERT            = 0;         // Do not overwrite data in the db.
  public static final int REPLACE           = 1;         // Replace the old value with new one.

  /* The magic number. */
  static final int        HEADER_MAGIC      = 0x97050eb5;

  /* The default block size. */
  static final int        BLOCK_SIZE        = 4096;

  /*
   * In freeing blocks, we will ignore any blocks smaller (and equal) to IGNORE_SIZE number of bytes.
   */
  static final int        IGNORE_SIZE       = 4;

  /* The number of key bytes kept in a hash bucket. */
  static final int        SMALL             = 4;

  /* The number of bucket available entries in a hash bucket. */
  static final int        BUCKET_AVAIL      = 6;

  /* The size of the bucket cache. */
  static final int        DEFAULT_CACHESIZE = 20;

  /*
   * The dbm file header keeps track of the current location of the hash directory and the free space in the file.
   */
  int                     header_magic;                  // To make sure the header is good.
  int                     block_size;                    // The I/O block size
  
  int                     dir_adr;                       // File address of hash directory table.
  int                     dir_size;                      // Size of the directory table.
  int                     dir_bits;                      // The number of address bits used in the table.
  
  int                     bucket_size;                   // Size in bytes of a hash bucket struct.
  int                     bucket_elems;                  // Number of elements in a hash bucket.
  
  int                     next_block;                    // The next unallocated block address.
  int                     av_length;                     // The number of avail elements in the table.
  int                     av_count;                      // The number of entries in the table.
  int                     av_next_block;                 // The file address of the next avail block.
  int                     av_size[];                     // The size of the available block.
  int                     av_adr[];                      // The file address of the available block.

  /* Size of disk image of above (exclude avail block). */
  static final int        HEADER_SIZE       = 4 * 11;

  /*
   * The dbm hash bucket element contains the full 31 bit hash value, 
   * the "pointer" to the key and data(stored together) with their sizes. 
   * It also has a small part of the actual key value. 
   * It is used to verify the first part of the key has the correct value without having to read the actual key.
   */
  static class BucketElement
  {
    int              hash_value;                      // The complete 31 bit value.
    byte             key_start[];                     // Up to the first SMALL bytes of the key.
    int              data_pointer;                    // The file address of the key record. The
                                                      // data record directly follows the key.
    int              key_size;                        // Size of key data in the file.
    int              data_size;                       // Size of associated data in the file.
    long             lastmod;                         // Last modified time.

    static final int SIZE = 4 + SMALL + 4 + 4 + 4 + 8;

    BucketElement()
    {
      this.hash_value = -1;
      this.key_start = new byte[SMALL];
      this.data_pointer = 0;
      this.key_size = 0;
      this.data_size = 0;
      this.lastmod = 0L;
    }
  }

  /*
   *  A bucket is a small hash table.
   *  This one consists of a number of bucket elements plus some bookkeeping fields. 
   *  The number of elements depends on the blocksize given at file creation time. 
   *  This bucket takes one block. 
   *  When one of these tables gets full, it is split into two hash buckets. 
   *  The contents are split between them by the use of the first few bits of the 31 bit hash function. 
   *  The location in a bucket is the hash value modulo the size of the bucket.
   */
  static class HashBucket
  {
    int              av_count;                                 // The number of bucket avail entries.
    int              av_size[];                                // Distributed avail.
    int              av_adr[];
    int              bucket_bits;                              // The number of bits used to get here.
    int              count;                                    // The number of element buckets full.
    BucketElement    h_table[];                                // The table.

    static final int SIZE = 4 + (4 + 4) * BUCKET_AVAIL + 4 + 4;

    HashBucket(DBM dbm, int bits)
    {
      this.av_count = 0;
      this.av_size = new int[BUCKET_AVAIL];
      this.av_adr = new int[BUCKET_AVAIL];
      this.bucket_bits = bits;
      this.count = 0;
      this.h_table = new BucketElement[dbm.bucket_elems];
      for (int i = 0; i < h_table.length; i++)
        h_table[i] = new BucketElement();
    }

    void addAvail(int adr, int size, boolean can_merge)
    {
      // Is it too small to deal with?
      if (size <= IGNORE_SIZE) return;

      if (can_merge) {
        // Search for blocks to coalesce with this one.
        for (int i = 0; i < av_count; i++) {
          // Can we merge with the previous block?
          if (av_adr[i] + av_size[i] == adr) {
            // Simply expand the entry.
            av_size[i] += size;
            return;
          }
          // Can we merge with the next block?
          else if (adr + size == av_adr[i]) {
            // Update this entry
            av_adr[i] = adr;
            av_size[i] += size;
            return;
          }
        }
      }

      // Search for place to put element. List is sorted by size.
      for (int i = 0; i < av_count; i++) {
        if (av_size[i] >= size) {
          System.arraycopy(av_size, i, av_size, i + 1, av_count - i);
          System.arraycopy(av_adr, i, av_adr, i + 1, av_count - i);
          av_size[i] = size;
          av_adr[i] = adr;
          av_count++;
          return;
        }
      }

      // Add the new element at end.
      av_size[av_count] = size;
      av_adr[av_count] = adr;
      av_count++;
    }

    void removeAvail(int index)
    {
      av_count--;
      if (index < av_count) {
        System.arraycopy(av_size, index + 1, av_size, index, av_count - index);
        System.arraycopy(av_adr, index + 1, av_adr, index, av_count - index);
      }
    }
  }

  /*
   * We want to keep from reading buckets as much as possible. The following is to implement a bucket cache. 
   * When full, buckets will be dropped in a least recently read from disk order.
   */
  static class CacheElement
  {
    HashBucket bucket;
    int        adr;
    boolean    changed;

    /*
     * To speed up fetching and "sequential" access, we need to implement a data cache for key/data pairs read from the file. 
     * To find a key, we must exactly match the key from the file. To reduce overhead, the data will be read at the same time. 
     * Both key and data will be stored in a data cache. Each bucket cache will have a one element
     */

    int        hash_val;
    int        data_size;
    int        key_size;
    byte[]     data;
    int        elem_loc;

    CacheElement()
    {
      this.bucket = null;
      this.adr = 0;
      this.changed = false;
      this.hash_val = -1;
      this.elem_loc = -1;
      this.data = null;
    }
  }

  /*
   * The final structure contains all main memory based information for a DBM file.
   *  This allows multiple dbm files to be opened at the same time by one program.
   */

  /* The reader/writer status. */
  int              read_write;

  /* Fast_write is set to TRUE if no syncs are to be done. */
  boolean          fast_write;

  /* The file descriptor which is set when open. */
  RandomAccessFile fd;

  /* The hash table directory from extendible hashing. */
  int              dir[];

  /* The bucket cache. */
  CacheElement     bucket_cache[];
  int              cache_size;
  int              last_read;

  /* Pointer to the current bucket's cache entry. */
  CacheElement     cache_entry;
  HashBucket       bucket;
  int              bucket_dir;

  /*
   * Bookkeeping of things that need to be written back at the end of an update.
   */
  boolean          header_changed;
  boolean          directory_changed;
  boolean          bucket_changed;
  boolean          second_changed;

  /* IO buffer, for all read/write operations. */
  private byte     buffer[];

  /**
   * Initialize dbm system. 
   * If the file has a size of zero bytes, a file initialization procedure is performed, 
   * setting up the initial structure in the file. 
   * <code>blocksize</code> is used during initialization to determine the size of various constructs. 
   * <code>blocksize</code> is ignored if the file has previously initialized. 
   * If <code>flags</code> is set to READER the user wants to just read the database and any call to store() or delete() will fail.
   * Many readers can access the database at the same time. 
   * If <code>flags</code> is set to WRITER, the user wants both read and write
   * access to the database and requires exclusive access to the database and if the database does not exist,create a new one. 
   * If <code>flags</code> is NEWDB, the user want a new database created, regardless of whether one existed, 
   * and wants read and write access to the new database.
   */
  public DBM(String name, int blocksize, int flags) throws IOException
  {
    boolean need_trunc = false;

    // Determine whether or not we set fast_write.
    this.fast_write = ((flags & FAST) != 0);

    // Open the file
    switch (flags & OPENMASK) {
      case READER:
        this.fd = new RandomAccessFile(name, "r");
        if (fd.length() == 0) {
          fd.close();
          throw new IOException("Empty database.");
        }
        break;

      case WRITER:
        this.fd = new RandomAccessFile(name, "rw");
        break;

      case NEWDB:
        this.fd = new RandomAccessFile(name, "rw");
        flags = WRITER;
        need_trunc = true;
        break;

      default:
        this.fd = new RandomAccessFile(name, "rw");
        flags = WRITER;
        break;
    }

    // Record the kind of user.
    this.read_write = flags & OPENMASK;

    // If we do have a write lock and it was NEWDB, it is now time to truncate the file.
    if (need_trunc && fd.length() != 0) fd.setLength(0);

    // Decide if this is a new file or an old file.
    if (fd.length() == 0) {
      // This is a new file. Create an empty database.

      // Start with the blocksize.
      if (blocksize < 512) blocksize = BLOCK_SIZE;

      // Set the magic number and the block_size
      this.header_magic = HEADER_MAGIC;
      this.block_size = blocksize;
      this.buffer = new byte[block_size];

      // Create the intial hash table directory.
      this.dir_size = 8;
      this.dir_bits = 3;
      
      // block_size 512
      //  -> block_size/4 = 128
      //    -> 8 * 2 * 2 * 2 * 2 = 128
      //      -> dir_size = 128
      //         dir_bits = 7
      while (dir_size * 4 < block_size) {
        dir_size <<= 1;
        dir_bits += 1;
      }

      // Check for correct block_size
      if (dir_size * 4 != block_size) {
        close();
        throw new IOException("Block size error.");
      }

      // Allocate the space for the directory.
      this.dir = new int[dir_size]; // -> dir_size = 128
      this.dir_adr = block_size;    // -> dir_adr = block_size = 512

      // Create the first and only hash bucket.
      this.bucket_elems = (block_size - HashBucket.SIZE) / BucketElement.SIZE;
      this.bucket_size = block_size;
      HashBucket bucket = new HashBucket(this, 0);
      bucket.av_count = 1;
      bucket.av_adr[0] = 3 * block_size;
      bucket.av_size[0] = block_size;

      // Set the table entries to point to hash buckets.
      for (int i = 0; i < dir_size; i++)
        dir[i] = 2 * block_size;

      // Initialize the active avail block.
      this.av_length = ((block_size - HEADER_SIZE) / (2 * 4));
      this.av_size = new int[av_length];
      this.av_adr = new int[av_length];
      this.av_count = 0;
      this.av_next_block = 0;                          
      this.next_block = 4 * block_size;

      // Write initial configuration to the file.
      try {
        // Block 0 is the file header and active avail block.
        writeHeader();
        // Block 1 is the initial bucket directory.
        writeDirectory();
        // Block 2 is the only bucket.
        writeBucket(bucket, 2 * block_size);

        // Wait for initial configuration to be written to disk.
        if (!fast_write) {
          fd.getFD().sync();
        }
      } catch (IOException e) {
        close();
        throw e;
      }
    } else {
      // This is an old database. Read in information from the file header and initialize the hash directory.
      try {
        // Read the partial file header.
        byte[] b = new byte[2 * 4];
        if (fd.read(b) != b.length) throw new IOException("Unable to read dbm header.");
        this.header_magic = getInt(b, 0);
        this.block_size = getInt(b, 1);

        // Is the magic number good?
        if (header_magic != HEADER_MAGIC) throw new IOException("Bad magic number.");

        // It is a good database, read the entrie header
        this.buffer = new byte[block_size];
        readHeader();

        // Read the hash table directory.
        this.dir = new int[dir_size];
        readDirectory();
      } catch (IOException e) {
        close();
        throw e;
      }
    }

    // Initialize the bucket cache.
    this.cache_size = DEFAULT_CACHESIZE;
    this.bucket_cache = new CacheElement[cache_size];
    for (int i = 0; i < cache_size; i++)
      this.bucket_cache[i] = new CacheElement();
    this.cache_entry = bucket_cache[0];
    this.bucket = cache_entry.bucket = new HashBucket(this, 0);
    this.last_read = -1;

    // Finish initialize dbf.
    this.header_changed = false;
    this.directory_changed = false;
    this.bucket_changed = false;
    this.second_changed = false;
  }

  public DBM(File file, int blocksize, int flags) throws IOException
  {
    this(file.getPath(), blocksize, flags);
  }

  public DBM(String name, int flags) throws IOException
  {
    this(name, BLOCK_SIZE, flags);
  }

  public DBM(File file, int flags) throws IOException
  {
    this(file.getPath(), BLOCK_SIZE, flags);
  }

  public synchronized void close() throws IOException
  {
    fd.close();
  }

  /**
   * Add a new element to the database.
   * <code>Content</code> is keyed by <code>key</code>. 
   * The file on disk is updated to reflect the structure of the new database before returning from this procedure. 
   * The <code>flags</code> defines the action to take when the <code>key</code> is already in the database. 
   * The value <code>REPLACE</code> asks that the old data be replaced by the new <code>content</code>. 
   * The value <code>INSERT</code> asks that an error be returned and no action taken. 
   * A return value of <code>true</code> means no errors.
   * A return value of <code>false</code> means that the item was not stored 
   * because the argument <code>flags</code> was <code>INSERT</code> and the <code>key</code> was already in the database.
   */
  public synchronized boolean store(byte[] key, byte[] content, int flags) throws IOException
  {
    int new_hash_val; // The new hash value.
    int elem_loc; // The location in hash bucket.
    int file_adr; // The address of new space in the file.
    int free_adr; // The address of new space in the file.
    int free_size;
    int new_size; // Used in allocating space.

    // First check to make sure this guy is a writer.
    if (read_write != WRITER) throw new IOException("Reader can't store.");

    // Check for illegal data values.
    if (key == null || content == null) throw new NullPointerException();

    // Look for the key in the file.
    // A side effect loads the correct bucket.
    new_hash_val = hash(key);
    elem_loc = findKey(key, new_hash_val);

    // Initialize these
    file_adr = 0;
    new_size = key.length + content.length;

    // Did we find the item?
    if (elem_loc != -1) {
      if (flags == REPLACE) {
        // Just replace the data.
        free_adr = bucket.h_table[elem_loc].data_pointer;
        free_size = bucket.h_table[elem_loc].key_size + bucket.h_table[elem_loc].data_size;
        if (free_size != new_size) {
          free(free_adr, free_size);
        } else {
          // Just reuse the same address!
          file_adr = free_adr;
        }
      } else {
        return false;
      }
    }

    // Get the file address for the new space.
    // (Current bucket's free space is first place to look.)
    if (file_adr == 0) {
      file_adr = alloc(new_size);
    }

    // If this is a new entry in the bucket, we need to do special things.
    if (elem_loc == -1) {
      if (bucket.count == bucket_elems) {
        // Split the current bucket.
        splitBucket(new_hash_val);
      }

      // Find space to insert into bucket and set elem_loc to that place.
      elem_loc = new_hash_val % bucket_elems;
      while (bucket.h_table[elem_loc].hash_value != -1) {
        elem_loc = (elem_loc + 1) % bucket_elems;
      }

      // We now have another element in the bucket. Add the new info.
      bucket.count++;
      bucket.h_table[elem_loc].hash_value = new_hash_val;
      System.arraycopy(key, 0, bucket.h_table[elem_loc].key_start, 0, (SMALL < key.length ? SMALL : key.length));
    }

    // Update current bucket data pointer and sizes.
    bucket.h_table[elem_loc].data_pointer = file_adr;
    bucket.h_table[elem_loc].key_size = key.length;
    bucket.h_table[elem_loc].data_size = content.length;
    bucket.h_table[elem_loc].lastmod = System.currentTimeMillis();

    // Write the data to the file.
    fd.seek(file_adr);
    fd.write(key);
    fd.write(content);

    // Current bucket has changed.
    cache_entry.changed = true;
    bucket_changed = true;

    // Write everything that is needed to the disk.
    endUpdate();
    return true;
  }

  public boolean store(String key, byte[] content, int mode) throws IOException
  {
    return store(key.getBytes("UTF8"), content, mode);
  }

  /**
   * Sets the last modified time of element.
   */
  public synchronized void setLastModifiedTime(byte[] key, long time) throws IOException
  {
    int hash_val;
    int elem_loc;

    // First check to make sure this guy is a writer.
    if (read_write != WRITER) throw new IOException("Reader can't change.");

    // Look for the key in the file.
    hash_val = hash(key);
    elem_loc = findKey(key, hash_val);

    // Did we find the item?
    if (elem_loc == -1) throw new IOException("Item not found.");

    // Update item information.
    bucket.h_table[elem_loc].lastmod = time;

    // Current bucket has changed.
    cache_entry.changed = true;
    bucket_changed = true;

    // Write everything that is needed to the disk.
    endUpdate();
  }

  public void setLastModifiedTime(String key, long time) throws IOException
  {
    setLastModifiedTime(key.getBytes("UTF8"), time);
  }

  /**
   * Gets the last modified time of element.
   */
  public synchronized long getLastModifiedTime(byte[] key) throws IOException
  {
    int hash_val;
    int elem_loc;

    // Look for the key in the file.
    hash_val = hash(key);
    elem_loc = findKey(key, hash_val);
    if (elem_loc == -1) // not found
      return 0;

    return bucket.h_table[elem_loc].lastmod;
  }

  public long getLastModifiedTime(String key) throws IOException
  {
    return getLastModifiedTime(key.getBytes("UTF8"));
  }

  /**
   * Test if a given key exists in the database.
   */
  public synchronized boolean contains(byte[] key) throws IOException
  {
    int hash_val = hash(key);
    return findKey(key, hash_val) >= 0;
  }

  public boolean contains(String key) throws IOException
  {
    return contains(key.getBytes("UTF8"));
  }

  /**
   * Look up a given key and return the information associated with that key.
   */
  public synchronized byte[] fetch(byte[] key) throws IOException
  {
    byte[] return_val; // The return value;
    int elem_loc; // The location in the bucket.
    int hash_val; // The hash value.

    // Find the key.
    hash_val = hash(key);
    elem_loc = findKey(key, hash_val);

    // Copy the data if the key was found.
    if (elem_loc >= 0) {
      // This is the item.
      // Return the associated data.
      return_val = new byte[cache_entry.data_size];
      System.arraycopy(cache_entry.data, cache_entry.key_size, return_val, 0, cache_entry.data_size);
      return return_val;
    }

    return null;
  }

  public byte[] fetch(String key) throws IOException
  {
    return fetch(key.getBytes("UTF8"));
  }

  /**
   * Remove the keyed item and the key from the database. 
   * The file on disk is updated to reflect the structure of the new database before returning from this procedure.
   */
  public synchronized boolean delete(byte[] key) throws IOException
  {
    int elem_loc; // The location in the current hash bucket.
    int last_loc; // Last location emptied by the delete.
    int home; // Home position of an item.
    int hash_val; // The hash value.
    int free_adr; // Temporary storage for address and size.
    int free_size;

    // First check to make sure this guy is a writer.
    if (read_write != WRITER) {
      throw new IOException("Reader can't delete.");
    }

    // Find the item.
    hash_val = hash(key);
    elem_loc = findKey(key, hash_val);
    if (elem_loc == -1) return false;

    // Free the file space.
    free_adr = bucket.h_table[elem_loc].data_pointer;
    free_size = cache_entry.key_size + cache_entry.data_size;
    free(free_adr, free_size);

    // Delete the element.
    bucket.h_table[elem_loc].hash_value = -1;
    bucket.count--;

    // Move other elements to guarantee that they can be found.
    last_loc = elem_loc;
    elem_loc = (elem_loc + 1) % bucket_elems;
    while (elem_loc != last_loc && bucket.h_table[elem_loc].hash_value != -1) {
      home = bucket.h_table[elem_loc].hash_value % bucket_elems;
      if ((last_loc < elem_loc && (home <= last_loc || home > elem_loc)) || (last_loc > elem_loc && home <= last_loc && home > elem_loc)) {
        bucket.h_table[last_loc] = bucket.h_table[elem_loc];
        bucket.h_table[elem_loc] = new BucketElement();
        last_loc = elem_loc;
      }
      elem_loc = (elem_loc + 1) % bucket_elems;
    }

    // Set the flags.
    bucket_changed = true;

    // Clear out the data cache for the current bucket.
    cache_entry.hash_val = -1;
    cache_entry.key_size = 0;
    cache_entry.elem_loc = -1;

    // Do the writes.
    endUpdate();
    return true;
  }

  public boolean delete(String key) throws IOException
  {
    return delete(key.getBytes("UTF8"));
  }

  /**
   * Start the visit of all keys in the database. 
   * This produces something in hash order, not in any sorted order.
   */
  public synchronized byte[] firstkey() throws IOException
  {
    // Get the first bucket.
    loadBucket(0);

    // Look for first entry.
    return getNextKey(-1);
  }

  /**
   * Continue visiting all keys. The next key following key is returned.
   */
  public synchronized byte[] nextkey(byte[] key) throws IOException
  {
    if (key == null) return null;

    // Find the key.
    int hash_val = hash(key);
    int elem_loc = findKey(key, hash_val);
    if (elem_loc == -1) return null;

    // Find the next key.
    return getNextKey(elem_loc);
  }

  /*
   * Find and read the next entry in the hash structure starting at elem_loc of the current bucket.
   */
  private byte[] getNextKey(int elem_loc) throws IOException
  {
    boolean found;
    byte[] find_data;
    byte[] return_val;

    // Find the next key.
    found = false;
    while (!found) {
      // Advance to the next location in the bucket.
      elem_loc++;
      if (elem_loc == bucket_elems) {
        // We have finished the current bucket, get the next bucket.
        elem_loc = 0;

        // Find the next bucket. 
        // It is possible serveral entries in the bucket directory point to the same bucket.
        while (bucket_dir < dir_size && dir[bucket_dir] == cache_entry.adr)
          bucket_dir++;

        // Check to see if there was a next bucket.
        if (bucket_dir < dir_size) {
          loadBucket(bucket_dir);
        } else {
          // No next key, just return.
          return null;
        }
      }
      found = bucket.h_table[elem_loc].hash_value != -1;
    }

    // Found the next key, read it into return_val.
    find_data = readEntry(elem_loc);
    return_val = new byte[bucket.h_table[elem_loc].key_size];
    System.arraycopy(find_data, 0, return_val, 0, return_val.length);
    return return_val;
  }

  private static final int KEYS        = 0;
  private static final int STRING_KEYS = 1;
  private static final int ELEMENTS    = 2;

  class Enumerator implements Enumeration
  {
    private int    type;
    private byte[] next;

    Enumerator(int type)
    {
      this.type = type;
      try {
        next = firstkey();
      } catch (IOException e) {
        throw new RuntimeException("DBM enumeration failure.");
      }
    }

    public boolean hasMoreElements()
    {
      return next != null;
    }

    public Object nextElement()
    {
      if (next == null) throw new NoSuchElementException();

      try {
        byte[] key = next;
        next = nextkey(next);
        return type == KEYS ? (Object) key : type == STRING_KEYS ? (Object) new String(key, "UTF8") : fetch(key);
      } catch (IOException e) {
        throw new RuntimeException("DBM enumeration failure.");
      }
    }
  }

  public Enumeration keys()
  {
    return new Enumerator(KEYS);
  }

  public Enumeration stringKeys()
  {
    return new Enumerator(STRING_KEYS);
  }

  public Enumeration elements()
  {
    return new Enumerator(ELEMENTS);
  }

  /*
   * This hash function computes a 31 bit value. 
   * The value is used to index the hash directory using the top n bits. 
   * It is also used in a hash bucket to find the home position of the element 
   * by taking the value modulo the bucket hash table size.
   */
  private int hash(byte[] key)
  {
    int value = 0x238F13AF * key.length;
    for (int i = 0; i < key.length; i++)
      value = (value + ((int) key[i] << (i * 5 % 24))) & 0x7FFFFFFF;
    return (1103515243 * value + 12345) & 0x7FFFFFFF;
  }

  /*
   * Find the key in the file and get ready to read the associated data. 
   * The return value is the location in the current hash bucket of the key's entry.
   */
  private int findKey(byte[] key, int new_hash_val) throws IOException
  {
    int bucket_hash_val; // The hash value from the bucket.
    byte[] file_key; // The complte key as stored in the file.
    int elem_loc; // The location in the bucket.
    int home_loc; // The home location in the bucket.
    int key_size; // Size of the key on the file.

    // Load proper bucket.
    loadBucket(new_hash_val >> (31 - dir_bits));

    // Is the element the last one found for this bucket?
    if (cache_entry.elem_loc != -1 && cache_entry.hash_val == new_hash_val && cache_entry.key_size == key.length && cache_entry.data != null && arrayEquals(cache_entry.data, key, key.length)) {
      // This is it. Return the cache pointer.
      return cache_entry.elem_loc;
    }

    // It is not the cached value, search for element in the bucket. */
    elem_loc = new_hash_val % bucket_elems;
    home_loc = elem_loc;
    bucket_hash_val = bucket.h_table[elem_loc].hash_value;
    while (bucket_hash_val != -1) {
      key_size = bucket.h_table[elem_loc].key_size;
      if (bucket_hash_val != new_hash_val || key_size != key.length || !arrayEquals(bucket.h_table[elem_loc].key_start, key, (SMALL < key_size ? SMALL : key_size))) {
        // Current elem_loc is not the item, go to next item.
        elem_loc = (elem_loc + 1) % bucket_elems;
        if (elem_loc == home_loc) return -1;
        bucket_hash_val = bucket.h_table[elem_loc].hash_value;
      } else {
        // This may be the one we want.
        // The only way to tell is to read it.
        file_key = readEntry(elem_loc);
        if (arrayEquals(file_key, key, key_size)) {
          // This is the item.
          return elem_loc;
        } else {
          // Not the item, try the next one. Return if not found.
          elem_loc = (elem_loc + 1) % bucket_elems;
          if (elem_loc == home_loc) return -1;
          bucket_hash_val = bucket.h_table[elem_loc].hash_value;
        }
      }
    }

    // If we get here, we never found the key.
    return -1;
  }

  /*
   * Read the data found in bucket entry elem_loc in file and return a pointer to it. 
   * Also, cache the read value.
   */
  private byte[] readEntry(int elem_loc) throws IOException
  {
    // Is it already in the cache?
    if (cache_entry.elem_loc == elem_loc) return cache_entry.data;

    // Set sizes and pointers.
    int key_size = bucket.h_table[elem_loc].key_size;
    int data_size = bucket.h_table[elem_loc].data_size;

    // Set up the cache.
    cache_entry.key_size = key_size;
    cache_entry.data_size = data_size;
    cache_entry.elem_loc = elem_loc;
    cache_entry.hash_val = bucket.h_table[elem_loc].hash_value;
    cache_entry.data = new byte[key_size + data_size];

    // Read into the cache.
    fd.seek(bucket.h_table[elem_loc].data_pointer);
    if (fd.read(cache_entry.data) != (key_size + data_size)) throw new IOException("Read error.");

    return cache_entry.data;
  }

  /*
   * Find a bucket that is pointed to by the bucket directory from location dir_index. 
   * The bucket cache is first checked to see if it is already in memory. 
   * If not, a bucket may be tossed to read the new bucket.
   */
  private void loadBucket(int dir_index) throws IOException
  {
    int bucket_adr; // The address of the correct hash bucket.

    // Initial set up.
    bucket_dir = dir_index;
    bucket_adr = dir[dir_index];

    // Is that one is not already current, we must find it.
    if (cache_entry.adr != bucket_adr) {
      // Look in the cache.
      for (int i = 0; i < cache_size; i++) {
        if (bucket_cache[i].adr == bucket_adr) {
          cache_entry = bucket_cache[i];
          bucket = cache_entry.bucket;
          return;
        }
      }

      // It is not in cache, read it from the disk.
      last_read = (last_read + 1) % cache_size;
      if (bucket_cache[last_read].changed) saveBucket(bucket_cache[last_read]);
      cache_entry = bucket_cache[last_read];
      cache_entry.adr = bucket_adr;
      if (cache_entry.bucket == null) cache_entry.bucket = new HashBucket(this, 0);
      bucket = cache_entry.bucket;
      cache_entry.elem_loc = -1;
      cache_entry.changed = false;

      // Read the bucket
      readBucket(bucket, bucket_adr);
    }
  }

  private void saveBucket(CacheElement ca_entry) throws IOException
  {
    writeBucket(ca_entry.bucket, ca_entry.adr);
    ca_entry.changed = false;
    ca_entry.hash_val = -1;
    ca_entry.elem_loc = -1;
  }

  /*
   * Split the current bucket. 
   * This includes moving all items in the bucket to a new bucket. 
   * This doesn't require any disk reads because all hash values are stored in the buckets. 
   * Splitting the current bucket may require doubling the size of the hash directory.
   */
  private void splitBucket(int next_insert) throws IOException
  {
    HashBucket bucket_0; // Pointers to the new buckets.
    HashBucket bucket_1;

    int new_bits; // The number of bits for the new buckets.
    int cache_0; // Location in the cache for the buckets.
    int cache_1;
    int adr_0; // File address of the new buckets.
    int adr_1;

    int old_adr[] = new int[31]; // Address of the old directories.
    int old_size[] = new int[31]; // Size of old directories.
    int old_count = 0; // Number of old directories.

    int index, index1; // Used in array indexing.

    while (bucket.count == bucket_elems) {
      // Initialize the "new" buckets in the cache.
      do {
        last_read = (last_read + 1) % cache_size;
        cache_0 = last_read;
      } while (bucket_cache[cache_0].bucket == bucket);
      if (bucket_cache[cache_0].changed) saveBucket(bucket_cache[cache_0]);
      do {
        last_read = (last_read + 1) % cache_size;
        cache_1 = last_read;
      } while (bucket_cache[cache_1].bucket == bucket);
      if (bucket_cache[cache_1].changed) saveBucket(bucket_cache[cache_1]);

      new_bits = bucket.bucket_bits + 1;
      bucket_0 = new HashBucket(this, new_bits);
      bucket_1 = new HashBucket(this, new_bits);
      adr_0 = alloc(bucket_size);
      adr_1 = alloc(bucket_size);
      bucket_cache[cache_0].adr = adr_0;
      bucket_cache[cache_1].adr = adr_1;
      bucket_cache[cache_0].bucket = bucket_0;
      bucket_cache[cache_1].bucket = bucket_1;

      // Double the directory size if necessary.
      if (dir_bits == bucket.bucket_bits) {
        old_adr[old_count] = dir_adr;
        old_size[old_count] = dir_size;
        old_count++;

        dir_size <<= 1;
        dir_adr = alloc(dir_size * 4);
        dir_bits = new_bits;
        bucket_dir *= 2;
        int new_dir[] = new int[dir_size];
        for (int i = 0; i < dir_size / 2; i++) {
          new_dir[i * 2] = dir[i];
          new_dir[i * 2 + 1] = dir[i];
        }
        dir = new_dir;
        header_changed = true;
      }

      // Copy all elements in bucket into the new buckets.
      for (int i = 0; i < bucket_elems; i++) {
        BucketElement old_el = bucket.h_table[i];
        bucket.h_table[i] = new BucketElement();
        int nsel = (old_el.hash_value >> (31 - new_bits)) & 1; // *
        int elem_loc = old_el.hash_value % bucket_elems;
        HashBucket select = (nsel == 0) ? bucket_0 : bucket_1;
        while (select.h_table[elem_loc].hash_value != -1)
          elem_loc = (elem_loc + 1) % bucket_elems;
        select.h_table[elem_loc] = old_el;
        select.count++;
      }

      // Allocate avail space for the bucket_1
      bucket_1.av_adr[0] = alloc(block_size);
      bucket_1.av_size[0] = block_size;
      bucket_1.av_count = 1;

      // Copy the avail elements in current bucket to bucket_0
      index = 0;
      index1 = 0;
      bucket_0.av_count = bucket.av_count;
      if (bucket_0.av_count == BUCKET_AVAIL) {
        // The avail is full, move the first one to bucket_1
        bucket_1.addAvail(bucket.av_adr[0], bucket.av_size[0], false);
        index = 1;
        bucket_0.av_count--;
      }
      for (; index < bucket.av_count; index++, index1++) {
        bucket_0.av_adr[index1] = bucket.av_adr[index];
        bucket_0.av_size[index1] = bucket.av_size[index];
      }

      // Update the directory. We have new file addresses for both buckets
      int dir_start0, dir_start1, dir_end;
      dir_start1 = (bucket_dir >>> (dir_bits - new_bits)) | 1;
      dir_end = (dir_start1 + 1) << (dir_bits - new_bits);
      dir_start1 = dir_start1 << (dir_bits - new_bits);
      dir_start0 = dir_start1 - (dir_end - dir_start1);
      for (index = dir_start0; index < dir_start1; index++)
        dir[index] = adr_0;
      for (index = dir_start1; index < dir_end; index++)
        dir[index] = adr_1;

      // Set changed flags.
      bucket_cache[cache_0].changed = true;
      bucket_cache[cache_1].changed = true;
      bucket_changed = true;
      directory_changed = true;
      second_changed = true;

      // Update the cache.
      bucket_dir = next_insert >> (31 - dir_bits);

      // Invalidate old cache entry
      int old_bucket_adr = cache_entry.adr;
      cache_entry.adr = 0;
      cache_entry.changed = false;

      // set current bucket to the proper bucket.
      if (dir[bucket_dir] == adr_0) {
        bucket = bucket_0;
        cache_entry = bucket_cache[cache_0];
        bucket_1.addAvail(old_bucket_adr, bucket_size, false);
      } else {
        bucket = bucket_1;
        cache_entry = bucket_cache[cache_1];
        bucket_0.addAvail(old_bucket_adr, bucket_size, false);
      }
    }

    // Get rid of old directories.
    for (index = 0; index < old_count; index++)
      free(old_adr[index], old_size[index]);
  }

  /*
   * Allocate space in the file for a block 'size' in length. 
   * Return the file address of the start of the block.
   * 
   * Each hash bucket has a fixed size avail table. We first check this avail table to satisfy the request for space. 
   * In most cases we can and this causes changes to be only in the current hash bucket. 
   * Allocation is done on a first fit basis from the entries. 
   * If a request can not be satisfied from the current hash bucket, then it is satisfied from the file header avail block.
   * If nothing is there that has enough space, another block at the end of the file is allocated
   * and the unused portion is reutrned to the avail block. 
   * This routine "guarantees" that an allocation does not cross a block boundary 
   * unless the size is larger than a single block. The avail structure is changed by this routine if a change is needed.
   */
  private int alloc(int num_bytes) throws IOException
  {
    int new_adr = 0; // the address of the block.
    int new_size = 0; // the size of the block.

    // The current bucket is the first place to look for space.
    for (int i = 0; i < bucket.av_count; i++) {
      if (bucket.av_size[i] >= num_bytes) {
        new_adr = bucket.av_adr[i];
        new_size = bucket.av_size[i];
        bucket.removeAvail(i);
        break;
      }
    }

    // If we did not find some space, we have more work to do.
    if (new_adr == 0) {
      // If the header avail table is less than half full, and there's something on the stack.
      if (av_count <= av_length / 2 - 1 && av_next_block != 0) popAvailBlock();

      // Check the header avail table.
      for (int i = 0; i < av_count; i++) {
        if (av_size[i] >= num_bytes) {
          new_adr = av_adr[i];
          new_size = av_size[i];
          removeAvail(i);
          break;
        }
      }

      if (new_adr == 0) {
        // Get another full block from end of file.
        new_adr = this.next_block;
        new_size = this.block_size;
        while (new_size < num_bytes)
          new_size += this.block_size;
        this.next_block += new_size;
      }

      header_changed = true;
    }

    // Put the unused space back in the avail block.
    free(new_adr + num_bytes, new_size - num_bytes);

    // return the address
    return new_adr;
  }

  /*
   * Free space of size num_bytes in the file at file address file_adr.
   * Make it available for reuse through alloc. 
   * This rountine changes the avail structure.
   */
  private void free(int file_adr, int num_bytes) throws IOException
  {
    // Is it too small to worry about?
    if (num_bytes <= IGNORE_SIZE) return;

    // Try to put into the current bucket.
    if (num_bytes < block_size && bucket.av_count < BUCKET_AVAIL) {
      bucket.addAvail(file_adr, num_bytes, true);
    } else {
      if (av_count == av_length) pushAvailBlock();
      addAvail(file_adr, num_bytes, true);
      header_changed = true;
    }
  }

  private void addAvail(int adr, int size, boolean can_merge)
  {
    // Is it too small to deal with?
    if (size <= IGNORE_SIZE) return;

    if (can_merge) {
      // Search for blocks to coalesce with this one.
      for (int i = 0; i < av_count; i++) {
        // Can we merge with the previous block?
        if (av_adr[i] + av_size[i] == adr) {
          // Simply expand the entry.
          av_size[i] += size;
          return;
        }
        // Can we merge with the next block?
        else if (adr + size == av_adr[i]) {
          // Update this entry
          av_adr[i] = adr;
          av_size[i] += size;
          return;
        }
      }
    }

    // Search for place to put element. List is sorted by size.
    for (int i = 0; i < av_count; i++) {
      if (av_size[i] >= size) {
        System.arraycopy(av_size, i, av_size, i + 1, av_count - i);
        System.arraycopy(av_adr, i, av_adr, i + 1, av_count - i);
        av_count++;
        av_size[i] = size;
        av_adr[i] = adr;
        return;
      }
    }

    // Add the new element at end.
    av_size[av_count] = size;
    av_adr[av_count] = adr;
    av_count++;
  }

  private void removeAvail(int index)
  {
    av_count--;
    if (index < av_count) {
      System.arraycopy(av_size, index + 1, av_size, index, av_count - index);
      System.arraycopy(av_adr, index + 1, av_adr, index, av_count - index);
    }
  }

  /*
   * Gets the avail block at the top of the stack and loads it into the active avail block. 
   * It does a "free" for iteself! 
   * This can be called now even when the avail block is not empty,so we must be smart about things.
   */
  private void popAvailBlock() throws IOException
  {
    int av_block_adr;
    int av_block_size;

    /* new avail block structure */
    int new_av_length;
    int new_av_count;
    int new_av_next_block;
    int new_av_size;
    int new_av_adr;

    // Set up variables.
    av_block_adr = av_next_block;
    av_block_size = 4 // av_length
    + 4 // av_count
    + 4 // av_next_block
    + ((av_length / 2 + 1) * (2 * 4)); // avail table

    // Read the block.
    byte[] buffer = new byte[av_block_size];
    fd.seek(av_block_adr);
    if (fd.read(buffer) != buffer.length) throw new IOException("Read error.");

    // Load avail block data and add the elements from the new block to the header.
    int offset = 0;
    new_av_length = getInt(buffer, offset++);
    new_av_count = getInt(buffer, offset++);
    new_av_next_block = getInt(buffer, offset++);
    for (int i = 0; i < new_av_count; i++) {
      new_av_size = getInt(buffer, offset++);
      new_av_adr = getInt(buffer, offset++);
      addAvail(new_av_adr, new_av_size, true);
    }

    // Fix next block, as well
    av_next_block = new_av_next_block;
    header_changed = true;

    // Free the previous avail block.
    addAvail(av_block_adr, av_block_size, true);
  }

  /* Splits the header avail block and pushes half onto the avail stack. */
  private void pushAvailBlock() throws IOException
  {
    int av_block_size;
    int new_adr;
    int new_size;
    int new_av_count;
    int new_av_size[];
    int new_av_adr[];

    // Calculate the size of the split block.
    av_block_size = 4 // av_length
    + 4 // av_count
    + 4 // av_next_block
    + ((av_length / 2 + 1) * (2 * 4)); // avail table

    // Get address in file for new av_block_size bytes.
    new_adr = 0;
    new_size = 0;
    for (int i = 0; i < av_count; i++) {
      if (av_size[i] >= av_block_size) {
        new_adr = av_adr[i];
        new_size = av_size[i];
        removeAvail(i);
        break;
      }
    }
    if (new_adr == 0) {
      new_adr = next_block;
      new_size = block_size;
      while (new_size < av_block_size)
        new_size += block_size;
      next_block += new_size;
    }

    // Split the header block.
    new_av_count = 0;
    new_av_size = new int[av_length];
    new_av_adr = new int[av_length];
    for (int index = 1; index < av_count; index++) {
      if ((index & 0x1) == 1) { // Index is odd.
        new_av_size[new_av_count] = av_size[index];
        new_av_adr[new_av_count] = av_adr[index];
        new_av_count++;
      } else {
        av_size[index >> 1] = av_size[index];
        av_adr[index >> 1] = av_adr[index];
      }
    }

    // Update the header avail count to previous size divided by 2.
    av_count >>= 1;

    // Free the unneeded space
    free(new_adr + av_block_size, new_size - av_block_size);

    // Update the disk.
    byte[] buffer = new byte[av_block_size];
    int offset = 0;
    putInt(buffer, offset++, av_length);
    putInt(buffer, offset++, new_av_count);
    putInt(buffer, offset++, av_next_block);
    for (int i = 0; i < new_av_count; i++) {
      putInt(buffer, offset++, new_av_size[i]);
      putInt(buffer, offset++, new_av_adr[i]);
    }

    fd.seek(new_adr);
    fd.write(buffer, 0, av_block_size);
    av_next_block = new_adr;
  }

  private void writeHeader() throws IOException
  {
    int offset = 0;
    putInt(buffer, offset++, header_magic);
    putInt(buffer, offset++, block_size);
    putInt(buffer, offset++, dir_adr);
    putInt(buffer, offset++, dir_size * 4);
    putInt(buffer, offset++, dir_bits);
    putInt(buffer, offset++, bucket_size);
    putInt(buffer, offset++, bucket_elems);
    putInt(buffer, offset++, next_block);
    putInt(buffer, offset++, av_length);
    putInt(buffer, offset++, av_count);
    putInt(buffer, offset++, av_next_block);
    for (int i = 0; i < av_length; i++) {
      putInt(buffer, offset++, av_size[i]);
      putInt(buffer, offset++, av_adr[i]);
    }

    fd.seek(0);
    fd.write(buffer);
  }

  private void readHeader() throws IOException
  {
    fd.seek(0);
    if (fd.read(buffer) != buffer.length) throw new IOException("Unable to read dbm header.");

    int offset = 0;
    header_magic = getInt(buffer, offset++);
    block_size = getInt(buffer, offset++);
    dir_adr = getInt(buffer, offset++);
    dir_size = getInt(buffer, offset++) / 4;
    dir_bits = getInt(buffer, offset++);
    bucket_size = getInt(buffer, offset++);
    bucket_elems = getInt(buffer, offset++);
    next_block = getInt(buffer, offset++);
    av_length = getInt(buffer, offset++);
    av_count = getInt(buffer, offset++);
    av_next_block = getInt(buffer, offset++);
    av_size = new int[av_length];
    av_adr = new int[av_length];
    for (int i = 0; i < av_length; i++) {
      av_size[i] = getInt(buffer, offset++);
      av_adr[i] = getInt(buffer, offset++);
    }
  }

  private void writeDirectory() throws IOException
  {
    byte[] buffer = new byte[dir_size * 4];
    int offset = 0;
    for (int i = 0; i < dir_size; i++) {
      putInt(buffer, offset++, dir[i]);
    }

    fd.seek(dir_adr);
    fd.write(buffer);
  }

  private void readDirectory() throws IOException
  {
    byte[] buffer = new byte[dir_size * 4];
    fd.seek(dir_adr);
    if (fd.read(buffer) != buffer.length) throw new IOException("Unable to read dbm directory.");

    int offset = 0;
    for (int i = 0; i < dir_size; i++) {
      dir[i] = getInt(buffer, offset++);
    }
  }

  private void writeBucket(HashBucket bucket, int file_adr) throws IOException
  {
    int offset = 0;
    putInt(buffer, offset++, bucket.av_count);
    for (int i = 0; i < BUCKET_AVAIL; i++) {
      putInt(buffer, offset++, bucket.av_size[i]);
      putInt(buffer, offset++, bucket.av_adr[i]);
    }
    putInt(buffer, offset++, bucket.bucket_bits);
    putInt(buffer, offset++, bucket.count);
    for (int i = 0; i < bucket.h_table.length; i++) {
      putInt(buffer, offset++, bucket.h_table[i].hash_value);
      System.arraycopy(bucket.h_table[i].key_start, 0, buffer, offset * 4, SMALL);
      offset += SMALL / 4;
      putInt(buffer, offset++, bucket.h_table[i].data_pointer);
      putInt(buffer, offset++, bucket.h_table[i].key_size);
      putInt(buffer, offset++, bucket.h_table[i].data_size);
      putLong(buffer, offset, bucket.h_table[i].lastmod);
      offset += 2;
    }

    fd.seek(file_adr);
    fd.write(buffer);
  }

  private void readBucket(HashBucket bucket, int file_adr) throws IOException
  {
    fd.seek(file_adr);
    if (fd.read(buffer) != buffer.length) throw new IOException("Unable to read dbm bucket.");

    int offset = 0;
    bucket.av_count = getInt(buffer, offset++);
    for (int i = 0; i < BUCKET_AVAIL; i++) {
      bucket.av_size[i] = getInt(buffer, offset++);
      bucket.av_adr[i] = getInt(buffer, offset++);
    }
    bucket.bucket_bits = getInt(buffer, offset++);
    bucket.count = getInt(buffer, offset++);
    for (int i = 0; i < bucket.h_table.length; i++) {
      bucket.h_table[i].hash_value = getInt(buffer, offset++);
      System.arraycopy(buffer, offset * 4, bucket.h_table[i].key_start, 0, SMALL);
      offset += SMALL / 4;
      bucket.h_table[i].data_pointer = getInt(buffer, offset++);
      bucket.h_table[i].key_size = getInt(buffer, offset++);
      bucket.h_table[i].data_size = getInt(buffer, offset++);
      bucket.h_table[i].lastmod = getLong(buffer, offset);
      offset += 2;
    }
  }

  /*
   * After all changes have been made in memory, we now write them all to disk.
   */
  private void endUpdate() throws IOException
  {
    // Write the current bucket.
    if (bucket_changed && cache_entry != null) {
      saveBucket(cache_entry);
      bucket_changed = false;
    }

    // Write the other changed buckets if there are any.
    if (second_changed) {
      for (int i = 0; i < cache_size; i++) {
        if (bucket_cache[i].changed) saveBucket(bucket_cache[i]);
      }
      second_changed = false;
    }

    // Write the directory
    if (directory_changed) {
      writeDirectory();
      directory_changed = false;
    }

    // Write the header.
    if (header_changed) {
      writeHeader();
      header_changed = false;
    }

    // Wait for all output to be done.
    if (!fast_write) {
      fd.getFD().sync();
    }
  }

  private static final void putInt(byte buffer[], int offset, int value)
  {
    buffer[offset * 4 + 0] = (byte) (value >>> 0);
    buffer[offset * 4 + 1] = (byte) (value >>> 8);
    buffer[offset * 4 + 2] = (byte) (value >>> 16);
    buffer[offset * 4 + 3] = (byte) (value >>> 24);
  }

  private static final int getInt(byte buffer[], int offset)
  {
    return ((buffer[offset * 4 + 0] & 0xff) << 0) + ((buffer[offset * 4 + 1] & 0xff) << 8) + ((buffer[offset * 4 + 2] & 0xff) << 16) + ((buffer[offset * 4 + 3] & 0xff) << 24);
  }

  private static final void putLong(byte buffer[], int offset, long value)
  {
    putInt(buffer, offset, (int) value);
    putInt(buffer, offset + 1, (int) (value >>> 32));
  }

  private static final long getLong(byte buffer[], int offset)
  {
    long low = (long) getInt(buffer, offset) & 0xffffffffL;
    long high = (long) getInt(buffer, offset + 1) & 0xffffffffL;
    return (high << 32) + low;
  }

  private static final boolean arrayEquals(byte[] a, byte[] b, int len)
  {
    for (int i = 0; i < len; i++) {
      if (a[i] != b[i]) return false;
    }
    return true;
  }
}
