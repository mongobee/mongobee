package com.github.mongobee.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lstolowski
 * @since 2014-09-16
 */
public class ListUtils {

  public static <T> List<T> intersection(List<T> listA, List<T> listB) {
    List<T> intersection = new ArrayList<>();
    for (T elem : listA) {
      if(listB.contains(elem)) {
        intersection.add(elem);
      }
    }
    return intersection;
  }


}
