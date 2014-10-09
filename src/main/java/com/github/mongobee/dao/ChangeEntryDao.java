package com.github.mongobee.dao;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.github.mongobee.exception.MongobeeConnectionException;
import com.mongodb.*;
import org.springframework.util.*;

import java.net.UnknownHostException;

import static com.github.mongobee.changeset.ChangeEntry.CHANGELOG_COLLECTION;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeEntryDao {

  private DB db;
  
  public DB connectMongoDb(MongoClientURI mongoClientURI, String dbName) throws UnknownHostException {
    MongoClient mongoClient = new MongoClient(mongoClientURI);
    String database = (!hasText(dbName)) ? mongoClientURI.getDatabase() : dbName;

    if (!hasText(database)){
      throw new MongobeeConfigurationException("DB name is not set. Should be defined in MongoDB URI or via setter");
    } else {
      db = mongoClient.getDB(database);
      return db;
    }
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
      throw new MongobeeConnectionException("Database is not connected. Mongobee thrown unexpected error",
        new NullPointerException());
    }
  }

  public DB getDb() {
    return db;
  }
}
