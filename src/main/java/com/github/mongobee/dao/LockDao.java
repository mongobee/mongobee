package com.github.mongobee.dao;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;

/**
 * @author colsson11
 * @since 13.01.15
 */
public class LockDao {

  private static final String COLLECTION = "mongobeelock";
  private static final String KEY_PROP_NAME = "key";

  private static final int INDEX_SORT_ASC = 1;

  private static final String LOCK_ENTRY_KEY_VAL = "LOCK";

  public void intitializeLock(DB db) {
    createCollectionAndUniqueIndexIfNotExists(db);
  }

  private void createCollectionAndUniqueIndexIfNotExists(DB db) {
    DBObject indexKeys = new BasicDBObject(KEY_PROP_NAME, INDEX_SORT_ASC);
    DBObject indexOptions = new BasicDBObject("unique", true).append("name", "mongobeelock_key_idx");

    db.getCollection(COLLECTION).createIndex(indexKeys, indexOptions);
  }

  public boolean acquireLock(DB db) {

    DBObject insertObj = new BasicDBObject(KEY_PROP_NAME, LOCK_ENTRY_KEY_VAL).append("status", "LOCK_HELD");

    // acquire lock by attempting to insert the same value in the collection - if it already exists (i.e. lock held)
    // there will be an exception
    try {
      db.getCollection(COLLECTION).insert(insertObj);
    } catch (DuplicateKeyException ex) {
      return false;
    }
    return true;
  }

  public void releaseLock(DB db) {
    // release lock by deleting collection entry
    db.getCollection(COLLECTION).remove(new BasicDBObject(KEY_PROP_NAME, LOCK_ENTRY_KEY_VAL));
  }

  /**
   * Check if the lock is held. Could be used by external process for example.
   *
   * @param db
   * @return true if the lock is currently held
   */
  public boolean isLockHeld(DB db) {
    return db.getCollection(COLLECTION).count() == 1;
  }

}
