package com.github.mongobee.utils;

import com.github.mongobee.exception.MongobeeChangeSetException;
import com.github.mongobee.test.changelogs.*;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static junit.framework.Assert.assertTrue;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeLogLoaderTest {

  @Test
  public void shouldFindChangeLogClasses(){
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeLogLoader changeLogLoader = new ChangeLogLoader(scanPackage);
    // when
    List<Class<?>> foundClasses = changeLogLoader.fetchChangeLogClasses();
    // then
    assertTrue(foundClasses != null && foundClasses.size() > 0);
  }
  
  @Test
  public void shouldFindChangeSetMethods() throws MongobeeChangeSetException {
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeLogLoader changeLogLoader = new ChangeLogLoader(scanPackage);

    // when
    List<Method> foundMethods = changeLogLoader.fetchChangeSets(MongobeeTestResource.class);
    
    // then
    assertTrue(foundMethods != null && foundMethods.size() == 5);
  }

  @Test
  public void shouldFindAnotherChangeSetMethods() throws MongobeeChangeSetException {
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeLogLoader changeLogLoader = new ChangeLogLoader(scanPackage);

    // when
    List<Method> foundMethods = changeLogLoader.fetchChangeSets(AnotherMongobeeTestResource.class);

    // then
    assertTrue(foundMethods != null && foundMethods.size() == 6);
  }

  @Test(expected = MongobeeChangeSetException.class)
  public void shouldFailOnDuplicatedChangeSets() throws MongobeeChangeSetException {
    String scanPackage = ChangeLogWithDuplicate.class.getPackage().getName();
    ChangeLogLoader changeLogLoader = new ChangeLogLoader(scanPackage);
    changeLogLoader.fetchChangeSets(ChangeLogWithDuplicate.class);
  }

}
