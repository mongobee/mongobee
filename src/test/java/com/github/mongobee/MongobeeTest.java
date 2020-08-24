package com.github.mongobee;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.github.mongobee.test.changelogs.MongobeeTestResource;
import com.mongodb.MongoClientURI;
import java.util.Collections;
import org.bson.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

@RunWith(MockitoJUnitRunner.class)
public class MongobeeTest extends AbstractTest {

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
    verify(dao, times(8)).save(any(ChangeEntry.class)); // 13 changesets saved to dbchangelog

    // dbchangelog collection checking
    long change1 =
        fakeMongoDatabase
            .getCollection(CHANGELOG_COLLECTION_NAME)
            .countDocuments(
                new Document()
                    .append(ChangeEntry.KEY_CHANGEID, "test1")
                    .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change1);
    long change2 =
        fakeMongoDatabase
            .getCollection(CHANGELOG_COLLECTION_NAME)
            .countDocuments(
                new Document()
                    .append(ChangeEntry.KEY_CHANGEID, "test2")
                    .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change2);
    long change5 =
        fakeMongoDatabase
            .getCollection(CHANGELOG_COLLECTION_NAME)
            .countDocuments(
                new Document()
                    .append(ChangeEntry.KEY_CHANGEID, "test5")
                    .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change5);

    long changeAll =
        fakeMongoDatabase
            .getCollection(CHANGELOG_COLLECTION_NAME)
            .countDocuments(new Document().append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(7, changeAll);
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
    // would be nicer with a mock for the whole execution, but this would mean breaking out to
    // separate class..
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
}
