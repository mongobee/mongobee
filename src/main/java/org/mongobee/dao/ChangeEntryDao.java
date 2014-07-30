package org.mongobee.dao;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import org.mongobee.changeset.ChangeEntry;

import static org.mongobee.changeset.ChangeEntry.CHANGELOG_COLLECTION;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeEntryDao {

  private DB db;
  
  public ChangeEntryDao(DB db) {
    this.db = db;
  }
  
  public boolean isNewChange(ChangeEntry changeEntry) {
    DBCollection monjeezlog = db.getCollection(CHANGELOG_COLLECTION);
    DBObject entry = monjeezlog.findOne(changeEntry.buildLocatingDBObject());

    return entry == null ? true : false;
  }

  public WriteResult save(ChangeEntry changeEntry) {
    DBCollection monjeezlog = db.getCollection(CHANGELOG_COLLECTION);
    return monjeezlog.save(changeEntry.buildFullDBObject());
  }
}
