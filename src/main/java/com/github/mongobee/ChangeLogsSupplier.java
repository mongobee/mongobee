package com.github.mongobee;

import com.github.mongobee.exception.MongobeeException;

import java.util.Collection;

/**
 * An interface to be implemented by any supplier of change logs.
 */
public interface ChangeLogsSupplier {

  /**
   * Returns a collection of objects where {@link com.github.mongobee.changeset.ChangeLog}
   * is present on or inherited by the object's type. The collection need not be sorted.
   * Any necessary filtering of objects must be performed by this method, such as discarding
   * change log classes whose Spring profiles are not active in a Spring environment.
   */
  Collection<?> get() throws MongobeeException;
}
