package com.github.mongobee.utils;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.*;

import static java.util.Arrays.asList;

/**
 * Utilities to deal with reflections and annotations
 *
 * @author lstolowski
 * @since 27/07/2014
 */
public class CoreChangeService implements ChangeService {
  private static final String DEFAULT_PROFILE = "default";

  private final String changeLogsBasePackage;
  private final List<String> activeProfiles;

  public CoreChangeService(String changeLogsBasePackage) {
    this.changeLogsBasePackage = changeLogsBasePackage;
    this.activeProfiles = asList(DEFAULT_PROFILE);
  }

  public List<Class<?>> fetchChangeLogs(){
    Reflections reflections = new Reflections(changeLogsBasePackage);
    List<Class<?>> changeLogs = new ArrayList<>(reflections.getTypesAnnotatedWith(ChangeLog.class)); // TODO remove dependency, do own method

    Collections.sort(changeLogs, new ChangeLogComparator());

    return changeLogs;
  }

  @Override
  public List<Method> fetchChangeSets(final Class<?> type) {
    final List<Method> changeSets = filterChangeSetAnnotation(asList(type.getDeclaredMethods()));

    Collections.sort(changeSets, new ChangeSetComparator());

    return changeSets;
  }

  @Override
  public boolean isRunAlwaysChangeSet(Method changesetMethod){
    if (changesetMethod.isAnnotationPresent(ChangeSet.class)){
      ChangeSet annotation = changesetMethod.getAnnotation(ChangeSet.class);
      return annotation.runAlways();
    } else {
      return false;
    }
  }

  @Override
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


  private List<Method> filterChangeSetAnnotation(List<Method> allMethods) {
    final List<Method> changesetMethods = new ArrayList<>();
    for (final Method method : allMethods) {
      if (method.isAnnotationPresent(ChangeSet.class)) {
        changesetMethods.add(method);
      }
    }
    return changesetMethods;
  }

}
