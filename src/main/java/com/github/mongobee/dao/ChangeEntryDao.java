package com.github.mongobee.dao;

import static com.github.mongobee.changeset.ChangeEntry.CHANGELOG_COLLECTION;
import static org.springframework.util.StringUtils.hasText;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.github.mongobee.exception.MongobeeConnectionException;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeEntryDao {
  private static final Logger logger = LoggerFactory.getLogger("Mongobee dao");

  private MongoDatabase mongoDatabase;
  private DB db;  // only for Jongo driver compatibility - do not use in other contexts
  private MongoClient mongoClient;
  private ChangeEntryIndexDao indexDao = new ChangeEntryIndexDao();

  private LockDao lockDao = new LockDao();

  public MongoDatabase getMongoDatabase() {
    return mongoDatabase;
  }

  /**
   * @deprecated implemented only for Jongo driver compatibility and backward compatibility - do not use in other contexts
   * @return com.mongodb.DB
   */
  public DB getDb() {
    return db;
  }

  public MongoDatabase connectMongoDb(MongoClient mongo, String dbName) throws MongobeeConfigurationException {
    if (!hasText(dbName)) {
      throw new MongobeeConfigurationException("DB name is not set. Should be defined in MongoDB URI or via setter");
    } else {

      this.mongoClient = mongo;

      db = mongo.getDB(dbName); // for Jongo driver and backward compatibility (constructor has required parameter Jongo(DB) )
      mongoDatabase = mongo.getDatabase(dbName);

      ensureChangeLogCollectionIndex(mongoDatabase.getCollection(CHANGELOG_COLLECTION));
      initializeLock();
      return mongoDatabase;
    }
  }

  public MongoDatabase connectMongoDb(MongoClientURI mongoClientURI, String dbName)
      throws MongobeeConfigurationException, MongobeeConnectionException {

    final MongoClient mongoClient = new MongoClient(mongoClientURI);
    final String database = (!hasText(dbName)) ? mongoClientURI.getDatabase() : dbName;
    return this.connectMongoDb(mongoClient, database);
  }

  /**
   * Try to acquire process lock
   *
   * @return true if successfully acquired, false otherwise
   * @throws MongobeeConnectionException exception
   */
  public boolean acquireProcessLock() throws MongobeeConnectionException {
    verifyDbConnection();
    return lockDao.acquireLock(getMongoDatabase());
  }

  public void releaseProcessLock() throws MongobeeConnectionException {
    verifyDbConnection();
    lockDao.releaseLock(getMongoDatabase());
  }

  public boolean isProccessLockHeld() throws MongobeeConnectionException {
    verifyDbConnection();
    return lockDao.isLockHeld(getMongoDatabase());
  }

  public boolean isNewChange(ChangeEntry changeEntry) throws MongobeeConnectionException {
    verifyDbConnection();

    MongoCollection<Document> mongobeeChangeLog = getMongoDatabase().getCollection(CHANGELOG_COLLECTION);
    Document entry = mongobeeChangeLog.find(changeEntry.buildSearchQueryDBObject()).first();

    return entry == null;
  }

  public void save(ChangeEntry changeEntry) throws MongobeeConnectionException {
    verifyDbConnection();

    MongoCollection<Document> mongobeeLog = getMongoDatabase().getCollection(CHANGELOG_COLLECTION);

    mongobeeLog.insertOne(changeEntry.buildFullDBObject());
  }

  private void verifyDbConnection() throws MongobeeConnectionException {
    if (getMongoDatabase() == null) {
      throw new MongobeeConnectionException("Database is not connected. Mongobee has thrown an unexpected error",
          new NullPointerException());
    }
  }

  private void ensureChangeLogCollectionIndex(MongoCollection<Document> collection) {
    Document index = indexDao.findRequiredChangeAndAuthorIndex(mongoDatabase);
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
      this.mongoClient.close();
  }

  private void initializeLock() {
    lockDao.intitializeLock(mongoDatabase);
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
