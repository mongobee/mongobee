package com.github.mongobee;

import com.github.fakemongo.Fongo;
import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.dao.ChangeEntryDao;
import com.github.mongobee.dao.ChangeEntryIndexDao;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.github.mongobee.exception.MongobeeException;
import com.github.mongobee.test.changelogs.MongobeeTestResource;
import com.mongodb.DB;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jongo.Jongo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.net.UnknownHostException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MongobeeTest {

  private static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";
  @InjectMocks
  private Mongobee runner = new Mongobee();

  @Mock
  private ChangeEntryDao dao;

  @Mock
  private ChangeEntryIndexDao indexDao;

  private DB fakeDb;
  private MongoDatabase fakeMongoDatabase;

  @Before
  public void init() throws MongobeeException, UnknownHostException {
    fakeDb = new Fongo("testServer").getDB("mongobeetest");
    fakeMongoDatabase = new Fongo("testServer").getDatabase("mongobeetest");
    when(dao.connectMongoDb(any(MongoClientURI.class), anyString()))
        .thenReturn(fakeMongoDatabase);
    when(dao.getDb()).thenReturn(fakeDb);
    when(dao.getMongoDatabase()).thenReturn(fakeMongoDatabase);
    doCallRealMethod().when(dao).save(any(ChangeEntry.class));
    doCallRealMethod().when(dao).setChangelogCollectionName(anyString());
    doCallRealMethod().when(dao).setIndexDao(any(ChangeEntryIndexDao.class));
    dao.setIndexDao(indexDao);
    dao.setChangelogCollectionName(CHANGELOG_COLLECTION_NAME);

    runner.setDbName("mongobeetest");
    runner.setEnabled(true);
    runner.setChangeLogsScanPackage(MongobeeTestResource.class.getPackage().getName());
  }

  @Test(expected = MongobeeConfigurationException.class)
  public void shouldThrowAnExceptionIfNoDbNameSet() throws Exception {
    Mongobee runner = new Mongobee(new MongoClientURI("mongodb://localhost:27017/"));
    runner.setEnabled(true);
    runner.setChangeLogsScanPackage(MongobeeTestResource.class.getPackage().getName());
    runner.execute();
  }

  @Test
  public void shouldExecuteAllChangeSets() throws Exception {
    // given
    when(dao.acquireProcessLock()).thenReturn(true);
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    verify(dao, times(13)).save(any(ChangeEntry.class)); // 13 changesets saved to dbchangelog

    // dbchangelog collection checking
    long change1 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME).count(new Document()
        .append(ChangeEntry.KEY_CHANGEID, "test1")
        .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change1);
    long change2 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME).count(new Document()
        .append(ChangeEntry.KEY_CHANGEID, "test2")
        .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change2);
    long change3 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME).count(new Document()
        .append(ChangeEntry.KEY_CHANGEID, "test3")
        .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change3);
    long change4 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME).count(new Document()
        .append(ChangeEntry.KEY_CHANGEID, "test4")
        .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change4);
    long change5 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME).count(new Document()
        .append(ChangeEntry.KEY_CHANGEID, "test5")
        .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change5);

    long changeAll = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME).count(new Document()
        .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(12, changeAll);
  }

  @Test
  public void shouldPassOverChangeSets() throws Exception {
    // given
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(false);

    // when
    runner.execute();

    // then
    verify(dao, times(0)).save(any(ChangeEntry.class)); // no changesets saved to dbchangelog
  }

  @Test
  public void shouldUsePreConfiguredMongoTemplate() throws Exception {
    MongoTemplate mt = mock(MongoTemplate.class);
    when(mt.getCollectionNames()).thenReturn(Collections.EMPTY_SET);
    when(dao.acquireProcessLock()).thenReturn(true);
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);
    runner.setMongoTemplate(mt);
    runner.afterPropertiesSet();
    verify(mt).getCollectionNames();
  }

  @Test
  public void shouldUsePreConfiguredJongo() throws Exception {
    Jongo jongo = mock(Jongo.class);
    when(dao.acquireProcessLock()).thenReturn(true);
    when(jongo.getDatabase()).thenReturn(null);
    runner.setJongo(jongo);
    runner.afterPropertiesSet();
    verify(jongo).getDatabase();
  }

  @Test
  public void shouldExecuteProcessWhenLockAcquired() throws Exception {
    // given
    when(dao.acquireProcessLock()).thenReturn(true);

    // when
    runner.execute();

    // then
    verify(dao, atLeastOnce()).isNewChange(any(ChangeEntry.class));
  }

  @Test
  public void shouldReleaseLockAfterWhenLockAcquired() throws Exception {
    // given
    when(dao.acquireProcessLock()).thenReturn(true);

    // when
    runner.execute();

    // then
    verify(dao).releaseProcessLock();
  }

  @Test
  public void shouldNotExecuteProcessWhenLockNotAcquired() throws Exception {
    // given
    when(dao.acquireProcessLock()).thenReturn(false);

    // when
    runner.execute();

    // then
    verify(dao, never()).isNewChange(any(ChangeEntry.class));
  }

  @Test
  public void shouldReturnExecutionStatusBasedOnDao() throws Exception {
    // given
    when(dao.isProccessLockHeld()).thenReturn(true);

    boolean inProgress = runner.isExecutionInProgress();

    // then
    assertTrue(inProgress);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldReleaseLockWhenExceptionInMigration() throws Exception {

    // given
    // would be nicer with a mock for the whole execution, but this would mean breaking out to separate class..
    // this should be "good enough"
    when(dao.acquireProcessLock()).thenReturn(true);
    when(dao.isNewChange(any(ChangeEntry.class))).thenThrow(RuntimeException.class);

    // when
    // have to catch the exception to be able to verify after
    try {
      runner.execute();
    } catch (Exception e) {
      // do nothing
    }
    // then
    verify(dao).releaseProcessLock();

  }

  @After
  public void cleanUp() {
    runner.setMongoTemplate(null);
    runner.setJongo(null);
    fakeDb.dropDatabase();
  }

}
