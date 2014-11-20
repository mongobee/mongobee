package com.github.mongobee;

import com.github.fakemongo.Fongo;
import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.dao.ChangeEntryDao;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.github.mongobee.exception.MongobeeException;
import com.github.mongobee.test.changelogs.MongobeeTestResource;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClientURI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.UnknownHostException;

import static com.github.mongobee.changeset.ChangeEntry.CHANGELOG_COLLECTION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MongobeeTest {

  @InjectMocks
  private Mongobee runner = new Mongobee();

  @Mock
  private ChangeEntryDao dao;

  private DB fakeDb;

  @Before
  public void init() throws MongobeeException, UnknownHostException {
    fakeDb = new Fongo("testServer").getDB("mongobeetest");
    when(dao.connectMongoDb(any(MongoClientURI.class), anyString()))
      .thenReturn(fakeDb);
    when(dao.getDb()).thenReturn(fakeDb);
    when(dao.save(any(ChangeEntry.class))).thenCallRealMethod();

    runner.setDbName("mongobeetest");
    runner.setEnabled(true);
    runner.setChangeLogsScanPackage(MongobeeTestResource.class.getPackage().getName());
  }


  @Test(expected = MongobeeConfigurationException.class)
  public void shouldThrowAnExceptionIfNoDbNameSet() throws Exception{
    Mongobee runner = new Mongobee(new MongoClientURI("mongodb://localhost:27017/"));
    runner.setEnabled(true);
    runner.setChangeLogsScanPackage(MongobeeTestResource.class.getPackage().getName());
    runner.execute();
  }

  @Test
  public void shouldExecute9ChangeSets() throws Exception {
    // given
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    verify(dao, times(9)).save(any(ChangeEntry.class)); // 9 changesets saved to dbchangelog

      // dbchangelog collection checking
    int change1 = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()
      .append("changeId", "test1")
      .append("author", "testuser")).count();
    assertEquals(1, change1);
    int change2 = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()
      .append("changeId", "test2")
      .append("author", "testuser")).count();
    assertEquals(1, change2);
    int change3 = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()
      .append("changeId", "test3")
      .append("author", "testuser")).count();
    assertEquals(1, change3);
    int change4 = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()
      .append("changeId", "test4")
      .append("author", "testuser")).count();
    assertEquals(1, change4);
    int changeAll = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()
      .append("author", "testuser")).count();
    assertEquals(9, changeAll);
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

}
