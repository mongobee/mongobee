package com.github.mongobee.utils;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.github.mongobee.exception.MongobeeChangeSetException;
import com.github.mongobee.exception.MongobeeException;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * Class for loading change logs and change sets
 * 
 * @author christopher.ogrady
 *
 */
public class ChangeLogLoader {

  private final String changeLogsBasePackage;

  /**
   * Creates a ChangeLogLoader which searches the provided base package for
   * change log classes
   * 
   * @param changeLogsBasePackage
   */
  public ChangeLogLoader(String changeLogsBasePackage) {
    this.changeLogsBasePackage = changeLogsBasePackage;
  }

  /**
   * Fetches classes having the ChangeLog annotation from the base package.
   * Filters using the filterChangeLogClasses method. Then sorts and returns the
   * results.
   * 
   * @return List of change log classes
   */
  List<Class<?>> fetchChangeLogClasses() {
    Reflections reflections = new Reflections(changeLogsBasePackage);
    Set<Class<?>> changeLogSet = reflections.getTypesAnnotatedWith(ChangeLog.class);
    List<Class<?>> filteredChangeLogClassList = filterChangeLogClasses(changeLogSet);
    Collections.sort(filteredChangeLogClassList, new ChangeLogComparator());

    return filteredChangeLogClassList;
  }
  
  /**
   * Returns a list of filtered change log classes. On the ChangeLogLoader base
   * class this is a no-op.
   * 
   * @param changeLogClassSet
   *          {@link Set} of change log classes to be filtered
   * @return {@link List} of filtered change log classes
   */
  public List<Class<?>> filterChangeLogClasses(Set<Class<?>> changeLogClassSet) {
    return new ArrayList<>(changeLogClassSet);
  }

  /**
   * Fetches or instantiates an instance of the provided change log class
   * 
   * @param changeLogClass
   * @return
   * @throws MongobeeException
   */
  public Object fetchChangeLogInstance(Class<?> changeLogClass) throws MongobeeException {
    try {
      return changeLogClass.getConstructor().newInstance();
    } catch (InvocationTargetException e) {
      throw new MongobeeException(e.getTargetException().getMessage(), e);
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException
        | SecurityException e) {
      throw new MongobeeException(e.getMessage(), e);
    }
  }
  
  /**
   * Fetches methods having the ChangeSet annotation from the provided class.
   * Filters using the filterChangeSetMethods method. Then sorts and returns the
   * results.
   * 
   * @param type
   *          {@link Class} of the change log for which change sets are being
   *          fetched
   * @return List of change set methods
   */
  List<Method> fetchChangeSets(final Class<?> type) throws MongobeeChangeSetException {
    final List<Method> changeSets = filterChangeSetAnnotation(asList(type.getDeclaredMethods()));
    final List<Method> filteredChangeSets = filterChangeSetMethods(changeSets);
    Collections.sort(filteredChangeSets, new ChangeSetComparator());
    return filteredChangeSets;
  }

  /**
   * Returns a list of filtered change set methods. On the ChangeLogLoader base
   * class this is a no-op.
   * 
   * @param methods
   *          {@link Set} of change set methods to be filtered
   * @return {@link List} of filtered change set methods
   */
  public List<Method> filterChangeSetMethods(List<Method> methods) {
    return methods;
  }

  private List<Method> filterChangeSetAnnotation(List<Method> allMethods) throws MongobeeChangeSetException {
    final Set<String> changeSetIds = new HashSet<>();
    final List<Method> changesetMethods = new ArrayList<>();
    for (final Method method : allMethods) {
      if (method.isAnnotationPresent(ChangeSet.class)) {
        String id = method.getAnnotation(ChangeSet.class).id();
        if (changeSetIds.contains(id)) {
          throw new MongobeeChangeSetException(String.format("Duplicated changeset id found: '%s'", id));
        }
        changeSetIds.add(id);
        changesetMethods.add(method);
      }
    }
    return changesetMethods;
  }
  
}
