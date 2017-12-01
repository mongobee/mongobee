package com.github.mongobee.exception;

/**
 * Error while connection to MongoDB
 *
 * @author lstolowski
 * @since 27/07/2014
 */
public class MongobeeConnectionException extends MongobeeException {

  public MongobeeConnectionException(String message) {
    super(message);
  }

  public MongobeeConnectionException(String message, Exception baseException) {
    super(message, baseException);
  }
}
