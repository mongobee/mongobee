package com.github.mongobee.dao;

import static com.github.mongobee.changeset.ChangeEntry.CHANGELOG_COLLECTION;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;

import com.github.fakemongo.Fongo;
import com.github.mongobee.changeset.ChangeEntry;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

/**
 * @author lstolowski
 * @since 10.12.14
 */
public class ChangeEntryIndexDaoTest {
    private static final String TEST_SERVER = "testServer";
    private static final String DB_NAME = "mongobeetest";
    private static final String CHANGEID_AUTHOR_CHANGELOGCLASS_INDEX_NAME = "changeId_1_author_1_changeLogClass_1";

    private ChangeEntryIndexDao dao = new ChangeEntryIndexDao();

    @Test
    public void shouldCreateRequiredUniqueIndex() {
        // given
        Mongo mongo = mock(Mongo.class);
        DB db = new Fongo(TEST_SERVER).getDB(DB_NAME);
        when(mongo.getDB(Mockito.anyString())).thenReturn(db);

        // when
        dao.createRequiredUniqueIndex(db.getCollection(CHANGELOG_COLLECTION));

        // then
        DBObject createdIndex = findIndex(db, CHANGEID_AUTHOR_CHANGELOGCLASS_INDEX_NAME);
        assertNotNull(createdIndex);
        assertTrue(dao.isUnique(createdIndex));
    }

    @Test
    public void shouldDropWrongIndex() {
        // init
        Mongo mongo = mock(Mongo.class);
        DB db = new Fongo(TEST_SERVER).getDB(DB_NAME);
        when(mongo.getDB(Mockito.anyString())).thenReturn(db);

        DBCollection collection = db.getCollection(CHANGELOG_COLLECTION);
        collection.createIndex(new BasicDBObject()
                                       .append(ChangeEntry.KEY_CHANGEID, 1)
                                       .append(ChangeEntry.KEY_AUTHOR, 1)
                                       .append(ChangeEntry.KEY_CHANGELOGCLASS, 1));
        DBObject index = new BasicDBObject("name", CHANGEID_AUTHOR_CHANGELOGCLASS_INDEX_NAME);

        // given
        DBObject createdIndex = findIndex(db, CHANGEID_AUTHOR_CHANGELOGCLASS_INDEX_NAME);
        assertNotNull(createdIndex);
        assertFalse(dao.isUnique(createdIndex));

        // when
        dao.dropIndex(db.getCollection(CHANGELOG_COLLECTION), index);

        // then
        assertNull(findIndex(db, CHANGEID_AUTHOR_CHANGELOGCLASS_INDEX_NAME));
    }

    private DBObject findIndex(DB db, String indexName) {
        for (DBObject index : db.getCollection(CHANGELOG_COLLECTION).getIndexInfo()) {
            String name = (String) index.get("name");
            if (indexName.equals(name)) {
                return index;
            }
        }
        return null;
    }

}
