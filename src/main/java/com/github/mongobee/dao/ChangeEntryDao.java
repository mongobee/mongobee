package com.github.mongobee.dao;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.github.mongobee.exception.MongobeeConnectionException;
import com.mongodb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

import static com.github.mongobee.changeset.ChangeEntry.CHANGELOG_COLLECTION;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeEntryDao {
  private static final Logger logger = LoggerFactory.getLogger("Mongobee dao");

  private DB db;

  public DB getDb() {
    return db;
  }

  public DB connectMongoDb(Mongo mongo, String dbName) throws MongobeeConfigurationException {
    if (!hasText(dbName)) {
      throw new MongobeeConfigurationException("DB name is not set. Should be defined in MongoDB URI or via setter");
    } else {
      db = mongo.getDB(dbName);
      ensureChangeLogCollectionIndex(db.getCollection(CHANGELOG_COLLECTION)); // TODO Issue#14
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

  public boolean isNewChange(ChangeEntry changeEntry) throws MongobeeConnectionException {
    verifyDbConnection();

    DBCollection mongobeeChangeLog = getDb().getCollection(CHANGELOG_COLLECTION);
    DBObject entry = mongobeeChangeLog.findOne(changeEntry.buildSearchQueryDBObject());

    return entry == null ? true : false;
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
    DBObject index = findRequiredChangeAndAuthorIndex();
    if (index == null) {
      createRequiredUniqueIndex(collection);
      logger.debug("Index in collection " + CHANGELOG_COLLECTION + " was created");
    } else if (!isUnique(index)) {
      collection.dropIndex(index.get("name").toString());
      createRequiredUniqueIndex(collection);
      logger.debug("Index in collection " + CHANGELOG_COLLECTION + " was recreated");
    }

  }

  private void createRequiredUniqueIndex(DBCollection collection) {
    collection.createIndex(new BasicDBObject()
          .append(ChangeEntry.KEY_CHANGEID, 1)
          .append(ChangeEntry.KEY_AUTHOR, 1),
        new BasicDBObject().append("unique", true));
  }

  private DBObject findRequiredChangeAndAuthorIndex() {
    DBCollection indexes = db.getCollection("system.indexes");
    DBObject index = indexes.findOne(new BasicDBObject()
            .append("ns", db.getName() + "." + CHANGELOG_COLLECTION)
            .append("key", new BasicDBObject().append(ChangeEntry.KEY_CHANGEID, 1).append(ChangeEntry.KEY_AUTHOR, 1))
    );

    return index;
  }

  private boolean isUnique(DBObject index) {
    Object unique = index.get("unique");
    if (unique != null && unique instanceof Boolean) {
      return (Boolean) unique;
    } else {
      return false;
    }
  }

}
