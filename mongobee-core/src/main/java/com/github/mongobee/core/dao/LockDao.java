package com.github.mongobee.core.dao;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;

/**
 * @author colsson11
 * @since 13.01.15
 */
public class LockDao {
  private static final Logger logger = LoggerFactory.getLogger(LockDao.class);
  private static final String KEY_PROP_NAME = "key";

  private static final int INDEX_SORT_ASC = 1;

  private static final String LOCK_ENTRY_KEY_VAL = "LOCK";
  private String lockCollectionName;
  
  public LockDao(String lockCollectionName) {
	this.lockCollectionName = lockCollectionName;
  }

  public void intitializeLock(MongoDatabase db) {
    createCollectionAndUniqueIndexIfNotExists(db);
  }

  private void createCollectionAndUniqueIndexIfNotExists(MongoDatabase db) {
    Document indexKeys = new Document(KEY_PROP_NAME, INDEX_SORT_ASC);
    IndexOptions indexOptions = new IndexOptions().unique(true).name("mongobeelock_key_idx");

    db.getCollection(lockCollectionName).createIndex(indexKeys, indexOptions);
  }

  public boolean acquireLock(MongoDatabase db) {

    Document insertObj = new Document(KEY_PROP_NAME, LOCK_ENTRY_KEY_VAL).append("status", "LOCK_HELD");

    // acquire lock by attempting to insert the same value in the collection - if it already exists (i.e. lock held)
    // there will be an exception
    try {
      db.getCollection(lockCollectionName).insertOne(insertObj);
    } catch (MongoWriteException ex) {
      if (ex.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
        logger.warn("Duplicate key exception while acquireLock. Probably the lock has been already acquired.");
      }
      return false;
    }
    return true;
  }

  public void releaseLock(MongoDatabase db) {
    // release lock by deleting collection entry
    db.getCollection(lockCollectionName).deleteMany(new Document(KEY_PROP_NAME, LOCK_ENTRY_KEY_VAL));
  }

  /**
   * Check if the lock is held. Could be used by external process for example.
   *
   * @param db MongoDatabase object
   * @return true if the lock is currently held
   */
  public boolean isLockHeld(MongoDatabase db) {
    return db.getCollection(lockCollectionName).count() == 1;
  }

  public void setLockCollectionName(String lockCollectionName) {
	this.lockCollectionName = lockCollectionName;
  }

}
