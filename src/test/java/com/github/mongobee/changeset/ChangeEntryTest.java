package com.github.mongobee.changeset;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.junit.Test;

import java.util.Date;

import static com.github.mongobee.changeset.ChangeEntry.KEY_AUTHOR;
import static com.github.mongobee.changeset.ChangeEntry.KEY_CHANGEID;
import static com.github.mongobee.changeset.ChangeEntry.KEY_CHANGELOGCLASS;
import static org.junit.Assert.*;

public class ChangeEntryTest {
    @Test
    public void shouldReturnTheCorrectlyPopulatedSearchObject() {
        ChangeEntry changeEntry = new ChangeEntry("changedId", "author", new Date(), "changeLogClass", "changeSetMethod");
        BasicDBObject expectedSearchQuery = new BasicDBObject().append(KEY_CHANGEID, changeEntry.getChangeId())
                                                               .append(KEY_AUTHOR, changeEntry.getAuthor())
                                                               .append(KEY_CHANGELOGCLASS, changeEntry.getChangeLogClass());

        DBObject searchQuery = changeEntry.buildSearchQueryDBObject();

        assertEquals(searchQuery, expectedSearchQuery);
    }
}
