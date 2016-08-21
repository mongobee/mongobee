package com.github.mongobee.dao;

import static com.github.mongobee.changeset.ChangeEntry.CHANGELOG_COLLECTION;
import static com.github.mongobee.changeset.ChangeEntry.KEY_CHANGELOGCLASS;

import com.github.mongobee.changeset.ChangeEntry;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * @author lstolowski
 * @since 10.12.14
 */
public class ChangeEntryIndexDao {

    public void createRequiredUniqueIndex(DBCollection collection) {
        collection.createIndex(new BasicDBObject()
                                       .append(ChangeEntry.KEY_CHANGEID, 1)
                                       .append(ChangeEntry.KEY_AUTHOR, 1)
                                       .append(ChangeEntry.KEY_CHANGELOGCLASS, 1),

                               new BasicDBObject().append("unique", true));
    }

    public DBObject findRequiredChangeAndAuthorIndex(DB db) {
        DBCollection indexes = db.getCollection("system.indexes");
        DBObject index = indexes.findOne(new BasicDBObject()
                                                 .append("ns", db.getName() + "." + CHANGELOG_COLLECTION)
                                                 .append("key", new BasicDBObject().append(ChangeEntry.KEY_CHANGEID, 1).append(ChangeEntry.KEY_AUTHOR, 1))
                                        );

        return index;
    }

    public boolean isUnique(DBObject index) {
        Object unique = index.get("unique");
        if (unique != null && unique instanceof Boolean) {
            return (Boolean) unique;
        } else {
            return false;
        }
    }

    public void dropIndex(DBCollection collection, DBObject index) {
        collection.dropIndex(index.get("name").toString());
    }

}
