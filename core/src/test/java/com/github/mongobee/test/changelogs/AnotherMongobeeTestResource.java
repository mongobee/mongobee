package com.github.mongobee.test.changelogs;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.mongodb.DB;

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
}
