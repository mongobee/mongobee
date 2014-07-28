package org.monjeez.dao;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import org.monjeez.changeset.ChangeEntry;
import org.monjeez.exception.MonjeezConnectionException;

import static org.monjeez.changeset.ChangeEntry.MONJEEZ_CHANGELOG_COLLECTION;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeEntryDao {

  private DB db;
  
  public ChangeEntryDao(DB db) {
    this.db = db;
  }
  
  public boolean checkConnection(){
    try {
      return db.collectionExists(MONJEEZ_CHANGELOG_COLLECTION);
  
    } catch (Exception e){
      throw new MonjeezConnectionException("Connection problems occured", e);
    }
  }
  
  
  public boolean isNewChange(ChangeEntry changeEntry) {
    DBCollection monjeezlog = db.getCollection(MONJEEZ_CHANGELOG_COLLECTION);
    DBObject entry = monjeezlog.findOne(changeEntry.buildLocatingDBObject());

    return entry == null ? true : false;
  }

  public WriteResult save(ChangeEntry changeEntry) {
    DBCollection monjeezlog = db.getCollection(MONJEEZ_CHANGELOG_COLLECTION);
    return monjeezlog.save(changeEntry.buildFullDBObject());
  }
}
