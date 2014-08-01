package org.mongobee;

import com.mongodb.MongoClientURI;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mongobee.exception.MongobeeConfigurationException;
import org.mongobee.test.changelogs.MongobeeTestResource;

public class MongobeeTest {
  
  private Mongobee runner;
  
  @Test(expected = MongobeeConfigurationException.class)
  public void shouldNThrowAnExceptionIfNoDbNameSet() throws Exception{
    MongoClientURI uri = new MongoClientURI("mongodb://localhost:27017/");
    runner = new Mongobee();
    runner.setEnabled(true);
    runner.setChangelogsScanPackage(MongobeeTestResource.class.getPackage().getName());
    runner.execute();
  }

  @Ignore @Test // TODO take Fongo to mock db
  public void shouldExecuteSample() throws Exception {
    // given
    runner = new Mongobee();
    runner.setDbName("mongobeetest");
    runner.setEnabled(true);
    runner.setChangelogsScanPackage(MongobeeTestResource.class.getPackage().getName());

    // when
    runner.execute();
  }

}
