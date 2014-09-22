package com.github.mongobee.utils;

import com.github.mongobee.changeset.Changeset;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * Sort changesets by 'order' value
 *
 * @author lstolowski
 * @since 2014-09-17
 */
public class ChangesetComparator implements Comparator<Method>, Serializable {
  @Override
  public int compare(Method o1, Method o2) {
    Changeset c1 = o1.getAnnotation(Changeset.class);
    Changeset c2 = o2.getAnnotation(Changeset.class);
    return c1.order().compareTo(c2.order());
  }
}
