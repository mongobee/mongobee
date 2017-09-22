package com.github.mongobee.core.exception;

/**
 * @author abelski
 */
public class MongobeeException extends Exception {
  public MongobeeException(String message) {
    super(message);
  }

  public MongobeeException(String message, Throwable cause) {
    super(message, cause);
  }
}
