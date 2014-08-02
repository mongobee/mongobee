package com.github.mongobee.utils;

import junit.framework.Assert;
import org.junit.Test;
import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.test.changelogs.MongobeeTestResource;

import java.lang.reflect.Method;
import java.util.List;

import static com.github.mongobee.utils.MongobeeAnnotationUtils.fetchChangelogsAt;
import static com.github.mongobee.utils.MongobeeAnnotationUtils.fetchChangesetsAt;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class MongobeeAnnotationUtilsTest {
  
  @Test
  public void shouldFindChangelogClasses(){
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    // when
    List<Class<?>> foundClasses = fetchChangelogsAt(scanPackage);
    // then
    Assert.assertTrue(foundClasses != null && foundClasses.size() > 0);
    
  }
  
  @Test
  public void shouldFindChangesetMethods(){

    // when
    List<Method> foundMethods = fetchChangesetsAt(MongobeeTestResource.class);
    
    // then
    Assert.assertTrue(foundMethods != null && foundMethods.size() == 4);
    
  }
  
  @Test
  public void shouldCreateEntry(){
    
    // given
    List<Method> foundMethods = fetchChangesetsAt(MongobeeTestResource.class);

    for (Method foundMethod : foundMethods) {
    
      // when
      ChangeEntry entry = MongobeeAnnotationUtils.createChangeEntryFor(foundMethod);
      
      // then
      Assert.assertEquals("testuser", entry.getAuthor());
      Assert.assertEquals(MongobeeTestResource.class.getName(), entry.getChangelogClass());
      Assert.assertNotNull(entry.getTimestamp());
      Assert.assertNotNull(entry.getChangeId());
      Assert.assertNotNull(entry.getChangesetMethodName());
    }
  }

}
