package com.github.mongobee.test.changelogs;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import org.jongo.Jongo;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * @author abelski
 */
@ChangeLog
public class SpringDataChangelog {
  @ChangeSet(author = "abelski", id = "spring_test4", order = "04")
  public void testChangeSet(MongoTemplate mongoTemplate) {
    System.out.println("invoked  with mongoTemplate=" + mongoTemplate.toString());
  }
}
