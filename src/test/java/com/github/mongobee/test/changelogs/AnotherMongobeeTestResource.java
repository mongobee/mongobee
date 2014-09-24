package com.github.mongobee.test.changelogs;

import com.mongodb.DB;
import org.jongo.Jongo;
import com.github.mongobee.changeset.Changelog;
import com.github.mongobee.changeset.Changeset;
import org.jongo.MongoCollection;

/**
 * @author lstolowski
 * @since 30.07.14
 */
@Changelog(order = "2")
public class AnotherMongobeeTestResource {

  @Changeset(author = "testuser", id = "Btest1", order = "01")
  public void testChangeset(){
    System.out.println("invoked B1");
  }
  @Changeset(author = "testuser", id = "Btest2", order = "02")
  public void testChangeset2(){
    System.out.println("invoked B2");
  }

  @Changeset(author = "testuser", id = "Btest3", order = "03")
  public void testChangeset3(DB db){
    System.out.println("invoked B3 with db=" + db.toString());
  }

  @Changeset(author = "testuser", id = "Btest4", order = "04")
  public void testChangeset4(Jongo jongo){
    System.out.println("invoked B4 with jongo=" + jongo.toString());
  }

  @Changeset(author = "testuser", id = "Btest5", order = "05", runAlways = true)
  public void testChangesetAlways(Jongo jongo){
    System.out.println("invoked B5 with always + jongo=" + jongo.getDatabase());
  }
}
