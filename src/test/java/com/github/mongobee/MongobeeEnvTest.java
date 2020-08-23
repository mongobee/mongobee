package com.github.mongobee;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.resources.EnvironmentMock;
import com.github.mongobee.test.changelogs.EnvironmentDependentTestResource;
import org.bson.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/** Created by lstolowski on 13.07.2017. */
@RunWith(MockitoJUnitRunner.class)
public class MongobeeEnvTest extends AbstractTest {

  @Test
  public void shouldRunChangesetWithEnvironment() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock());
    runner.setChangeLogsScanPackage(EnvironmentDependentTestResource.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    long change1 =
        fakeMongoDatabase
            .getCollection(CHANGELOG_COLLECTION_NAME)
            .countDocuments(
                new Document()
                    .append(ChangeEntry.KEY_CHANGEID, "Envtest1")
                    .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change1);
  }

  @Test
  public void shouldRunChangesetWithNullEnvironment() throws Exception {
    // given
    runner.setSpringEnvironment(null);
    runner.setChangeLogsScanPackage(EnvironmentDependentTestResource.class.getPackage().getName());
    when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

    // when
    runner.execute();

    // then
    long change1 =
        fakeMongoDatabase
            .getCollection(CHANGELOG_COLLECTION_NAME)
            .count(
                new Document()
                    .append(ChangeEntry.KEY_CHANGEID, "Envtest1")
                    .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change1);
  }
}
