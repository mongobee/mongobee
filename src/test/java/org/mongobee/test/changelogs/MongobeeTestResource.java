package org.mongobee.test.changelogs;

import com.mongodb.DB;
import org.jongo.Jongo;
import org.mongobee.changeset.Changelog;
import org.mongobee.changeset.Changeset;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
@Changelog(order = "1")
public class MongobeeTestResource {
  
  @Changeset(author = "testuser", id = "test1", order = "01")
  public void testChangeset(){

    System.out.println("invoked 1");
    
  }
  @Changeset(author = "testuser", id = "test2", order = "02")
  public void testChangeset2(){

    System.out.println("invoked 2");
    
  }

  @Changeset(author = "testuser", id = "test3", order = "03")
  public void testChangeset3(DB db){

    System.out.println("invoked 3 with db=" + db.toString());

  }

  @Changeset(author = "testuser", id = "test4", order = "04")
  public void testChangeset4(Jongo jongo){

      System.out.println("invoked 4 with jongo=" + jongo.toString());

  }
}
