package com.github.mongobee.test.changelogs;

import com.github.mongobee.changeset.ChangeLog;
import com.mongodb.DB;
import org.jongo.Jongo;
import com.github.mongobee.changeset.ChangeSet;
import com.mongodb.client.MongoDatabase;

/**
 * @author lstolowski
 * @since 30.07.14
 */
@ChangeLog(order = "2")
public class AnotherMongobeeTestResource {

  @ChangeSet(author = "testuser", id = "Btest1", order = "01")
  public void testChangeSet(){
    System.out.println("invoked B1");
  }
  @ChangeSet(author = "testuser", id = "Btest2", order = "02")
  public void testChangeSet2(){
    System.out.println("invoked B2");
  }

  @ChangeSet(author = "testuser", id = "Btest3", order = "03")
  public void testChangeSet3(DB db){
    System.out.println("invoked B3 with db=" + db.toString());
  }

  @ChangeSet(author = "testuser", id = "Btest4", order = "04")
  public void testChangeSet4(Jongo jongo){
    System.out.println("invoked B4 with jongo=" + jongo.toString());
  }

  @ChangeSet(author = "testuser", id = "Btest5", order = "05", runAlways = true)
  public void testChangeSetWithAlways(Jongo jongo){
    System.out.println("invoked B5 with always + jongo=" + jongo.getDatabase());
  }

  @ChangeSet(author = "testuser", id = "Btest6", order = "06")
  public void testChangeSet6(MongoDatabase mongoDatabase) {
    System.out.println("invoked B6 with db=" + mongoDatabase.toString());
  }

}
