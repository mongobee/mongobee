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
  public static final String CHANGELOG_COLLECTION = "dbchangelog";

  private String changeId;
  private String author;
  private Date timestamp;
  private String changelogClass;
  private String changesetMethodName;

  public ChangeEntry(String changeId, String author, Date timestamp, String changelogClass, String changesetMethodName) {
    this.changeId = changeId;
    this.author = author;
    this.timestamp = new Date(timestamp.getTime());
    this.changelogClass = changelogClass;
    this.changesetMethodName = changesetMethodName;
  }

  public DBObject buildFullDBObject(){
    BasicDBObject entry = new BasicDBObject();
    
    entry.append("changeId", this.changeId)
            .append("author", this.author)
            .append("timestamp", this.timestamp)
            .append("changelogClass", this.changelogClass)
            .append("changesetMethod", this.changesetMethodName);
    
    return entry;
  }
  
  public DBObject buildSearchQueryDBObject(){
    return new BasicDBObject()
                  .append("changeId", this.changeId)
                  .append("author", this.author);
  }

  @Override
  public String toString() {
    return "[Changeset: id=" + this.changeId +
            ", author=" + this.author +
            ", changelogClass=" + this.changelogClass +
            ", changesetMethod=" + this.changesetMethodName + "]";
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

  public String getChangelogClass() {
    return this.changelogClass;
  }

  public String getChangesetMethodName() {
    return this.changesetMethodName;
  }
}
