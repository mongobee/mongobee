package com.github.mongobee.test.changelogs;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeLog(order = "4")
public class ApplicationContextDependentTestResource {
  @ChangeSet(author = "testuser", id = "Appcontexttest1", order = "01")
  public void testChangeSet8WithApplicationContext(MongoTemplate template, ApplicationContext applicationContext) {
    System.out.println("invoked Appcontexttest1 with mongotemplate=" + template.toString() + " and ApplicationContext " + applicationContext);
  }
}
