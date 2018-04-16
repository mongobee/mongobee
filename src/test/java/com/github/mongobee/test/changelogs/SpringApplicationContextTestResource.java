
package com.github.mongobee.test.changelogs;

import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;

@ChangeLog(order = "4")
public class SpringApplicationContextTestResource {
	@ChangeSet(author = "spring_user", id = "spring_app_context", order = "01")
	public void testChangeSet8WithApplicationContext(MongoTemplate template, ApplicationContext applicationContext) {
		System.out.println("Using Spring Application Context with Mongobee");
	}
}
