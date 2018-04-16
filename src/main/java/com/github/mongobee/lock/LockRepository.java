package com.github.mongobee.lock;

import com.mongodb.DuplicateKeyException;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Date;

import static com.github.mongobee.lock.LockEntry.EXPIRES_AT_FIELD;
import static com.github.mongobee.lock.LockEntry.KEY_FIELD;
import static com.github.mongobee.lock.LockEntry.OWNER_FIELD;
import static com.github.mongobee.lock.LockEntry.STATUS_FIELD;
import static com.github.mongobee.lock.LockStatus.LOCK_HELD;

/**
 * <p>Repository class to manage lock in database</p>
 *
 * @author dieppa
 * @since 04/04/2018
 */
public class LockRepository {

  private String lockCollectionName;
  private MongoDatabase db;

  public LockRepository(String lockCollectionName) {
    this.lockCollectionName = lockCollectionName;
  }

  /**
   * Initializes connection and ensure indexes and keys.
   *
   * @param db MongoDatabase
   * @see MongoDatabase
   */
  void intitialize(MongoDatabase db) {
    this.db = db;
    createCollectionAndUniqueIndexIfNotExists();
  }

  private void createCollectionAndUniqueIndexIfNotExists() {
    final Document indexKeys = new Document(KEY_FIELD, 1);
    final IndexOptions indexOptions = new IndexOptions().unique(true).name("mongobeelock_key_idx");
    db.getCollection(lockCollectionName).createIndex(indexKeys, indexOptions);
  }

  /**
   * If there is a lock in the database with the same key, updates it if either is expired or both share the same owner.
   * If there is no lock with the same key, it's inserted.
   *
   * @param newLock lock to replace the existing one or be inserted.
   * @throws LockPersistenceException if there is a lock in database with same key, but is expired and belong to
   *                                  another owner or cannot insert/update the lock for any other reason
   */
  public void insertUpdate(LockEntry newLock) throws LockPersistenceException {
    insertUpdate(newLock, false);
  }

  /**
   * If there is a lock in the database with the same key and owner, updates it.Otherwise throws a LockPersistenceException
   *
   * @param newLock lock to replace the existing one.
   * @throws LockPersistenceException if there is no lock in the database with the same key and owner or cannot update
   *                                  the lock for any other reason
   */
  public void updateIfSameOwner(LockEntry newLock) throws LockPersistenceException {
    insertUpdate(newLock, true);
  }

  /**
   * Retrieves a lock by key
   *
   * @param lockKey key
   * @return LockEntry
   */
  LockEntry findByKey(String lockKey) {
    Document result = db.getCollection(lockCollectionName).find(new Document().append(KEY_FIELD, lockKey)).first();
    if (result != null) {
      return new LockEntry(
          result.getString(KEY_FIELD),
          result.getString(STATUS_FIELD),
          result.getString(OWNER_FIELD),
          result.getDate(EXPIRES_AT_FIELD)
      );
    }
    return null;
  }

  /**
   * Removes from database all the locks with the same key(only can be one) and owner
   *
   * @param lockKey
   * @param owner
   */
  void removeByKeyAndOwner(String lockKey, String owner) {
    db.getCollection(lockCollectionName)
        .deleteMany(Filters.and(Filters.eq(KEY_FIELD, lockKey), Filters.eq(OWNER_FIELD, owner)));
  }

  /**
   * Updates the collection name
   *
   * @param lockCollectionName String
   */
  public void setLockCollectionName(String lockCollectionName) {
    this.lockCollectionName = lockCollectionName;
  }

  private void insertUpdate(LockEntry newLock, boolean onlyIfSameOwner) throws LockPersistenceException {
    boolean lockHeld;
    try {

      final Bson acquireLockQuery =
          getAcquireLockQuery(newLock.getKey(), newLock.getOwner(), onlyIfSameOwner);

      final UpdateResult result = db.getCollection(lockCollectionName).updateMany(
          acquireLockQuery,
          new Document().append("$set", newLock.buildFullDBObject()),
          new UpdateOptions().upsert(!onlyIfSameOwner));

      lockHeld = result.getModifiedCount() <= 0 && result.getUpsertedId() == null;

    } catch (MongoWriteException ex) {
      if (!(lockHeld = ex.getError().getCategory() == ErrorCategory.DUPLICATE_KEY)) {
        throw ex;
      }

    } catch (DuplicateKeyException ex) {
      lockHeld = true;
    }

    if (lockHeld) {
      throw new LockPersistenceException("Lock is held");
    }
  }

  private Bson getAcquireLockQuery(String lockKey, String owner, boolean onlyIfSameOwner) {
    final Bson expiresAtCond = Filters.lt(EXPIRES_AT_FIELD, new Date());
    final Bson ownerCond = Filters.eq(OWNER_FIELD, owner);
    final Bson orCond = onlyIfSameOwner ? Filters.or(ownerCond) : Filters.or(expiresAtCond, ownerCond);
    return Filters.and(Filters.eq(KEY_FIELD, lockKey), Filters.eq(STATUS_FIELD, LOCK_HELD.name()), orCond);
  }
}
