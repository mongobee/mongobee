package com.github.mongobee.dao;

import com.github.fakemongo.Fongo;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.mongodb.FongoMongoCollection;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author lstolowski
 * @since 10.12.14
 */
public class ChangeEntryDaoTest {
  private static final String TEST_SERVER = "testServer";
  private static final String DB_NAME = "mongobeetest";
  private static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";
  private static final String LOCK_COLLECTION_NAME = "mongobeelock";

  @Test
  public void shouldCreateChangeIdAuthorIndexIfNotFound() throws MongobeeConfigurationException {

    // given
    ChangeEntryDao dao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME);

    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = new Fongo(TEST_SERVER).getDatabase(DB_NAME);

    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    when(indexDaoMock.findRequiredChangeAndAuthorIndex(db)).thenReturn(null);
    dao.setIndexDao(indexDaoMock);

    // when
    dao.connectMongoDb(mongoClient.getDatabase(DB_NAME), mongoClient.getDB(DB_NAME));

    //then
    verify(indexDaoMock, times(1)).createRequiredUniqueIndex(any(FongoMongoCollection.class));
    // and not
    verify(indexDaoMock, times(0)).dropIndex(any(FongoMongoCollection.class), any(Document.class));
  }

  @Test
  public void shouldNotCreateChangeIdAuthorIndexIfFound() throws MongobeeConfigurationException {

    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = new Fongo(TEST_SERVER).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME);
    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    when(indexDaoMock.findRequiredChangeAndAuthorIndex(db)).thenReturn(new Document());
    when(indexDaoMock.isUnique(any(Document.class))).thenReturn(true);
    dao.setIndexDao(indexDaoMock);

    // when
    dao.connectMongoDb(mongoClient.getDatabase(DB_NAME), mongoClient.getDB(DB_NAME));

    //then
    verify(indexDaoMock, times(0)).createRequiredUniqueIndex(db.getCollection(CHANGELOG_COLLECTION_NAME));
    // and not
    verify(indexDaoMock, times(0)).dropIndex(db.getCollection(CHANGELOG_COLLECTION_NAME), new Document());
  }

  @Test
  public void shouldRecreateChangeIdAuthorIndexIfFoundNotUnique() throws MongobeeConfigurationException {

    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = new Fongo(TEST_SERVER).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME);
    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    when(indexDaoMock.findRequiredChangeAndAuthorIndex(db)).thenReturn(new Document());
    when(indexDaoMock.isUnique(any(Document.class))).thenReturn(false);
    dao.setIndexDao(indexDaoMock);

    // when
    dao.connectMongoDb(mongoClient.getDatabase(DB_NAME), mongoClient.getDB(DB_NAME));

    //then
    verify(indexDaoMock, times(1)).dropIndex(any(FongoMongoCollection.class), any(Document.class));
    // and
    verify(indexDaoMock, times(1)).createRequiredUniqueIndex(any(FongoMongoCollection.class));
  }

}
