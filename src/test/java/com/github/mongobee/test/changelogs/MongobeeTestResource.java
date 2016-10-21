package com.github.mongobee.test.changelogs;

import org.jongo.Jongo;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.mongodb.DB;
import com.mongodb.client.MongoDatabase;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
@ChangeLog(order = "1")
public class MongobeeTestResource {

  @ChangeSet(author = "testuser", id = "test1", order = "01")
  public void testChangeSet() {

    System.out.println("invoked 1");

  }

  @ChangeSet(author = "testuser", id = "test2", order = "02")
  public void testChangeSet2() {

    System.out.println("invoked 2");

  }

  @ChangeSet(author = "testuser", id = "test3", order = "03")
  public void testChangeSet3(DB db) {

    System.out.println("invoked 3 with db=" + db.toString());

  }

  @ChangeSet(author = "testuser", id = "test4", order = "04")
  public void testChangeSet4(Jongo jongo) {

    System.out.println("invoked 4 with jongo=" + jongo.toString());

  }

  @ChangeSet(author = "testuser", id = "test5", order = "05")
  public void testChangeSet5(MongoDatabase mongoDatabase) {

    System.out.println("invoked 5 with mongoDatabase=" + mongoDatabase.toString());

  }

}
