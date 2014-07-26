package org.monjeez;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class MongoAuth {

  private String username;
  private String password;
  private String dbName;

  /**
   * Credentials for authentication <br/>
   * User should be defined in the same database as defined in Monjeez runner
   * @param username the user's name
   * @param password the user's password
   */
  public MongoAuth(String username, String password) {
    this.username = username;
    this.password = password;
  }

  /**
   * Credentials for authentication
   * @param username the user's name
   * @param password the user's password
   * @param dbName  the database where the user is defined
   */
  public MongoAuth(String username, String password, String dbName) {
    this.username = username;
    this.password = password;
    this.dbName = dbName;
  }

  String getUsername() {
    return username;
  }

  String getPassword() {
    return password;
  }

  String getDbName() {
    return dbName;
  }
}
