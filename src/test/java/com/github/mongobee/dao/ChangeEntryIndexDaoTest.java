package com.github.mongobee.dao;

import com.github.fakemongo.Fongo;
import com.github.mongobee.changeset.ChangeEntry;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import org.junit.Test;
import org.mockito.Mockito;

import static com.github.mongobee.changeset.ChangeEntry.CHANGELOG_COLLECTION;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author lstolowski
 * @since 10.12.14
 */
public class ChangeEntryIndexDaoTest {

  private static final String DB_NAME = "mongobeetest";
  private ChangeEntryIndexDao dao = new ChangeEntryIndexDao();

  @Test
  public void shouldCreateRequiredUniqueIndex(){
    // given
    Mongo mongo = mock(Mongo.class);
    DB db = new Fongo("testServer").getDB(DB_NAME);
    when(mongo.getDB(Mockito.anyString())).thenReturn(db);

    // when
    dao.createRequiredUniqueIndex(db.getCollection(CHANGELOG_COLLECTION));


    // then
    assertTrue(findIndex(db, "changeId_1_author_1"));
  }

  @Test
  public void shouldDropWrongIndex(){
    // init
    Mongo mongo = mock(Mongo.class);
    DB db = new Fongo("testServer").getDB(DB_NAME);
    when(mongo.getDB(Mockito.anyString())).thenReturn(db);


    DBCollection collection = db.getCollection(CHANGELOG_COLLECTION);
    collection.createIndex(new BasicDBObject()
            .append(ChangeEntry.KEY_CHANGEID, 1)
            .append(ChangeEntry.KEY_AUTHOR, 1));
    DBObject index = new BasicDBObject("name", "changeId_1_author_1");

    // given
    assertTrue(findIndex(db, "changeId_1_author_1"));

    // when
    dao.dropIndex(db.getCollection(CHANGELOG_COLLECTION), index);


    // then
    assertFalse(findIndex(db, "changeId_1_author_1"));

  }


  private boolean findIndex(DB db, String indexName){
    for (DBObject dbObject : db.getCollection(CHANGELOG_COLLECTION).getIndexInfo()) {
      String name = (String) dbObject.get("name");
      if (indexName.equals(name)){
        return true;
      }
    }
    return false;
  }

}