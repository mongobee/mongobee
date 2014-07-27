package org.monjeez;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.monjeez.test.changelogs.MonjeezUtilsTestResource;

@Ignore
public class MonjeezTest {
  
  private Monjeez runner;
  
  @Before
  public void initRunner(){
    runner = new Monjeez();
    
    runner.setDbName("monjeeztest");
    runner.setHost("localhost");
    runner.setEnabled(true);
    runner.setChangelogsBasePackage(MonjeezUtilsTestResource.class.getPackage().getName());
    
  }
  
  @Test
  public void executeSample() throws Exception {
    
    runner.execute();
    
  }

}
