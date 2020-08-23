package com.github.mongobee;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.dao.ChangeEntryDao;
import com.github.mongobee.dao.ChangeEntryIndexDao;
import com.github.mongobee.test.changelogs.MongobeeTestResource;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import java.net.InetSocketAddress;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public abstract class AbstractTest {

  protected static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";

  @Mock ChangeEntryDao dao;

  @Mock ChangeEntryIndexDao indexDao;

  @InjectMocks protected Mongobee runner = new Mongobee();

  protected MongoDatabase fakeMongoDatabase;

  private MongoClient mongoClient;

  private MongoServer server;

  @Before
  public void init() throws Exception {
    server = new MongoServer(new MemoryBackend());
    InetSocketAddress serverAddress = server.bind();

    mongoClient =
        MongoClients.create(
            MongoClientSettings.builder()
                .applyToClusterSettings(
                    builder -> builder.hosts(Arrays.asList(new ServerAddress(serverAddress))))
                .build());

    fakeMongoDatabase = mongoClient.getDatabase("mongobeetest");

    when(dao.connectMongoDb(any(MongoClientURI.class), anyString())).thenReturn(fakeMongoDatabase);
    when(dao.getMongoDatabase()).thenReturn(fakeMongoDatabase);
    when(dao.acquireProcessLock()).thenReturn(true);
    doCallRealMethod().when(dao).save(any(ChangeEntry.class));
    doCallRealMethod().when(dao).setChangelogCollectionName(anyString());
    doCallRealMethod().when(dao).setIndexDao(any(ChangeEntryIndexDao.class));
    dao.setIndexDao(indexDao);
    dao.setChangelogCollectionName(CHANGELOG_COLLECTION_NAME);

    runner.setDbName("mongobeetest");
    runner.setMongoClient(mongoClient);
    runner.setChangeLogsScanPackage(MongobeeTestResource.class.getPackage().getName());
    runner.setEnabled(true);
  }

  @After
  public void cleanUp() {
    runner.setMongoTemplate(null);
    mongoClient.close();
    server.shutdownNow();
  }
}
