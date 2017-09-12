package com.github.mongobee.spring;

import com.github.fakemongo.Fongo;
import com.github.mongobee.core.changeset.ChangeEntry;
import com.github.mongobee.core.dao.ChangeEntryDao;
import com.github.mongobee.core.dao.ChangeEntryIndexDao;
import com.github.mongobee.core.test.changelogs.MongobeeTestResource;
import com.github.mongobee.spring.test.changelogs.SpringDataChangelog;
import com.mongodb.DB;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.github.mongobee.spring.SpringMongobee;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author j-coll
 */
@RunWith(MockitoJUnitRunner.class)
public class SpringMongobeeTest {

  protected static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";
  @InjectMocks
  protected SpringMongobee runner = new SpringMongobee();

  @Mock
  protected ChangeEntryDao dao;

  @Mock
  protected ChangeEntryIndexDao indexDao;

  protected DB fakeDb;
  protected MongoDatabase fakeMongoDatabase;

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
    runner.setChangeLogsScanPackage(SpringDataChangelog.class.getPackage().getName());
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


  @After
  public void cleanUp() {
    fakeDb.dropDatabase();
    runner.setMongoTemplate(null);
  }

}
