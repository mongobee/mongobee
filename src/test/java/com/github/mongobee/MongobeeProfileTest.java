package com.github.mongobee;

import com.github.fakemongo.Fongo;
import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.dao.ChangeEntryDao;
import com.github.mongobee.resources.EnvironmentMock;
import com.github.mongobee.test.changelogs.AnotherMongobeeTestResource;
import com.github.mongobee.test.profiles.def.UnProfiledChangelog;
import com.github.mongobee.test.profiles.dev.ProfiledDevChangelog;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClientURI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.github.mongobee.changeset.ChangeEntry.CHANGELOG_COLLECTION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for Spring profiles integration
 *
 * @author lstolowski
 * @since 2014-09-17
 */
@RunWith(MockitoJUnitRunner.class)
public class MongobeeProfileTest {

  @InjectMocks
  private Mongobee runner = new Mongobee();

  @Mock
  private ChangeEntryDao dao;

  private DB fakeDb;

  @Before
  public void init() throws Exception {

    fakeDb = new Fongo("testServer").getDB("mongobeetest");
    when(dao.connectMongoDb(any(MongoClientURI.class), anyString()))
      .thenReturn(fakeDb);
    when(dao.getDb()).thenReturn(fakeDb);
    when(dao.save(any(ChangeEntry.class))).thenCallRealMethod();

    runner.setDbName("mongobeetest");
    runner.setEnabled(true);
  }

  @Test
  public void shouldRunDevProfileAndNonAnnotated() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("dev", "test"));
    runner.setChangelogsScanPackage(ProfiledDevChangelog.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    int change1 = fakeDb.getCollection(CHANGELOG_COLLECTION)
          .find(new BasicDBObject()
            .append("changeId", "Pdev1")
            .append("author", "testuser")).count();
    assertEquals(1, change1);  //  no-@Profile  should not match

    int change2 = fakeDb.getCollection(CHANGELOG_COLLECTION)
          .find(new BasicDBObject()
            .append("changeId", "Pdev4")
            .append("author", "testuser")).count();
    assertEquals(1, change2);  //  @Profile("dev")  should not match

    int change3 = fakeDb.getCollection(CHANGELOG_COLLECTION)
          .find(new BasicDBObject()
            .append("changeId", "Pdev3")
            .append("author", "testuser")).count();
    assertEquals(0, change3);  //  @Profile("default")  should not match
  }

  @Test
  public void shouldRunUnprofiledChangelog() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("test"));
    runner.setChangelogsScanPackage(UnProfiledChangelog.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    int change1 = fakeDb.getCollection(CHANGELOG_COLLECTION)
      .find(new BasicDBObject()
        .append("changeId", "Pdev1")
        .append("author", "testuser")).count();
    assertEquals(1, change1);

    int change2 = fakeDb.getCollection(CHANGELOG_COLLECTION)
      .find(new BasicDBObject()
        .append("changeId", "Pdev2")
        .append("author", "testuser")).count();
    assertEquals(1, change2);

    int change3 = fakeDb.getCollection(CHANGELOG_COLLECTION)
      .find(new BasicDBObject()
        .append("changeId", "Pdev3")
        .append("author", "testuser")).count();
    assertEquals(1, change3);  //  @Profile("dev")  should not match

    int change4 = fakeDb.getCollection(CHANGELOG_COLLECTION)
      .find(new BasicDBObject()
        .append("changeId", "Pdev4")
        .append("author", "testuser")).count();
    assertEquals(0, change4);  //  @Profile("default")  should not match
  }

  @Test
  public void shouldNotRunAnyChangeset() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("foobar"));
    runner.setChangelogsScanPackage(ProfiledDevChangelog.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    int changes = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()).count();
    assertEquals(0, changes);
  }

  @Test
  public void shouldRunChangesetsWhenNoEnv() throws Exception {
    // given
    runner.setSpringEnvironment(null);
    runner.setChangelogsScanPackage(AnotherMongobeeTestResource.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    int changes = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()).count();
    assertEquals(9, changes);
  }

  @Test
  public void shouldRunChangesetsWhenEmptyEnv() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock());
    runner.setChangelogsScanPackage(AnotherMongobeeTestResource.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    int changes = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()).count();
    assertEquals(9, changes);
  }

  @Test
  public void shouldRunAllChangesets() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("dev"));
    runner.setChangelogsScanPackage(AnotherMongobeeTestResource.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    int changes = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()).count();
    assertEquals(9, changes);
  }

}
