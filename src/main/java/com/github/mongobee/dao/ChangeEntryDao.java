package com.github.mongobee.dao;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.github.mongobee.exception.MongobeeConnectionException;
import com.mongodb.*;
import org.omg.PortableInterceptor.LOCATION_FORWARD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.*;

import java.net.UnknownHostException;
import java.util.List;

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

  public DB connectMongoDb(Mongo mongo, String dbName) throws UnknownHostException {
    if (!hasText(dbName)){
      throw new MongobeeConfigurationException("DB name is not set. Should be defined in MongoDB URI or via setter");
    } else {
      db = mongo.getDB(dbName);
       ensureChangelogCollectionIndexes(db.getCollection(CHANGELOG_COLLECTION)); // TODO Issue#14
      return db;
    }
  }

  public DB connectMongoDb(MongoClientURI mongoClientURI, String dbName) throws UnknownHostException {
    Mongo mongoClient = new MongoClient(mongoClientURI);
    String database = (!hasText(dbName)) ? mongoClientURI.getDatabase() : dbName;
    return this.connectMongoDb(mongoClient, database);
  }

  public boolean isNewChange(ChangeEntry changeEntry) {
    verifyDbConnection();

    DBCollection mongobeeChangeLog = getDb().getCollection(CHANGELOG_COLLECTION);
    DBObject entry = mongobeeChangeLog.findOne(changeEntry.buildSearchQueryDBObject());

    return entry == null ? true : false;
  }

  public WriteResult save(ChangeEntry changeEntry) {
    verifyDbConnection();

    DBCollection mongobeeLog = getDb().getCollection(CHANGELOG_COLLECTION);
    return mongobeeLog.save(changeEntry.buildFullDBObject());
  }

  private void verifyDbConnection(){
    if (getDb() == null) {
      throw new MongobeeConnectionException("Database is not connected. Mongobee has thrown an unexpected error",
        new NullPointerException());
    }
  }

  // TODO Issue#14: add changeId_1_author_1 + unique: true
  private void ensureChangelogCollectionIndexes(DBCollection collection) {

    DBObject index = findRequiredIndex(collection);
    if (index != null && !isUnique(index)){
      // todo remove index,


      // todo create new one
      //collection.createIndex(new BasicDBObject().append("changeId", 1).append("author", 1), new BasicDBObject("unique", true));

    } else if (index == null) {
      // todo create index
      // collection.createIndex(new BasicDBObject().append("changeId", 1).append("author", 1), new BasicDBObject("unique", true));

      // indexes created
      logger.debug("Indexes in collection " + CHANGELOG_COLLECTION + " was created");
    }


  }

  private boolean isUnique(DBObject index) {
    if (index.get("unique") != null && index.get("unique") instanceof Boolean){
      return (Boolean) index.get("unique");
    } else {
      return false;
    }
  }

  private DBObject findRequiredIndex(DBCollection collection) {
    List<DBObject> indices = collection.getIndexInfo();
    for (com.mongodb.DBObject index : indices){
      Object key = index.get("key");
      if (key.equals(new BasicDBObject().append("changeId",1).append("author",1))){
        // todo here it is a problem: changeId_1_author_1 equals to author_1_changeId_1 !!!
        // solution: return collection and remove all not unique
        // solution2: check here if unique if not remove   << better?
        return index;
      }
    }
    return null;  // not found
  }

  // todo refactor index checking and creating to separate util class


}
