package com.github.mongobee;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.bson.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.support.StaticApplicationContext;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.exception.MongobeeException;
import com.github.mongobee.test.changelogs.SpringApplicationContextTestResource;

@RunWith(MockitoJUnitRunner.class)
public class MongobeeApplicationContextTest extends MongobeeBaseTest {

	@Test
	public void testRunWithApplicationContext() throws MongobeeException {
	  // given
    runner.setChangeLogsScanPackage(SpringApplicationContextTestResource.class.getPackage().getName());
		runner.setSpringApplicationContext(new StaticApplicationContext());
		when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

		// when
		runner.execute();

		// then
		long change1 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
		        .count(new Document().append(ChangeEntry.KEY_CHANGEID, "spring_app_context")
		                .append(ChangeEntry.KEY_AUTHOR, "spring_user"));
		assertEquals(1, change1);
	}

	@Test
	public void testRunWithoutApplicationContext() throws MongobeeException {
	  // given
    runner.setChangeLogsScanPackage(SpringApplicationContextTestResource.class.getPackage().getName());
		runner.setSpringApplicationContext(null);
		when(dao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

		// when
		runner.execute();

		// then
		long change1 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
		        .count(new Document().append(ChangeEntry.KEY_CHANGEID, "spring_app_context")
		                .append(ChangeEntry.KEY_AUTHOR, "spring_user"));
		assertEquals(1, change1);
	}
}
