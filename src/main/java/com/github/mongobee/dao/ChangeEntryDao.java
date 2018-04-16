package com.github.mongobee.dao;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.github.mongobee.exception.MongobeeConnectionException;
import com.mongodb.DB;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeEntryDao {
  private static final Logger logger = LoggerFactory.getLogger("Mongobee dao");

  private MongoDatabase mongoDatabase;
  private DB db;  // only for Jongo driver compatibility - do not use in other contexts
  private ChangeEntryIndexDao indexDao;
  private String changelogCollectionName;

  public ChangeEntryDao(String changelogCollectionName, String lockCollectionName) {
    this.indexDao = new ChangeEntryIndexDao(changelogCollectionName);
    this.changelogCollectionName = changelogCollectionName;
  }

  public MongoDatabase getMongoDatabase() {
    return mongoDatabase;
  }

  /**
   * @return com.mongodb.DB
   * @deprecated implemented only for Jongo driver compatibility and backward compatibility - do not use in other contexts
   */
  public DB getDb() {
    return db;
  }

  public void connectMongoDb(MongoDatabase mongoDatabase, DB db) throws MongobeeConfigurationException {
    this.db = db; // for Jongo driver and backward compatibility (constructor has required parameter Jongo(DB) )
    this.mongoDatabase = mongoDatabase;

    ensureChangeLogCollectionIndex(mongoDatabase.getCollection(changelogCollectionName));
  }

  public boolean isNewChange(ChangeEntry changeEntry) throws MongobeeConnectionException {
    verifyDbConnection();

    MongoCollection<Document> mongobeeChangeLog = getMongoDatabase().getCollection(changelogCollectionName);
    Document entry = mongobeeChangeLog.find(changeEntry.buildSearchQueryDBObject()).first();

    return entry == null;
  }

  public void save(ChangeEntry changeEntry) throws MongobeeConnectionException {
    verifyDbConnection();

    MongoCollection<Document> mongobeeLog = getMongoDatabase().getCollection(changelogCollectionName);

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
      logger.debug("Index in collection " + changelogCollectionName + " was created");
    } else if (!indexDao.isUnique(index)) {
      indexDao.dropIndex(collection, index);
      indexDao.createRequiredUniqueIndex(collection);
      logger.debug("Index in collection " + changelogCollectionName + " was recreated");
    }

  }

  public void setIndexDao(ChangeEntryIndexDao changeEntryIndexDao) {
    this.indexDao = changeEntryIndexDao;
  }

  public void setChangelogCollectionName(String changelogCollectionName) {
    this.indexDao.setChangelogCollectionName(changelogCollectionName);
    this.changelogCollectionName = changelogCollectionName;
  }
}
