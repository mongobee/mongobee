package com.github.mongobee.dao;

import com.github.fakemongo.Fongo;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import org.junit.Test;
import org.mockito.Mockito;

import static com.github.mongobee.changeset.ChangeEntry.CHANGELOG_COLLECTION;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author lstolowski
 * @since 10.12.14
 */
public class ChangeEntryDaoTest {
  private static final String DB_NAME = "mongobeetest";

  @Test
  public void shouldCreateChangeIdAuthorIndexIfNotFound() throws MongobeeConfigurationException {

    // given
    Mongo mongo = mock(Mongo.class);
    DB db = new Fongo("testServer").getDB(DB_NAME);
    when(mongo.getDB(Mockito.anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao();
    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    when(indexDaoMock.findRequiredChangeAndAuthorIndex(db)).thenReturn(null);
    dao.setIndexDao(indexDaoMock);

    // when
    dao.connectMongoDb(mongo, DB_NAME);

    //then
    verify(indexDaoMock, times(1)).createRequiredUniqueIndex(db.getCollection(CHANGELOG_COLLECTION));
    verify(indexDaoMock, times(0)).dropIndex(db.getCollection(CHANGELOG_COLLECTION), new BasicDBObject());
  }

  @Test
  public void shouldNotCreateChangeIdAuthorIndexIfFound() throws MongobeeConfigurationException {

    // given
    Mongo mongo = mock(Mongo.class);
    DB db = new Fongo("testServer").getDB(DB_NAME);
    when(mongo.getDB(Mockito.anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao();
    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    when(indexDaoMock.findRequiredChangeAndAuthorIndex(db)).thenReturn(new BasicDBObject());
    when(indexDaoMock.isUnique(any(DBObject.class))).thenReturn(true);
    dao.setIndexDao(indexDaoMock);

    // when
    dao.connectMongoDb(mongo, DB_NAME);

    //then
    verify(indexDaoMock, times(0)).createRequiredUniqueIndex(db.getCollection(CHANGELOG_COLLECTION));
    verify(indexDaoMock, times(0)).dropIndex(db.getCollection(CHANGELOG_COLLECTION), new BasicDBObject());
  }

  @Test
  public void shouldCreateChangeIdAuthorIndexIfNotUnique() throws MongobeeConfigurationException {

    // given
    Mongo mongo = mock(Mongo.class);
    DB db = new Fongo("testServer").getDB(DB_NAME);
    when(mongo.getDB(Mockito.anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao();
    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    when(indexDaoMock.findRequiredChangeAndAuthorIndex(db)).thenReturn(new BasicDBObject());
    when(indexDaoMock.isUnique(any(DBObject.class))).thenReturn(false);
    dao.setIndexDao(indexDaoMock);

    // when
    dao.connectMongoDb(mongo, DB_NAME);

    //then
    verify(indexDaoMock, times(1)).createRequiredUniqueIndex(db.getCollection(CHANGELOG_COLLECTION));
    verify(indexDaoMock, times(1)).dropIndex(db.getCollection(CHANGELOG_COLLECTION), new BasicDBObject());
  }


}