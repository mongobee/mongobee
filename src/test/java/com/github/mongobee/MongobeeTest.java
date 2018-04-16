package com.github.mongobee;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.github.mongobee.exception.MongobeeException;
import com.github.mongobee.exception.MongobeeLockException;
import com.github.mongobee.test.changelogs.MongobeeTestResource;
import com.github.mongobee.test.proxy.ProxiesMongobeeTestResource;
import com.github.mongobee.utils.TimeUtils;
import com.mongodb.DB;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jongo.Jongo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.Times;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MongobeeTest extends MongobeeTestBase {

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
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);
    runner.setMongoTemplate(mt);
    runner.afterPropertiesSet();
    verify(mt).getCollectionNames();
  }

  @Test
  public void shouldUsePreConfiguredJongo() throws Exception {
    setJongoField(null);
    Jongo jongo = mock(Jongo.class);
    when(proxyFactory.createProxyFromOriginal(jongo)).thenReturn(jongo);
    when(jongo.getDatabase()).thenReturn(null);
    runner.setJongo(jongo);
    runner.afterPropertiesSet();
    verify(jongo).getDatabase();
  }

  @Test
  public void shouldExecuteProcessWhenLockAcquired() throws Exception {

    // when
    runner.execute();

    // then
    verify(dao, atLeastOnce()).isNewChange(any(ChangeEntry.class));
  }

  @Test
  public void shouldReleaseLockAfterWhenLockAcquired() throws Exception {
    // when
    runner.execute();

    // then
    verify(lockChecker).releaseLockDefault();
  }

  @Test
  public void shouldNotExecuteProcessWhenLockNotAcquired() throws Exception {
    // given
    doThrow(new MongobeeLockException("")).when(lockChecker).acquireLockDefault();

    // when
    runner.execute();

    // then
    verify(dao, never()).isNewChange(any(ChangeEntry.class));
  }

  @Test(expected = MongobeeException.class)
  public void shouldNotExecuteProcessAndThrowsExceptionWhenLockNotAcquiredAndFlagThrowExceptionIfLockNotAcquiredTrue()
      throws Exception {
    // given
    doThrow(new MongobeeLockException("")).when(lockChecker).acquireLockDefault();
    runner.setThrowExceptionIfCannotObtainLock(true);

    // when
    runner.execute();

  }

  @Test
  public void shouldReturnExecutionStatusBasedOnDao() throws Exception {
    // given
    when(lockChecker.isLockHeld()).thenReturn(true);

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
    when(dao.isNewChange(any(ChangeEntry.class))).thenThrow(RuntimeException.class);

    // when
    // have to catch the exception to be able to verify after
    try {
      runner.execute();
    } catch (Exception e) {
      // do nothing
    }
    // then
    verify(lockChecker).releaseLockDefault();
  }

  @Test
  public void shouldCallLockCheckerWhenSetLockMaxWait() {
    //when
    runner.setChangeLogLockWaitTime(100);

    //then
    verify(lockChecker).setLockMaxWaitMillis(new TimeUtils().minutesToMillis(100));
  }

  @Test
  public void shouldCallLockCheckerMethodsWhenSetLockConfig() {
    //given
    when(lockChecker.setLockAcquiredForMillis(anyLong())).thenReturn(lockChecker);
    when(lockChecker.setLockMaxWaitMillis(anyLong())).thenReturn(lockChecker);
    when(lockChecker.setLockMaxTries(anyInt())).thenReturn(lockChecker);
    //when
    runner.setLockConfig(3, 4, 5);

    //then
    verify(lockChecker, new Times(1)).setLockAcquiredForMillis(new TimeUtils().minutesToMillis(3));
    verify(lockChecker, new Times(1)).setLockMaxWaitMillis(new TimeUtils().minutesToMillis(4));
    verify(lockChecker, new Times(1)).setLockMaxTries(5);
    verify(runner, new Times(1)).setThrowExceptionIfCannotObtainLock(true);
  }

  @Test
  public void shouldCallLockCheckerWithDefaultConfigMethodsWhenSetLockQuickConfig() {
    //given
    when(lockChecker.setLockAcquiredForMillis(anyLong())).thenReturn(lockChecker);
    when(lockChecker.setLockMaxWaitMillis(anyLong())).thenReturn(lockChecker);
    when(lockChecker.setLockMaxTries(anyInt())).thenReturn(lockChecker);
    //when
    runner.setLockQuickConfig();

    //then
    verify(lockChecker, new Times(1)).setLockAcquiredForMillis(new TimeUtils().minutesToMillis(3));
    verify(lockChecker, new Times(1)).setLockMaxWaitMillis(new TimeUtils().minutesToMillis(4));
    verify(lockChecker, new Times(1)).setLockMaxTries(3);
    verify(runner, new Times(1)).setThrowExceptionIfCannotObtainLock(true);
  }


  @Test
  public void callMongoClientWhenClosing() {
    //when
    runner.close();

    //then
    verify(runner.getMongoClient()).close();
  }

  @Test
  public void shouldCallLockCheckerWhenSetWaitForLockTrue() {
    //when
    runner.setWaitForLock(true);

    //then
    verify(lockChecker, new Times(1)).setLockMaxTries(2);
  }


  @Test
  public void shouldCallLockCheckerWhenSetWaitForLockFalse() {
    //when
    runner.setWaitForLock(false);

    //then
    verify(lockChecker, new Times(1)).setLockMaxTries(1);
  }

  @Test
  public void shouldCallLockRepositoryWhenSetLockCollectionName() {
    //when
    runner.setLockCollectionName("LOCK_COLLECTION_NAME");

    //then
    verify(lockRepository, new Times(1)).setLockCollectionName("LOCK_COLLECTION_NAME");
  }

  @Test
  public void shouldInjectProxyToChangeEntry() throws Exception {

    ProxiesMongobeeTestResource changeLog = mock(ProxiesMongobeeTestResource.class);

    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);
    doReturn(Collections.singletonList(ProxiesMongobeeTestResource.class))
        .when(changeService).fetchChangeLogs();
    doReturn(changeLog).when(changeService).createInstance(any(Class.class));
    doReturn(Arrays.asList(
        ProxiesMongobeeTestResource.class.getDeclaredMethod("testInsertWithDB", DB.class),
        ProxiesMongobeeTestResource.class.getDeclaredMethod("testJongo", Jongo.class),
        ProxiesMongobeeTestResource.class.getDeclaredMethod("testMongoDatabase", MongoDatabase.class)))
        .when(changeService).fetchChangeSets(any(Class.class));

    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);
    // given
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);
    when(proxyFactory.createProxyFromOriginal(fakeMongoDatabase)).thenReturn(fakeMongoDatabase);

    DB proxyDb = mock(DB.class);
    when(proxyFactory.createProxyFromOriginal(fakeDb)).thenReturn(proxyDb);

    Jongo proxyJongo = mock(Jongo.class);
    when(proxyFactory.createProxyFromOriginal(jongo)).thenReturn(proxyJongo);

    MongoDatabase proxyMongoDatabase = mock(MongoDatabase.class);
    when(proxyFactory.createProxyFromOriginal(fakeMongoDatabase)).thenReturn(proxyMongoDatabase);

    // when
    runner.execute();

    //then
    verify(changeLog, new Times(1)).testInsertWithDB(proxyDb);
    verify(changeLog, new Times(1)).testJongo(proxyJongo);
    verify(changeLog, new Times(1)).testMongoDatabase(proxyMongoDatabase);
  }

}
