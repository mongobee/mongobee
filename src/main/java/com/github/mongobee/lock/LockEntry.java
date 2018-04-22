package com.github.mongobee.lock;

import org.bson.Document;

import java.util.Date;

/**
 * Entry in the changes collection log {@link com.github.mongobee.Mongobee#DEFAULT_CHANGELOG_COLLECTION_NAME}
 * Type: entity class.
 *
 * @author lstolowski
 * @since 27/07/2014
 */
public class LockEntry {

  static final String KEY_FIELD = "key";
  static final String STATUS_FIELD = "status";
  static final String OWNER_FIELD = "owner";
  static final String EXPIRES_AT_FIELD = "expiresAt";

  private final String key;
  private final String status;
  private final String owner;
  private final Date expiresAt;

  LockEntry(String key, String status, String owner, Date expiresAt) {
    this.key = key;
    this.status = status;
    this.owner = owner;
    this.expiresAt = expiresAt;
  }

  Document buildFullDBObject() {
    Document entry = new Document();
    entry.append(KEY_FIELD, this.key)
        .append(STATUS_FIELD, this.status)
        .append(OWNER_FIELD, this.owner)
        .append(EXPIRES_AT_FIELD, this.expiresAt);
    return entry;
  }

  /**
   * @return lock's key
   */
  String getKey() {
    return key;
  }

  /**
   * @return lock's status
   */
  String getStatus() {
    return status;
  }

  /**
   * @return lock's owner
   */
  String getOwner() {
    return owner;
  }

  /**
   * @return lock's expiration time
   * @see Date
   */
  Date getExpiresAt() {
    return expiresAt;
  }

  boolean isOwner(String owner) {
    return this.owner.equals(owner);
  }

  @Override
  public String toString() {
    return "LockEntry{" +
        "key='" + key + '\'' +
        ", status='" + status + '\'' +
        ", owner='" + owner + '\'' +
        ", expiresAt=" + expiresAt.toString() +
        '}';
  }
}
