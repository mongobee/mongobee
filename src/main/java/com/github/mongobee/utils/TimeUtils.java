package com.github.mongobee.utils;

import java.util.Date;

/**
 * Class to manage time operation
 *
 * @author dieppa
 * @since 04/04/2018
 */
public class TimeUtils {

  /**
   * @param millis milliseconds to add to the Date
   * @return current date plus milliseconds passed as parameter
   */
  public Date currentTimePlusMillis(long millis) {
    return new Date(System.currentTimeMillis() + millis);
  }

  /**
   * @return current Date
   */
  public Date currentTime() {
    return new Date(System.currentTimeMillis());
  }

  /**
   * Converts minutes to milliseconds
   *
   * @param minutes minutes to be converted
   * @return equivalent to the minutes passed in milliseconds
   */
  public long minutesToMillis(long minutes) {
    return minutes * 60 * 1000;
  }

  /**
   * Converts minutes to milliseconds
   *
   * @param minutes minutes to be converted
   * @return equivalent to the minutes passed in milliseconds
   */
  public long millisToMinutes(long minutes) {
    return minutes / (60 * 1000);
  }

}
