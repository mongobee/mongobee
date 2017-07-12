package com.github.mongobee.utils;

import com.github.mongobee.ChangeLogsSupplier;
import com.github.mongobee.PackageScanningChangeLogsSupplier;
import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.github.mongobee.exception.MongobeeChangeSetException;
import com.github.mongobee.exception.MongobeeException;
import org.reflections.Reflections;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.Arrays.asList;

/**
 * Utilities to deal with reflections and annotations
 *
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeService {

  private static final String DEFAULT_PROFILE = "default";
  private static final Comparator<Object> CHANGELOG_COMPARATOR = new ChangeLogComparator();
  private static final Comparator<Method> CHANGESET_COMPARATOR = new ChangeSetComparator();

  private final ChangeLogsSupplier changeLogsSupplier;
  private final List<String> activeProfiles;

  public ChangeService(String changeLogsBasePackage) {
    this(new PackageScanningChangeLogsSupplier(changeLogsBasePackage), null);
  }

  public ChangeService(ChangeLogsSupplier changeLogsSupplier) {
    this(changeLogsSupplier, null);
  }

  public ChangeService(ChangeLogsSupplier changeLogsSupplier, Environment environment) {
    this.changeLogsSupplier = changeLogsSupplier;

    if (environment != null && environment.getActiveProfiles() != null && environment.getActiveProfiles().length> 0) {
      this.activeProfiles = asList(environment.getActiveProfiles());
    } else {
      this.activeProfiles = asList(DEFAULT_PROFILE);
    }
  }

  public List<Object> fetchChangeLogs() throws MongobeeException {

    // changelogs in inactive Profiles will have been filtered out by the ChangeLogsSupplier

    List<Object> changeLogs = new ArrayList<>(changeLogsSupplier.get());
    Collections.sort(changeLogs, CHANGELOG_COMPARATOR);
    return changeLogs;
  }

  public List<Method> fetchChangeSets(final Class<?> type) throws MongobeeChangeSetException {

    // search class hierarchy for ChangeSet methods--this allows for possible proxying of ChangeLog classes
    List<Method> changeSets;
    Class<?> cls = type;
    do {
      changeSets = filterChangeSetAnnotation(asList(type.getDeclaredMethods()));
    } while (changeSets.isEmpty() && (cls = cls.getSuperclass()) != Object.class);

    final List<Method> filteredChangeSets = filterMethodsByActiveProfiles(changeSets);

    Collections.sort(filteredChangeSets, CHANGESET_COMPARATOR);

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

  private boolean matchesActiveSpringProfile(Method method) {

    if (method.isAnnotationPresent(Profile.class)) {
      List<String> profiles = asList(method.getAnnotation(Profile.class).value());
      for (String profile : profiles) {
        if (profile != null && profile.length() > 0 && profile.charAt(0) == '!') {
          if (!activeProfiles.contains(profile.substring(1))) {
            return true;
          }
        } else if (activeProfiles.contains(profile)) {
          return true;
        }
      }
      return false;
    } else {
      return true; // no-profiled changeset always matches
    }
  }

  private List<Method> filterMethodsByActiveProfiles(Collection<Method> methods) {
    List<Method> filtered = new ArrayList<>();
    for (Method m : methods) {
      if (matchesActiveSpringProfile(m)){
        filtered.add(m);
      }
    }
    return filtered;
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
