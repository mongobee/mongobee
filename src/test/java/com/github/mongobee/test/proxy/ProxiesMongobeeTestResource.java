package com.github.mongobee.test.proxy;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.client.MongoDatabase;
import org.jongo.Jongo;

/**
 * @author dieppa
 * @since 04/04/2018
 */
@ChangeLog(order = "5")
public class ProxiesMongobeeTestResource {

  @ChangeSet(author = "testuser", id = "proxyDbTest", order = "01")
  public void testInsertWithDB(DB db) throws InterruptedException {
    DBCollection coll = db.getCollection("anyCollection");

    coll.insert(new BasicDBObject("value", "value1"));
    coll.insert(new BasicDBObject("value", "value2"));
  }

  @ChangeSet(author = "testuser", id = "proxyJongTest", order = "02")
  public void testJongo(Jongo jongo) {
    System.out.println("invoked proxyJongTest with jongo=" + jongo.toString());
  }

  @ChangeSet(author = "testuser", id = "ProxyMongoDatabaseTest", order = "03")
  public void testMongoDatabase(MongoDatabase mongoDatabase) {
    System.out.println("invoked ProxyMongoDatabaseTest with db=" + mongoDatabase.toString());
  }

}
