package org.github.mongobee.jongo.test.changelogs;

import com.github.mongobee.core.changeset.ChangeLog;
import com.github.mongobee.core.changeset.ChangeSet;
import org.jongo.Jongo;

/**
 * @author j-coll
 */
@ChangeLog(order = "1")
public class JongoChangelog {

  @ChangeSet(author = "testuser", id = "Jtest1", order = "01")
  public void testChangeSet1() {
    System.out.println("invoked J1");
  }

  @ChangeSet(author = "testuser", id = "Jtest2", order = "02")
  public void testChangeSet4(Jongo jongo){
    System.out.println("invoked J2 with jongo=" + jongo.toString());
  }

  @ChangeSet(author = "testuser", id = "Jtest3", order = "03", runAlways = true)
  public void testChangeSetWithAlways(Jongo jongo){
    System.out.println("invoked J3 with always + jongo=" + jongo.getDatabase());
  }

}
