package com.github.mongobee.dao;

import com.github.mongobee.changeset.ChangeEntry;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

/**
 * @author lstolowski
 * @since 10.12.14
 *
 * @author Amit Sadafule
 * @since 19.08.149
 */
public class ChangeEntryIndexDao {


  private String changelogCollectionName;
	  
  public ChangeEntryIndexDao(String changelogCollectionName) {
	this.changelogCollectionName = changelogCollectionName;
  }

  public void createRequiredUniqueIndex(MongoCollection<Document> collection) {
    collection.createIndex(new Document()
            .append(ChangeEntry.KEY_CHANGEID, 1)
            .append(ChangeEntry.KEY_AUTHOR, 1),
        new IndexOptions().unique(true)
    );
  }

  public Document findRequiredChangeAndAuthorIndex(MongoDatabase db) {
    ListIndexesIterable<Document> indexes = db.getCollection(changelogCollectionName).listIndexes();
    if(indexes == null) {
      return null;
    }
    for(Document indexDefinition : indexes) {
      Document key = indexDefinition.get("key", Document.class);
      if(key.get(ChangeEntry.KEY_CHANGEID) != null && key.get(ChangeEntry.KEY_AUTHOR) != null) {
        return indexDefinition;
      }
    }
    return null;
  }

  public boolean isUnique(Document index) {
    Object unique = index.get("unique");
    if (unique != null && unique instanceof Boolean) {
      return (Boolean) unique;
    } else {
      return false;
    }
  }

  public void dropIndex(MongoCollection<Document> collection, Document index) {
    collection.dropIndex(index.get("name").toString());
  }

  public void setChangelogCollectionName(String changelogCollectionName) {
	this.changelogCollectionName = changelogCollectionName;
  }

}
