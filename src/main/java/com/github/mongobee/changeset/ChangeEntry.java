package com.github.mongobee.changeset;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.util.Date;

/**
 * Entry in the changes collection log {@link ChangeEntry#CHANGELOG_COLLECTION}
 * Type: entity class.
 * @author lstolowski
 * @since 27/07/2014
 */
public class ChangeEntry {
  public static final String CHANGELOG_COLLECTION = "dbchangelog"; // ! Don't change due to backward compatibility issue

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

  public DBObject buildFullDBObject(){
    BasicDBObject entry = new BasicDBObject();
    
    entry.append("changeId", this.changeId)
            .append("author", this.author)
            .append("timestamp", this.timestamp)
            .append("changeLogClass", this.changeLogClass)
            .append("changeSetMethod", this.changeSetMethodName);
    
    return entry;
  }
  
  public DBObject buildSearchQueryDBObject(){
    return new BasicDBObject()
                  .append("changeId", this.changeId)
                  .append("author", this.author);
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
