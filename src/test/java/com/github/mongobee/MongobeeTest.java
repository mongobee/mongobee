package com.github.mongobee;

import static com.github.mongobee.changeset.ChangeEntry.CHANGELOG_COLLECTION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.UnknownHostException;
import java.util.Collections;

import org.jongo.Jongo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.github.fakemongo.Fongo;
import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.dao.ChangeEntryDao;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.github.mongobee.exception.MongobeeException;
import com.github.mongobee.test.changelogs.MongobeeTestResource;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;

@RunWith(MockitoJUnitRunner.class)
public class MongobeeTest {

  @InjectMocks
  private Mongobee runner = new Mongobee();

  @Mock
  private ChangeEntryDao dao;

  private DB fakeDb;
  private MongoDatabase fakeMongoDatabase;

  @Before
  public void init() throws MongobeeException, UnknownHostException {
    fakeDb = new Fongo("testServer").getDB("mongobeetest");
    fakeMongoDatabase = new Fongo("testServer").getDatabase("mongobeetest");
    when(dao.connectMongoDb(any(MongoClientURI.class), anyString()))
        .thenReturn(fakeDb);
    when(dao.getDb()).thenReturn(fakeDb);
    when(dao.getMongoDatabase()).thenReturn(fakeMongoDatabase);
    when(dao.save(any(ChangeEntry.class))).thenCallRealMethod();

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
  public void shouldExecute9ChangeSets() throws Exception {
    // given
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    verify(dao, times(12)).save(any(ChangeEntry.class)); // 12 changesets saved to dbchangelog

    // dbchangelog collection checking
    int change1 = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()
        .append(ChangeEntry.KEY_CHANGEID, "test1")
        .append(ChangeEntry.KEY_AUTHOR, "testuser")).count();
    assertEquals(1, change1);
    int change2 = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()
        .append(ChangeEntry.KEY_CHANGEID, "test2")
        .append(ChangeEntry.KEY_AUTHOR, "testuser")).count();
    assertEquals(1, change2);
    int change3 = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()
        .append(ChangeEntry.KEY_CHANGEID, "test3")
        .append(ChangeEntry.KEY_AUTHOR, "testuser")).count();
    assertEquals(1, change3);
    int change4 = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()
        .append(ChangeEntry.KEY_CHANGEID, "test4")
        .append(ChangeEntry.KEY_AUTHOR, "testuser")).count();
    assertEquals(1, change4);
    int change5 = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()
            .append(ChangeEntry.KEY_CHANGEID, "test5")
            .append(ChangeEntry.KEY_AUTHOR, "testuser")).count();
    assertEquals(1, change5);
    int changeAll = fakeDb.getCollection(CHANGELOG_COLLECTION).find(new BasicDBObject()
        .append(ChangeEntry.KEY_AUTHOR, "testuser")).count();
    assertEquals(11, changeAll);
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
  public void shouldUsePreConfiguredMongoTemplate () throws Exception {
    MongoTemplate mt = mock(MongoTemplate.class);
    when(mt.getCollectionNames()).thenReturn(Collections.EMPTY_SET);
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);
    runner.setMongoTemplate(mt);
    runner.afterPropertiesSet();
    verify(mt).getCollectionNames();
  }
  
  @Test
  public void shouldUsePreConfiguredJongo () throws Exception {
    Jongo jongo = mock(Jongo.class);
    when(jongo.getDatabase()).thenReturn(null);
    runner.setJongo(jongo);
    runner.afterPropertiesSet();
    verify(jongo).getDatabase();
  }
  
  @After
  public void cleanUp() {
    runner.setMongoTemplate(null);
    runner.setJongo(null);
  }
  
}
