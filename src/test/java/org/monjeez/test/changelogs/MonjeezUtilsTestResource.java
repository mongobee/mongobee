package org.monjeez.test.changelogs;

import org.monjeez.changeset.Changelog;
import org.monjeez.changeset.Changeset;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
@Changelog
public class MonjeezUtilsTestResource {
  
  @Changeset(author = "lstolowski", id = "test1")
  public void testChangeset(){

    System.out.println("here");
    
  }
  @Changeset(author = "lstolowski", id = "test2")
  public void testChangeset2(){

    System.out.println("here2");
    
  }
  
}
