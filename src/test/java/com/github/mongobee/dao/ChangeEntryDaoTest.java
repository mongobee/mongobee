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
import static org.junit.Assert.assertTrue;

/**
 * @author lstolowski
 * @since 10.12.14
 */
public class ChangeEntryDaoTest {
  private static final String TEST_SERVER = "testServer";
  private static final String DB_NAME = "mongobeetest";

  @Test
  public void shouldCreateChangeIdAuthorIndexIfNotFound() throws MongobeeConfigurationException {

    // given
    Mongo mongo = mock(Mongo.class);
    DB db = new Fongo(TEST_SERVER).getDB(DB_NAME);
    when(mongo.getDB(Mockito.anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao();
    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    when(indexDaoMock.findRequiredChangeAndAuthorIndex(db)).thenReturn(null);
    dao.setIndexDao(indexDaoMock);

    // when
    dao.connectMongoDb(mongo, DB_NAME);

    //then
    verify(indexDaoMock, times(1)).createRequiredUniqueIndex(db.getCollection(CHANGELOG_COLLECTION));
    // and not
    verify(indexDaoMock, times(0)).dropIndex(db.getCollection(CHANGELOG_COLLECTION), new BasicDBObject());
  }

  @Test
  public void shouldNotCreateChangeIdAuthorIndexIfFound() throws MongobeeConfigurationException {

    // given
    Mongo mongo = mock(Mongo.class);
    DB db = new Fongo(TEST_SERVER).getDB(DB_NAME);
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
    // and not
    verify(indexDaoMock, times(0)).dropIndex(db.getCollection(CHANGELOG_COLLECTION), new BasicDBObject());
  }

  @Test
  public void shouldRecreateChangeIdAuthorIndexIfFoundNotUnique() throws MongobeeConfigurationException {

    // given
    Mongo mongo = mock(Mongo.class);
    DB db = new Fongo(TEST_SERVER).getDB(DB_NAME);
    when(mongo.getDB(Mockito.anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao();
    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    when(indexDaoMock.findRequiredChangeAndAuthorIndex(db)).thenReturn(new BasicDBObject());
    when(indexDaoMock.isUnique(any(DBObject.class))).thenReturn(false);
    dao.setIndexDao(indexDaoMock);

    // when
    dao.connectMongoDb(mongo, DB_NAME);

    //then
    verify(indexDaoMock, times(1)).dropIndex(db.getCollection(CHANGELOG_COLLECTION), new BasicDBObject());
    // and
    verify(indexDaoMock, times(1)).createRequiredUniqueIndex(db.getCollection(CHANGELOG_COLLECTION));
  }
  
  
  @Test
  public void shouldInitiateLock() throws MongobeeConfigurationException {

    // given
    Mongo mongo = mock(Mongo.class);
    DB db = new Fongo(TEST_SERVER).getDB(DB_NAME);
    when(mongo.getDB(Mockito.anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao();
    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    dao.setIndexDao(indexDaoMock);

    LockDao lockDao = mock(LockDao.class);
    dao.setLockDao(lockDao);
    
    // when
    dao.connectMongoDb(mongo, DB_NAME);

    // then
    verify(lockDao).intitializeLock(db);

  }
  
  @Test
  public void shouldGetLockWhenLockDaoGetsLock() throws Exception{
	 
	 // given
	 Mongo mongo = mock(Mongo.class);
	 DB db = new Fongo(TEST_SERVER).getDB(DB_NAME);
	 when(mongo.getDB(Mockito.anyString())).thenReturn(db);
 
	 ChangeEntryDao dao = new ChangeEntryDao();
	 
	 LockDao lockDao = mock(LockDao.class);
	 when(lockDao.acquireLock(any(DB.class))).thenReturn(true);
	 dao.setLockDao(lockDao);
	 
	 dao.connectMongoDb(mongo, DB_NAME);
	 
	 // when
	 boolean hasLock = dao.acquireProcessLock();
	 
	 // then
	 assertTrue(hasLock);
  }
  
  @Test
  public void shouldReleaseLockFromLockDao() throws Exception{
	 
	 // given
	 Mongo mongo = mock(Mongo.class);
	 DB db = new Fongo(TEST_SERVER).getDB(DB_NAME);
	 when(mongo.getDB(Mockito.anyString())).thenReturn(db);
 
	 ChangeEntryDao dao = new ChangeEntryDao();
	 
	 LockDao lockDao = mock(LockDao.class);
	 dao.setLockDao(lockDao);
	 
	 dao.connectMongoDb(mongo, DB_NAME);
	 
	 // when
	 dao.releaseProcessLock();
	 
	 // then
	 verify(lockDao).releaseLock(any(DB.class));
  }
  
  @Test
  public void shouldCheckLockHeldFromFromLockDao() throws Exception{
	 
	 // given
	 Mongo mongo = mock(Mongo.class);
	 DB db = new Fongo(TEST_SERVER).getDB(DB_NAME);
	 when(mongo.getDB(Mockito.anyString())).thenReturn(db);
 
	 ChangeEntryDao dao = new ChangeEntryDao();
	 
	 LockDao lockDao = mock(LockDao.class);
	 dao.setLockDao(lockDao);
	 
	 dao.connectMongoDb(mongo, DB_NAME);
	 
	 // when
	 when(lockDao.isLockHeld(db)).thenReturn(true);
	 
	 boolean lockHeld = dao.isProccessLockHeld();
	 
	 // then
	 assertTrue(lockHeld);
  }

}