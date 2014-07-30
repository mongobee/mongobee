package org.mongobee;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mongobee.test.changelogs.MongobeeUtilsTestResource;

@Ignore // TODO take Fongo to mock db
public class MongobeeTest {
  
  private Mongobee runner;
  
  @Before
  public void initRunner(){
    runner = new Mongobee();
    
    runner.setDbName("mongobeetest");
    runner.setHost("localhost");
    runner.setEnabled(true);
    runner.setChangelogsBasePackage(MongobeeUtilsTestResource.class.getPackage().getName());
    
  }
  
  @Test
  public void executeSample() throws Exception {
    runner.execute();
  }

}
