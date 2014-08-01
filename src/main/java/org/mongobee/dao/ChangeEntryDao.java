package org.mongobee.dao;

import com.mongodb.*;
import org.mongobee.changeset.ChangeEntry;
import org.mongobee.exception.MongobeeConfigurationException;
import org.mongobee.exception.MongobeeConnectionException;

import java.net.UnknownHostException;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mongobee.changeset.ChangeEntry.CHANGELOG_COLLECTION;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeEntryDao {

  private DB db;
  
  public DB connectMongoDb(MongoClientURI mongoClientURI, String dbName) throws UnknownHostException {
    MongoClient mongoClient = new MongoClient(mongoClientURI);
    String database = (isBlank(dbName)) ? mongoClientURI.getDatabase() : dbName;

    if (isBlank(database)){
      throw new MongobeeConfigurationException("DB name is not set. Should be defined in MongoDB URI or via setter");
    } else {
      db = mongoClient.getDB(database);
      return db;
    }
  }

  public boolean isNewChange(ChangeEntry changeEntry) {
    verifyDbConnection();

    DBCollection mongobeeChangelog = db.getCollection(CHANGELOG_COLLECTION);
    DBObject entry = mongobeeChangelog.findOne(changeEntry.buildSearchQueryDBObject());

    return entry == null ? true : false;
  }

  public WriteResult save(ChangeEntry changeEntry) {
    verifyDbConnection();

    DBCollection monjeezlog = db.getCollection(CHANGELOG_COLLECTION);
    return monjeezlog.save(changeEntry.buildFullDBObject());
  }

  private void verifyDbConnection(){
    if (db == null) {
      throw new MongobeeConnectionException("Database is not connected. Mongobee thrown unexpected error",
        new NullPointerException());
    }
  }

}
