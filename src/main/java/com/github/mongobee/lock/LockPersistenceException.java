package com.github.mongobee.lock;

import com.github.mongobee.exception.MongobeeException;

/**
 * @author dieppa
 * @since 04/04/2018
 */
class LockPersistenceException extends MongobeeException {

  LockPersistenceException(String msg) {
    super(msg);
  }

}
