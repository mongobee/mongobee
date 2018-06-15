package com.github.mongobee.utils;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.changeset.ChangeSet;
import com.github.mongobee.exception.MongobeeChangeSetException;
import com.github.mongobee.exception.MongobeeException;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

/**
 * Utilities to deal with reflections and annotations
 *
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeService {

  private final ChangeLogLoader changeLogLoader;

  /**
   * Constructs a ChangeService which uses the parameters passed in.
   * 
   * @param changeLogsBasePackage
   * @deprecated Use the constructor taking in a ChangeLogLoader moving forward
   */
  public ChangeService(String changeLogsBasePackage) {
    this(changeLogsBasePackage, null);
  }

  /**
   * Constructs a ChangeService which uses the parameters passed in.
   * 
   * @param changeLogsBasePackage
   * @param environment
   * @deprecated Use the constructor taking in a ChangeLogLoader moving forward
   */
  public ChangeService(String changeLogsBasePackage, Environment environment) {
    this.changeLogLoader = new SpringProfileChangeLogLoader(changeLogsBasePackage, environment);
  }

  public ChangeService(ChangeLogLoader changeLogLoader) {
    this.changeLogLoader = changeLogLoader;
  }

  public List<Class<?>> fetchChangeLogs(){
    return this.changeLogLoader.fetchChangeLogClasses();
  }

  public List<Method> fetchChangeSets(final Class<?> type) throws MongobeeChangeSetException {
    return this.changeLogLoader.fetchChangeSets(type);
  }

  public Object fetchChangeLogInstance(final Class<?> changeLogClass) throws MongobeeException {
    return this.changeLogLoader.fetchChangeLogInstance(changeLogClass);
  }

  public boolean isRunAlwaysChangeSet(Method changesetMethod){
    if (changesetMethod.isAnnotationPresent(ChangeSet.class)){
      ChangeSet annotation = changesetMethod.getAnnotation(ChangeSet.class);
      return annotation.runAlways();
    } else {
      return false;
    }
  }

  public ChangeEntry createChangeEntry(Method changesetMethod){
    if (changesetMethod.isAnnotationPresent(ChangeSet.class)){
      ChangeSet annotation = changesetMethod.getAnnotation(ChangeSet.class);
  
      return new ChangeEntry(
          annotation.id(),
          annotation.author(),
          new Date(),
          changesetMethod.getDeclaringClass().getName(),
          changesetMethod.getName());
    } else {
      return null;
    }
  }
}
