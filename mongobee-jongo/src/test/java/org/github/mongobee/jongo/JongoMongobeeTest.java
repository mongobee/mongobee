package org.github.mongobee.jongo;

import com.github.fakemongo.Fongo;
import com.github.mongobee.core.changeset.ChangeEntry;
import com.github.mongobee.core.dao.ChangeEntryDao;
import com.github.mongobee.core.dao.ChangeEntryIndexDao;
import com.github.mongobee.core.exception.MongobeeException;
import com.mongodb.DB;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.github.mongobee.jongo.test.changelogs.JongoChangelog;
import org.jongo.Jongo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.UnknownHostException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author j-coll
 */
@RunWith(MockitoJUnitRunner.class)
public class JongoMongobeeTest {

  private static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";
  @InjectMocks
  private JongoMongobee runner = new JongoMongobee();

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
    runner.setChangeLogsScanPackage(JongoChangelog.class.getPackage().getName());
  }

  @Test
  public void shouldExecuteAllChangeSets() throws Exception {
    // given
    when(dao.acquireProcessLock()).thenReturn(true);
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    verify(dao, times(3)).save(any(ChangeEntry.class)); // 3 changesets saved to dbchangelog
  }

  @Test
  public void shouldUsePreConfiguredJongo() throws Exception {
    Jongo jongo = mock(Jongo.class);
    when(dao.acquireProcessLock()).thenReturn(true);
    when(jongo.getDatabase()).thenReturn(null);
    runner.setJongo(jongo);
    runner.execute();
    verify(jongo).getDatabase();
  }


  @After
  public void cleanUp() {
    runner.setJongo(null);
    fakeDb.dropDatabase();
  }

}
