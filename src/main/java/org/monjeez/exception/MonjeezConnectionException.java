package org.monjeez.exception;

/**
 * Error while connection to MongoDB
 * @author lstolowski
 * @since 27/07/2014
 */
public class MonjeezConnectionException extends RuntimeException {
  public MonjeezConnectionException(String message, Exception baseException) {
    super(message, baseException);
  }
}
