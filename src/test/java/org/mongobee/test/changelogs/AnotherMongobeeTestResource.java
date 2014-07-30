package org.mongobee.test.changelogs;

import com.mongodb.DB;
import org.jongo.Jongo;
import org.mongobee.changeset.Changelog;
import org.mongobee.changeset.Changeset;

/**
 * Created with IntelliJ IDEA.
 * User: Lukasz
 * Date: 30.07.14
 * Time: 21:06
 * To change this template use File | Settings | File Templates.
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
}
