package com.github.mongobee.lock;

import com.github.mongobee.exception.MongobeeException;

/**
 * @author dieppa
 * @since 04/04/2018
 */
class LockPersistenceException extends MongobeeException {

  private static final long serialVersionUID = -4232386506613422980L;

  LockPersistenceException(String msg) {
    super(msg);
  }

}
