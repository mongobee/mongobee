package com.github.mongobee.utils;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.changeset.ChangeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.io.File;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import static java.util.Arrays.asList;

/**
 * Utilities to deal with reflections and annotations
 *
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeService {
  private static final Logger logger = LoggerFactory.getLogger(ChangeService.class);

  private static final String DEFAULT_PROFILE = "default";

  private final String changeLogsBasePackage;
  private final List<String> activeProfiles;

  public ChangeService(String changeLogsBasePackage) {
    this(changeLogsBasePackage, null);
  }

  public ChangeService(String changeLogsBasePackage, Environment environment) {
    this.changeLogsBasePackage = changeLogsBasePackage;

    if (environment != null && environment.getActiveProfiles() != null && environment.getActiveProfiles().length> 0) {
      this.activeProfiles = asList(environment.getActiveProfiles());
    } else {
      this.activeProfiles = asList(DEFAULT_PROFILE);
    }
  }

  public List<Class<?>> fetchChangeLogs(){
    Set<Class<?>> changeLogs = getClassesInPackage(changeLogsBasePackage);
    List<Class<?>> filteredChangeLogs = (List<Class<?>>) filterByActiveProfiles(changeLogs);

    Collections.sort(filteredChangeLogs, new ChangeLogComparator());

    return filteredChangeLogs;
  }

  public List<Method> fetchChangeSets(final Class<?> type) {
    final List<Method> changeSets = filterChangeSetAnnotation(asList(type.getDeclaredMethods()));
    final List<Method> filteredChangeSets = (List<Method>) filterByActiveProfiles(changeSets);

    Collections.sort(filteredChangeSets, new ChangeSetComparator());

    return filteredChangeSets;
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

  private boolean matchesActiveSpringProfile(AnnotatedElement element) {
    if (element.isAnnotationPresent(Profile.class)) {
      Profile profiles = element.getAnnotation(Profile.class);
      List<String> values = asList(profiles.value());
      return ListUtils.intersection(activeProfiles, values).size() > 0 ? true : false;

    } else {
      return true; // no-profiled changeset always matches
    }
  }

  private List<?> filterByActiveProfiles(Collection<? extends AnnotatedElement> annotated) {
    List<AnnotatedElement> filtered = new ArrayList<>();
    for (AnnotatedElement element : annotated) {
      if (matchesActiveSpringProfile(element)){
        filtered.add( element);
      }
    }
    return filtered;
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

  /**
   * find all classes in a package
   */
  private static Set<Class<?>> getClassesInPackage(String packageName) {
    Set<Class<?>> classes = new HashSet();
    String packageNameSlashed = packageName.replace(".", "/");
    // get a file object for the package
    URL directoryURL = Thread.currentThread().getContextClassLoader().getResource(packageNameSlashed);
    if (directoryURL == null) {
      logger.warn("Could not retrieve URL resource: " + packageNameSlashed);
      return classes;
    }

    String directoryString = directoryURL.getFile();
    if (directoryString == null) {
      logger.warn("Could not find directory for URL resource: " + packageNameSlashed);
      return classes;
    }

    File directory = new File(directoryString);
    if (directory.exists()) {
      // Get the list of the files contained in the package
      String[] files = directory.list();
      for (String fileName : files) {
        // We are only interested in .class files
        if (fileName.endsWith(".class") || fileName.endsWith(".java")) {
          // Remove the .class extension
          fileName = fileName.substring(0, fileName.length() - 6);
          try {
            classes.add(Class.forName(packageName + "." + fileName));
          } catch (ClassNotFoundException e) {
            logger.warn(packageName + "." + fileName + " does not appear to be a valid class.", e);
          }
        }
      }
    } else {
      logger.warn(packageName + " does not appear to exist as a valid package on the file system.");
    }
    return classes;
  }

}
