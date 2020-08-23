package com.github.mongobee.dao;

import com.github.mongobee.changeset.ChangeEntry;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

/**
 * @author lstolowski
 * @since 10.12.14
 */
public class ChangeEntryIndexDao {

  private String changelogCollectionName;

  public ChangeEntryIndexDao(String changelogCollectionName) {
    this.changelogCollectionName = changelogCollectionName;
  }

  public void createRequiredUniqueIndex(MongoCollection<Document> collection) {
    collection.createIndex(
        new Document().append(ChangeEntry.KEY_CHANGEID, 1).append(ChangeEntry.KEY_AUTHOR, 1),
        new IndexOptions().unique(true));
  }

  public Document findRequiredChangeAndAuthorIndex(MongoDatabase db) {
    MongoCollection<Document> indexes = db.getCollection("system.indexes");
    Document index =
        indexes
            .find(
                new Document()
                    .append("ns", db.getName() + "." + changelogCollectionName)
                    .append(
                        "key",
                        new Document()
                            .append(ChangeEntry.KEY_CHANGEID, 1)
                            .append(ChangeEntry.KEY_AUTHOR, 1)))
            .first();

    return index;
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
