package com.github.mongobee.dao;

import com.mongodb.MongoClientSettings;
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

public abstract class AbstractDaoTest {

  protected MongoDatabase mongoDatabase;
  protected MongoClient mongoClient;
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

    mongoDatabase = mongoClient.getDatabase("mongobeetest");
  }

  @After
  public void cleanUp() {
    mongoClient.close();
    server.shutdownNow();
  }
}
