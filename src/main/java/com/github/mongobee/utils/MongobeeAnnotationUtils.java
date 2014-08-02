package com.github.mongobee.utils;

import org.apache.commons.lang3.StringUtils;
import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.changeset.Changelog;
import com.github.mongobee.changeset.Changeset;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Utilities to deal with reflections and annotations
 * @author lstolowski
 * @since 27/07/2014
 */
public class MongobeeAnnotationUtils {
  
  public static List<Class<?>> fetchChangelogsAt(String changelogsBasePackage){
    Reflections reflections = new Reflections(changelogsBasePackage);
    Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Changelog.class);

    List<Class<?>> annotatedSorted = new ArrayList<>(annotated);

    Collections.sort(annotatedSorted, new Comparator<Class<?>>() {
      @Override
      public int compare(Class<?> o1, Class<?> o2) {
        Changelog c1 = o1.getAnnotation(Changelog.class);
        Changelog c2 = o2.getAnnotation(Changelog.class);

        String val1 = StringUtils.isEmpty(c1.order()) ? o1.getCanonicalName() : c1.order();
        String val2 = StringUtils.isEmpty(c2.order()) ? o2.getCanonicalName() : c2.order();

        return val1.compareTo(val2);
      }
    });

    return annotatedSorted;
  }

  public static List<Method> fetchChangesetsAt(final Class<?> type) {
    final List<Method> methods = new ArrayList<>();
      final List<Method> allMethods = new ArrayList<>(Arrays.asList(type.getDeclaredMethods()));
      for (final Method method : allMethods) {
        if (method.isAnnotationPresent(Changeset.class)) {
          methods.add(method);
        }
      }

    Collections.sort(methods, new Comparator<Method>() {
      @Override
      public int compare(Method o1, Method o2) {
        Changeset c1 = o1.getAnnotation(Changeset.class);
        Changeset c2 = o2.getAnnotation(Changeset.class);
        return c1.order().compareTo(c2.order());
      }
    });

    return methods;
  }
  
  
  public static ChangeEntry createChangeEntryFor(Method changesetMethod){
    if (changesetMethod.isAnnotationPresent(Changeset.class)){
      Changeset annotation = changesetMethod.getAnnotation(Changeset.class);
  
      return new ChangeEntry(
                      annotation.id(), 
                      annotation.author(), 
                      new Date(), 
                      changesetMethod.getDeclaringClass().getName(), 
                      changesetMethod.getName()
                  );
      
    } else {
      return null;
    }
  }
  
}
