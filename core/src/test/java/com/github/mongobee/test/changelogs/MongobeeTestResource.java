package com.github.mongobee.test.changelogs;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.mongodb.DB;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
@ChangeLog(order = "1")
public class MongobeeTestResource {
  
  @ChangeSet(author = "testuser", id = "test1", order = "01")
  public void testChangeSet(){

    System.out.println("invoked 1");
    
  }
  @ChangeSet(author = "testuser", id = "test2", order = "02")
  public void testChangeSet2(){

    System.out.println("invoked 2");
    
  }

  @ChangeSet(author = "testuser", id = "test3", order = "03")
  public void testChangeSet3(DB db){

    System.out.println("invoked 3 with db=" + db.toString());

  }
}
