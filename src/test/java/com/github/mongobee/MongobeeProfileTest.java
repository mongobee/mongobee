package com.github.mongobee;

import com.github.fakemongo.Fongo;
import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.dao.ChangeEntryDao;
import com.github.mongobee.dao.ChangeEntryIndexDao;
import com.github.mongobee.resources.EnvironmentMock;
import com.github.mongobee.test.changelogs.AnotherMongobeeTestResource;
import com.github.mongobee.test.profiles.def.UnProfiledChangeLog;
import com.github.mongobee.test.profiles.dev.ProfiledDevChangeLog;
import com.mongodb.DB;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

/**
 * Tests for Spring profiles integration
 *
 * @author lstolowski
 * @since 2014-09-17
 */
@RunWith(MockitoJUnitRunner.class)
public class MongobeeProfileTest {
  private static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";
  public static final int CHANGELOG_COUNT = 13;

  @InjectMocks
  private Mongobee runner = new Mongobee();

  @Mock
  private ChangeEntryDao dao;
  
  @Mock
  private ChangeEntryIndexDao indexDao;

  private DB fakeDb;

  private MongoDatabase fakeMongoDatabase;

  @Before
  public void init() throws Exception {
    fakeDb = new Fongo("testServer").getDB("mongobeetest");
    fakeMongoDatabase = new Fongo("testServer").getDatabase("mongobeetest");

    when(dao.connectMongoDb(any(MongoClientURI.class), anyString()))
        .thenReturn(fakeMongoDatabase);
    when(dao.getDb()).thenReturn(fakeDb);
    when(dao.getMongoDatabase()).thenReturn(fakeMongoDatabase);
    when(dao.acquireProcessLock()).thenReturn(true);
    doCallRealMethod().when(dao).save(any(ChangeEntry.class));
    doCallRealMethod().when(dao).setChangelogCollectionName(anyString());
    doCallRealMethod().when(dao).setIndexDao(any(ChangeEntryIndexDao.class));
    dao.setIndexDao(indexDao);
    dao.setChangelogCollectionName(CHANGELOG_COLLECTION_NAME);

    runner.setDbName("mongobeetest");
    runner.setEnabled(true);
  } // TODO code duplication

  @Test
  public void shouldRunDevProfileAndNonAnnotated() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("dev", "test"));
    runner.setChangeLogsScanPackage(ProfiledDevChangeLog.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    long change1 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .count(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev1")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change1);  //  no-@Profile  should not match

    long change2 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .count(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev4")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change2);  //  @Profile("dev")  should not match

    long change3 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .count(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev3")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(0, change3);  //  @Profile("default")  should not match
  }

  @Test
  public void shouldRunUnprofiledChangeLog() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("test"));
    runner.setChangeLogsScanPackage(UnProfiledChangeLog.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    long change1 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .count(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev1")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change1);

    long change2 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .count(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev2")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change2);

    long change3 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .count(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev3")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change3);  //  @Profile("dev")  should not match

    long change4 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .count(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev4")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(0, change4);  //  @Profile("pro")  should not match

    long change5 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .count(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev5")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change5);  //  @Profile("!pro")  should match
  }

  @Test
  public void shouldNotRunAnyChangeSet() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("foobar"));
    runner.setChangeLogsScanPackage(ProfiledDevChangeLog.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    long changes = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME).count(new Document());
    assertEquals(0, changes);
  }

  @Test
  public void shouldRunChangeSetsWhenNoEnv() throws Exception {
    // given
    runner.setSpringEnvironment(null);
    runner.setChangeLogsScanPackage(AnotherMongobeeTestResource.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    long changes = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME).count(new Document());
    assertEquals(CHANGELOG_COUNT, changes);
  }

  @Test
  public void shouldRunChangeSetsWhenEmptyEnv() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock());
    runner.setChangeLogsScanPackage(AnotherMongobeeTestResource.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    long changes = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME).count(new Document());
    assertEquals(CHANGELOG_COUNT, changes);
  }

  @Test
  public void shouldRunAllChangeSets() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("dev"));
    runner.setChangeLogsScanPackage(AnotherMongobeeTestResource.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    long changes = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME).count(new Document());
    assertEquals(CHANGELOG_COUNT, changes);
  }

  @After
  public void cleanUp() {
    runner.setMongoTemplate(null);
    runner.setJongo(null);
    fakeDb.dropDatabase();
  }

}
