package org.mongobee;

import com.github.fakemongo.Fongo;
import com.mongodb.DB;
import com.mongodb.FongoDB;
import com.mongodb.MongoClientURI;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mongobee.changeset.ChangeEntry;
import org.mongobee.dao.ChangeEntryDao;
import org.mongobee.exception.MongobeeConfigurationException;
import org.mongobee.test.changelogs.MongobeeTestResource;

import java.net.UnknownHostException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
@RunWith(MockitoJUnitRunner.class)
public class MongobeeTest {

  @InjectMocks
  private Mongobee runner = new Mongobee();

  @Mock
  private ChangeEntryDao dao;

  @Before
  public void init() throws UnknownHostException {
    DB fakeDb = new Fongo("testServer").getDB("mongobeetest");
    when(dao.connectMongoDb(any(MongoClientURI.class), anyString()))
      .thenReturn(fakeDb);

    runner.setDbName("mongobeetest");
    runner.setEnabled(true);
    runner.setChangelogsScanPackage(MongobeeTestResource.class.getPackage().getName());
  }


  @Test(expected = MongobeeConfigurationException.class)
  public void shouldThrowAnExceptionIfNoDbNameSet() throws Exception{
    Mongobee runner = new Mongobee(new MongoClientURI("mongodb://localhost:27017/"));
    runner.setEnabled(true);
    runner.setChangelogsScanPackage(MongobeeTestResource.class.getPackage().getName());
    runner.execute();
  }

  @Test
  public void shouldExecute8Changesets() throws Exception {
    // given
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    verify(dao, times(8)).save(any(ChangeEntry.class)); // 8 changesets saved to dbchangelog
  }

  @Test
  public void shouldPassOverChangesets() throws Exception {
    // given
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(false);

    // when
    runner.execute();

    // then
    verify(dao, times(0)).save(any(ChangeEntry.class)); // no changesets saved to dbchangelog
  }

}
