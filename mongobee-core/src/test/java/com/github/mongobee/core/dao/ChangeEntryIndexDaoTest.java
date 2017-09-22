package com.github.mongobee.core.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bson.Document;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.github.fakemongo.Fongo;
import com.github.mongobee.core.changeset.ChangeEntry;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

/**
 * @author lstolowski
 * @since 10.12.14
 */
public class ChangeEntryIndexDaoTest {
  private static final String TEST_SERVER = "testServer";
  private static final String DB_NAME = "mongobeetest";
  private static final String CHANGEID_AUTHOR_INDEX_NAME = "changeId_1_author_1";
  private static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";

  private ChangeEntryIndexDao dao = new ChangeEntryIndexDao(CHANGELOG_COLLECTION_NAME);

  @Test
  public void shouldCreateRequiredUniqueIndex() {
    // given
    MongoClient mongo = mock(MongoClient.class);
    MongoDatabase db = new Fongo(TEST_SERVER).getDatabase(DB_NAME);
    when(mongo.getDatabase(Mockito.anyString())).thenReturn(db);

    // when
    dao.createRequiredUniqueIndex(db.getCollection(CHANGELOG_COLLECTION_NAME));

    // then
    Document createdIndex = findIndex(db, CHANGEID_AUTHOR_INDEX_NAME);
    assertNotNull(createdIndex);
    assertTrue(dao.isUnique(createdIndex));
  }

  @Test
  @Ignore("Fongo has not implemented dropIndex for MongoCollection object (issue with mongo driver 3.x)")
  public void shouldDropWrongIndex() {
    // init
    MongoClient mongo = mock(MongoClient.class);
    MongoDatabase db = new Fongo(TEST_SERVER).getDatabase(DB_NAME);
    when(mongo.getDatabase(Mockito.anyString())).thenReturn(db);

    MongoCollection<Document> collection = db.getCollection(CHANGELOG_COLLECTION_NAME);
    collection.createIndex(new Document()
        .append(ChangeEntry.KEY_CHANGEID, 1)
        .append(ChangeEntry.KEY_AUTHOR, 1));
    Document index = new Document("name", CHANGEID_AUTHOR_INDEX_NAME);

    // given
    Document createdIndex = findIndex(db, CHANGEID_AUTHOR_INDEX_NAME);
    assertNotNull(createdIndex);
    assertFalse(dao.isUnique(createdIndex));

    // when
    dao.dropIndex(db.getCollection(CHANGELOG_COLLECTION_NAME), index);

    // then
    assertNull(findIndex(db, CHANGEID_AUTHOR_INDEX_NAME));
  }

  private Document findIndex(MongoDatabase db, String indexName) {

    for (MongoCursor<Document> iterator = db.getCollection(CHANGELOG_COLLECTION_NAME).listIndexes().iterator(); iterator.hasNext(); ) {
      Document index = iterator.next();
      String name = (String) index.get("name");
      if (indexName.equals(name)) {
        return index;
      }
    }
    return null;
  }

}
