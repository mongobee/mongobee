package com.github.mongobee.utils;

import com.github.mongobee.changeset.ChangeLog;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Sort ChangeLogs by 'order' value or class name (if no 'order' is set)
 *
 * @author lstolowski
 * @since 2014-09-17
 */
public class ChangeLogComparator implements Comparator<Object>, Serializable {
  @Override
  public int compare(Object o1, Object o2) {
    Object[] objects = new Object[] {o1, o2};
    ChangeLog[] changeLogs = new ChangeLog[2];
    Class<?>[] classes = new Class<?>[2];

    for (int i = 0; i < 2; i++) {
      classes[i] = objects[i].getClass();
      // search class hierarchy for @ChangeLog--this allows for possible proxying of ChangeLog classes
      while (changeLogs[i] == null) {
        changeLogs[i] = classes[i].getAnnotation(ChangeLog.class);
        if (changeLogs[i] == null) {
          classes[i] = classes[i].getSuperclass();
          if (classes[i] == Object.class) {
            throw new IllegalArgumentException(
                String.format("Could not get ChangeLog annotation from class: %s", objects[i].getClass().getName()));
          }
        }
      }
    }

    String val1 = StringUtils.isEmpty(changeLogs[0].order())
        ? classes[0].getCanonicalName()
        : changeLogs[0].order();
    String val2 = StringUtils.isEmpty(changeLogs[1].order())
        ? classes[1].getCanonicalName()
        : changeLogs[1].order();

    if (val1 == null && val2 == null){
      return 0;
    } else if (val1 == null) {
      return -1;
    } else if (val2 == null) {
      return 1;
    }

    return val1.compareTo(val2);
  }
}
