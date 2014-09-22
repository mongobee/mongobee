package com.github.mongobee.utils;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.changeset.Changelog;
import com.github.mongobee.changeset.Changeset;
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

  private final String changelogsBasePackage;
  private final List<String> activeProfiles;

  public ChangeService(String changelogsBasePackage) {
    this(changelogsBasePackage, null);
  }

  public ChangeService(String changelogsBasePackage, Environment environment) {
    this.changelogsBasePackage = changelogsBasePackage;

    if (environment != null && environment.getActiveProfiles() != null && environment.getActiveProfiles().length> 0) {
      this.activeProfiles = asList(environment.getActiveProfiles());
    } else {
      this.activeProfiles = asList(DEFAULT_PROFILE);
    }
  }

  public List<Class<?>> fetchChangelogs(){
    Reflections reflections = new Reflections(changelogsBasePackage);
    Set<Class<?>> changelogs = reflections.getTypesAnnotatedWith(Changelog.class); // TODO remove dependency, do own method
    List<Class<?>> filteredChangelogs = (List<Class<?>>) filterByActiveProfiles(changelogs);

    Collections.sort(filteredChangelogs, new ChangelogComparator());

    return filteredChangelogs;
  }

  public List<Method> fetchChangesets(final Class<?> type) {
    final List<Method> changesets = filterChangesetAnnotation(asList(type.getDeclaredMethods()));
    final List<Method> filteredChangesets = (List<Method>) filterByActiveProfiles(changesets);

    Collections.sort(filteredChangesets, new ChangesetComparator());

    return filteredChangesets;
  }

  public boolean isRunAlwaysChangeset(Method changesetMethod){
    if (changesetMethod.isAnnotationPresent(Changeset.class)){
      Changeset annotation = changesetMethod.getAnnotation(Changeset.class);
      return annotation.runAlways();
    } else {
      return false;
    }
  }

  public ChangeEntry createChangeEntry(Method changesetMethod){
    if (changesetMethod.isAnnotationPresent(Changeset.class)){
      Changeset annotation = changesetMethod.getAnnotation(Changeset.class);
  
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

  private List<Method> filterChangesetAnnotation(List<Method> allMethods) {
    final List<Method> changesetMethods = new ArrayList<>();
    for (final Method method : allMethods) {
      if (method.isAnnotationPresent(Changeset.class)) {
        changesetMethods.add(method);
      }
    }
    return changesetMethods;
  }

}
