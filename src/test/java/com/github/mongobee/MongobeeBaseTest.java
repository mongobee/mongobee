package com.github.mongobee;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.github.fakemongo.Fongo;
import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.dao.ChangeEntryDao;
import com.github.mongobee.dao.ChangeEntryIndexDao;
import com.github.mongobee.test.changelogs.MongobeeTestResource;
import com.mongodb.DB;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;

public class MongobeeBaseTest {
  protected static final int CHANGELOG_COUNT = 14;
	protected static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";

	@InjectMocks
	protected Mongobee runner = new Mongobee();

	@Mock
	protected ChangeEntryDao dao;
  @Mock
	private ChangeEntryIndexDao indexDao;

  protected DB fakeDb;
  protected MongoDatabase fakeMongoDatabase;

	@Before
	public void init() throws Exception {
		fakeDb = new Fongo("testServer").getDB("mongobeetest");
		fakeMongoDatabase = new Fongo("testServer").getDatabase("mongobeetest");

		when(dao.connectMongoDb(any(MongoClientURI.class), anyString())).thenReturn(fakeMongoDatabase);
		when(dao.getDb()).thenReturn(fakeDb);
		when(dao.getMongoDatabase()).thenReturn(fakeMongoDatabase);
		when(dao.acquireProcessLock()).thenReturn(true);

		doCallRealMethod().when(dao).save(any(ChangeEntry.class));
		doCallRealMethod().when(dao).setChangelogCollectionName(anyString());
		doCallRealMethod().when(dao).setIndexDao(any(ChangeEntryIndexDao.class));

		dao.setIndexDao(indexDao);
		dao.setChangelogCollectionName(CHANGELOG_COLLECTION_NAME);

    runner.setChangeLogsScanPackage(MongobeeTestResource.class.getPackage().getName());
		runner.setDbName("mongobeetest");
		runner.setEnabled(true);
	}

  @After
  public void cleanUp() {
    runner.setMongoTemplate(null);
    runner.setJongo(null);
    runner.setSpringApplicationContext(null);
    fakeDb.dropDatabase();
  }
}
