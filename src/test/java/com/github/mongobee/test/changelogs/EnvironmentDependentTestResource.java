package com.github.mongobee.test.changelogs;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeLog(order = "3")
public class EnvironmentDependentTestResource {
  @ChangeSet(author = "testuser", id = "Envtest1", order = "01")
  public void testChangeSet7WithEnvironment(MongoTemplate template, Environment env) {
    System.out.println("invoked Envtest1 with mongotemplate=" + template.toString() + " and Environment " + env);
  }
}
