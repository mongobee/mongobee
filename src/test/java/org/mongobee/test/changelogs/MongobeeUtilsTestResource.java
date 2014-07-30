package org.mongobee.test.changelogs;

import com.mongodb.DB;
import org.jongo.Jongo;
import org.mongobee.changeset.Changelog;
import org.mongobee.changeset.Changeset;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
@Changelog
public class MongobeeUtilsTestResource {
  
  @Changeset(author = "testuser", id = "test1")
  public void testChangeset(){

    System.out.println("invoked 1");
    
  }
  @Changeset(author = "testuser", id = "test2")
  public void testChangeset2(){

    System.out.println("invoked 2");
    
  }

  @Changeset(author = "testuser", id = "test3")
  public void testChangeset3(DB db){

    System.out.println("invoked 3 with db=" + db.toString());

  }

  @Changeset(author = "testuser", id = "test4")
  public void testChangeset4(Jongo jongo){

      System.out.println("invoked 4 with jongo=" + jongo.toString());

  }
}
