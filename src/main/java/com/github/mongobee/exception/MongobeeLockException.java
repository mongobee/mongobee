package com.github.mongobee.exception;

/**
 * Error while can not obtain process lock
 */
public class MongobeeLockException extends MongobeeException {
  private static final long serialVersionUID = -5564744042764984567L;

  public MongobeeLockException(String message) {
    super(message);
  }

  public MongobeeLockException(Exception ex) {
    super(ex);
  }
}
