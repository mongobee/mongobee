package com.github.mongobee;

import com.github.fakemongo.Fongo;
import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.dao.ChangeEntryDao;
import com.github.mongobee.dao.ChangeEntryIndexDao;
import com.github.mongobee.lock.LockChecker;
import com.github.mongobee.lock.LockRepository;
import com.github.mongobee.test.changelogs.MongobeeTestResource;
import com.github.mongobee.utils.ChangeService;
import com.github.mongobee.utils.proxy.ProxyFactory;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.jongo.Jongo;
import org.junit.After;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.lang.reflect.Field;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Class to provide common configuration for Mongobee**Test
 *
 * @author dieppa
 * @since 04/04/2018
 */
public class MongobeeTestBase {

  static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";

  @InjectMocks
  protected Mongobee runner = spy(new Mongobee());

  protected Jongo jongo;
  protected DB fakeDb;
  protected MongoDatabase fakeMongoDatabase;

  @Mock
  protected ChangeEntryDao dao;

  @Mock
  protected LockChecker lockChecker;

  @Mock
  protected LockRepository lockRepository;

  @Mock
  protected ProxyFactory proxyFactory;

  @Mock
  private ChangeEntryIndexDao indexDao;

  @Spy
  protected ChangeService changeService;

  public static MongoClient getFakeMongoClient(MongoDatabase fakeMongoDatabase, DB fakeDb) {
    MongoClient mongoClient = mock(MongoClient.class);
    when(mongoClient.getDatabase(anyString())).thenReturn(fakeMongoDatabase);
    when(mongoClient.getDB(anyString())).thenReturn(fakeDb);
    return mongoClient;
  }

  @Before
  public void init() throws Exception {
    fakeDb = new Fongo("testServer").getDB("mongobeetest");
    fakeMongoDatabase = new Fongo("testServer").getDatabase("mongobeetest");
    jongo = new Jongo(fakeDb);
    when(dao.getDb()).thenReturn(fakeDb);
    when(dao.getMongoDatabase()).thenReturn(fakeMongoDatabase);

    doCallRealMethod().when(dao).save(any(ChangeEntry.class));
    doCallRealMethod().when(dao).setChangelogCollectionName(anyString());
    doCallRealMethod().when(dao).setIndexDao(any(ChangeEntryIndexDao.class));
    dao.setIndexDao(indexDao);
    dao.setChangelogCollectionName(CHANGELOG_COLLECTION_NAME);

    setJongoField(jongo);
    when(proxyFactory.createProxyFromOriginal(jongo)).thenReturn(jongo);
    when(proxyFactory.createProxyFromOriginal(fakeMongoDatabase)).thenReturn(fakeMongoDatabase);
    when(proxyFactory.createProxyFromOriginal(fakeDb)).thenReturn(fakeDb);

    doReturn(MongobeeTestBase.getFakeMongoClient(fakeMongoDatabase, fakeDb))
        .when(runner).getMongoClient();

    runner.setDbName("mongobeetest");
    runner.setEnabled(true);
    runner.setChangeLogsScanPackage(MongobeeTestResource.class.getPackage().getName());
  }

  protected void setJongoField(Jongo jongInstance) throws NoSuchFieldException, IllegalAccessException {
    Field f = runner.getClass().getSuperclass().getDeclaredField("jongo");
    f.setAccessible(true);
    f.set(runner, jongInstance);
    f.setAccessible(false);
  }

  @After
  public void cleanUp() {
    runner.setMongoTemplate(null);
    runner.setJongo(null);
    fakeDb.dropDatabase();
  }

}
