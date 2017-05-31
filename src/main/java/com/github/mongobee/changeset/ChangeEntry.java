package com.github.mongobee.changeset;

import java.util.Date;

import org.bson.Document;

/**
 * Entry in the changes collection log {@link com.github.mongobee.Mongobee#DEFAULT_CHANGELOG_COLLECTION_NAME}
 * Type: entity class.
 *
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeEntry {
  public static final String KEY_CHANGEID = "changeId";
  public static final String KEY_AUTHOR = "author";
  public static final String KEY_TIMESTAMP = "timestamp";
  public static final String KEY_CHANGELOGCLASS = "changeLogClass";
  public static final String KEY_CHANGESETMETHOD = "changeSetMethod";

  private String changeId;
  private String author;
  private Date timestamp;
  private String changeLogClass;
  private String changeSetMethodName;
  
  public ChangeEntry(String changeId, String author, Date timestamp, String changeLogClass, String changeSetMethodName) {
    this.changeId = changeId;
    this.author = author;
    this.timestamp = new Date(timestamp.getTime());
    this.changeLogClass = changeLogClass;
    this.changeSetMethodName = changeSetMethodName;
  }

  public Document buildFullDBObject() {
    Document entry = new Document();

    entry.append(KEY_CHANGEID, this.changeId)
        .append(KEY_AUTHOR, this.author)
        .append(KEY_TIMESTAMP, this.timestamp)
        .append(KEY_CHANGELOGCLASS, this.changeLogClass)
        .append(KEY_CHANGESETMETHOD, this.changeSetMethodName);

    return entry;
  }

  public Document buildSearchQueryDBObject() {
    return new Document()
        .append(KEY_CHANGEID, this.changeId)
        .append(KEY_AUTHOR, this.author);
  }

  @Override
  public String toString() {
    return "[ChangeSet: id=" + this.changeId +
        ", author=" + this.author +
        ", changeLogClass=" + this.changeLogClass +
        ", changeSetMethod=" + this.changeSetMethodName + "]";
  }

  public String getChangeId() {
    return this.changeId;
  }

  public String getAuthor() {
    return this.author;
  }

  public Date getTimestamp() {
    return this.timestamp;
  }

  public String getChangeLogClass() {
    return this.changeLogClass;
  }

  public String getChangeSetMethodName() {
    return this.changeSetMethodName;
  }
  
}
