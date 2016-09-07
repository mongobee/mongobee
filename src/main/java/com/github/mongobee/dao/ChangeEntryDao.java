package com.github.mongobee.dao;

import static com.github.mongobee.changeset.ChangeEntry.CHANGELOG_COLLECTION;
import static org.springframework.util.StringUtils.hasText;

import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.github.mongobee.exception.MongobeeConnectionException;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteResult;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeEntryDao {
  private static final Logger logger = LoggerFactory.getLogger("Mongobee dao");

  private DB db;
  private Mongo mongo;
  private ChangeEntryIndexDao indexDao = new ChangeEntryIndexDao();

  private LockDao lockDao = new LockDao();

  public DB getDb() {
    return db;
  }

  public DB connectMongoDb(Mongo mongo, String dbName) throws MongobeeConfigurationException {
    if (!hasText(dbName)) {
      throw new MongobeeConfigurationException("DB name is not set. Should be defined in MongoDB URI or via setter");
    } else {
      this.mongo = mongo;
      db = mongo.getDB(dbName);
      ensureChangeLogCollectionIndex(db.getCollection(CHANGELOG_COLLECTION));
      initializeLock();
      return db;
    }
  }

  public DB connectMongoDb(MongoClientURI mongoClientURI, String dbName)
      throws MongobeeConfigurationException, MongobeeConnectionException {
    try {
      final Mongo mongoClient = new MongoClient(mongoClientURI);
      final String database = (!hasText(dbName)) ? mongoClientURI.getDatabase() : dbName;
      return this.connectMongoDb(mongoClient, database);
    } catch (UnknownHostException e) {
      throw new MongobeeConnectionException(e.getMessage(), e);
    }

  }

  /**
   * Try to acquire process lock
   *
   * @return true if successfully acquired, false otherwise
   */
  public boolean acquireProcessLock() throws MongobeeConnectionException {
    verifyDbConnection();
    return lockDao.acquireLock(getDb());
  }

  public void releaseProcessLock() throws MongobeeConnectionException {
    verifyDbConnection();
    lockDao.releaseLock(getDb());
  }

  public boolean isProccessLockHeld() throws MongobeeConnectionException {
    verifyDbConnection();
    return lockDao.isLockHeld(getDb());
  }

  public boolean isNewChange(ChangeEntry changeEntry) throws MongobeeConnectionException {
    verifyDbConnection();

    DBCollection mongobeeChangeLog = getDb().getCollection(CHANGELOG_COLLECTION);
    DBObject entry = mongobeeChangeLog.findOne(changeEntry.buildSearchQueryDBObject());

    return entry == null;
  }

  public WriteResult save(ChangeEntry changeEntry) throws MongobeeConnectionException {
    verifyDbConnection();

    DBCollection mongobeeLog = getDb().getCollection(CHANGELOG_COLLECTION);
    return mongobeeLog.save(changeEntry.buildFullDBObject());
  }

  private void verifyDbConnection() throws MongobeeConnectionException {
    if (getDb() == null) {
      throw new MongobeeConnectionException("Database is not connected. Mongobee has thrown an unexpected error",
          new NullPointerException());
    }
  }

  private void ensureChangeLogCollectionIndex(DBCollection collection) {
    DBObject index = indexDao.findRequiredChangeAndAuthorIndex(db);
    if (index == null) {
      indexDao.createRequiredUniqueIndex(collection);
      logger.debug("Index in collection " + CHANGELOG_COLLECTION + " was created");
    } else if (!indexDao.isUnique(index)) {
      indexDao.dropIndex(collection, index);
      indexDao.createRequiredUniqueIndex(collection);
      logger.debug("Index in collection " + CHANGELOG_COLLECTION + " was recreated");
    }

  }

  public void close() {
      this.mongo.close();
  }

  private void initializeLock() {
    lockDao.intitializeLock(db);
  }

  /* Visible for testing */
  void setIndexDao(ChangeEntryIndexDao changeEntryIndexDao) {
    this.indexDao = changeEntryIndexDao;
  }

  /* Visible for testing */
  void setLockDao(LockDao lockDao) {
    this.lockDao = lockDao;
  }

}
