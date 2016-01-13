package com.github.mongobee.spring.test;

import com.github.fakemongo.Fongo;
import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.dao.ChangeEntryDao;
import com.github.mongobee.spring.resources.EnvironmentMock;
import com.github.mongobee.spring.SpringMongobee;
import com.github.mongobee.spring.test.changelogs.AnotherMongobeeTestResource;
import com.github.mongobee.spring.test.profiles.def.UnProfiledChangeLog;
import com.github.mongobee.spring.test.profiles.dev.ProfiledDevChangeLog;
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
public class SpringMongobeeProfileTest {

  public static final int CHANGELOG_COUNT = 7;
  @InjectMocks
  private SpringMongobee runner = new SpringMongobee();

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
    runner.setChangeLogsScanPackage(ProfiledDevChangeLog.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.afterPropertiesSet();

    // then
    int change1 = fakeDb.getCollection(CHANGELOG_COLLECTION)
          .find(new BasicDBObject()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev1")
            .append(ChangeEntry.KEY_AUTHOR, "testuser")).count();
    assertEquals(1, change1);  //  no-@Profile  should not match

    int change2 = fakeDb.getCollection(CHANGELOG_COLLECTION)
          .find(new BasicDBObject()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev4")
            .append(ChangeEntry.KEY_AUTHOR, "testuser")).count();
    assertEquals(1, change2);  //  @Profile("dev")  should not match

    int change3 = fakeDb.getCollection(CHANGELOG_COLLECTION)
          .find(new BasicDBObject()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev3")
            .append(ChangeEntry.KEY_AUTHOR, "testuser")).count();
    assertEquals(0, change3);  //  @Profile("default")  should not match
  }

  @Test
  public void shouldRunUnprofiledChangeLog() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("test"));
    runner.setChangeLogsScanPackage(UnProfiledChangeLog.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.afterPropertiesSet();

    // then
    int change1 = fakeDb.getCollection(CHANGELOG_COLLECTION)
      .find(new BasicDBObject()
        .append(ChangeEntry.KEY_CHANGEID, "Pdev1")
        .append(ChangeEntry.KEY_AUTHOR, "testuser")).count();
    assertEquals(1, change1);

    int change2 = fakeDb.getCollection(CHANGELOG_COLLECTION)
      .find(new BasicDBObject()
        .append(ChangeEntry.KEY_CHANGEID, "Pdev2")
        .append(ChangeEntry.KEY_AUTHOR, "testuser")).count();
    assertEquals(1, change2);

    int change3 = fakeDb.getCollection(CHANGELOG_COLLECTION)
      .find(new BasicDBObject()
        .append(ChangeEntry.KEY_CHANGEID, "Pdev3")
        .append(ChangeEntry.KEY_AUTHOR, "testuser")).count();
    assertEquals(1, change3);  //  @Profile("dev")  should not match

    int change4 = fakeDb.getCollection(CHANGELOG_COLLECTION)
      .find(new BasicDBObject()
        .append(ChangeEntry.KEY_CHANGEID, "Pdev4")
        .append(ChangeEntry.KEY_AUTHOR, "testuser")).count();
    assertEquals(0, change4);  //  @Profile("default")  should not match
  }

  @Test
  public void shouldNotRunAnyChangeSet() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("foobar"));
    runner.setChangeLogsScanPackage(ProfiledDevChangeLog.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.afterPropertiesSet();

    // then
    int changes = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()).count();
    assertEquals(0, changes);
  }

  @Test
  public void shouldRunChangeSetsWhenNoEnv() throws Exception {
    // given
    runner.setSpringEnvironment(null);
    runner.setChangeLogsScanPackage(AnotherMongobeeTestResource.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.afterPropertiesSet();

    // then
    int changes = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()).count();
    assertEquals(CHANGELOG_COUNT, changes);
  }

  @Test
  public void shouldRunChangeSetsWhenEmptyEnv() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock());
    runner.setChangeLogsScanPackage(AnotherMongobeeTestResource.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.afterPropertiesSet();

    // then
    int changes = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()).count();
    assertEquals(CHANGELOG_COUNT, changes);
  }

  @Test
  public void shouldRunAllChangeSets() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("dev"));
    runner.setChangeLogsScanPackage(AnotherMongobeeTestResource.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.afterPropertiesSet();

    // then
    int changes = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()).count();
    assertEquals(CHANGELOG_COUNT, changes);
  }

}
