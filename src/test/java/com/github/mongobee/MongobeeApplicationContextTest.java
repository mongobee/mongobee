package com.github.mongobee;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.init.MongobeeTest;
import com.github.mongobee.resources.ApplicationContextMock;
import com.github.mongobee.test.changelogs.ApplicationContextDependentTestResource;
import org.bson.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MongobeeApplicationContextTest extends MongobeeTest {

  @Test
  public void shouldRunChangesetWithApplicationContext() throws Exception {
    // given
    runner.setSpringApplicationContext(new ApplicationContextMock());
    runner.setChangeLogsScanPackage(ApplicationContextDependentTestResource.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    long change1 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .count(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Appcontexttest1")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change1);
  }

  @Test
  public void shouldRunChangesetWithNullApplicationContext() throws Exception {
    // given
    runner.setSpringApplicationContext(null);
    runner.setChangeLogsScanPackage(ApplicationContextDependentTestResource.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    long change1 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .count(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Appcontexttest1")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change1);
  }

}
