![mongobee](https://raw.githubusercontent.com/mongobee/mongobee/master/misc/mongobee_min.png)

[![Build Status](https://travis-ci.org/mongobee/mongobee.svg?branch=master)](https://travis-ci.org/mongobee/mongobee) [![Coverity Scan Build Status](https://scan.coverity.com/projects/2721/badge.svg)](https://scan.coverity.com/projects/2721) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.mongobee/mongobee/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.mongobee/mongobee) [![Licence](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/mongobee/mongobee/blob/master/LICENSE)
---


**mongobee** is a Java tool which helps you to *manage changes* in your MongoDB and *synchronize* them with your application.
The concept is very similar to other db migration tools such as [Liquibase](http://www.liquibase.org) or [Flyway](http://flywaydb.org) but *without using XML/JSON/YML files*.

The goal is to keep this tool simple and comfortable to use.


**mongobee** provides new approach for adding changes (change sets) based on Java classes and methods with appropriate annotations.

## Getting started

### Add a dependency

With Maven
```xml
<dependency>
  <groupId>com.github.mongobee</groupId>
  <artifactId>mongobee</artifactId>
  <version>0.13</version>
</dependency>
```
With Gradle
```groovy
compile 'org.javassist:javassist:3.18.2-GA' // workaround for ${javassist.version} placeholder issue*
compile 'com.github.mongobee:mongobee:0.13'
```

### Usage with Spring

You need to instantiate Mongobee object and provide some configuration.
If you use Spring can be instantiated as a singleton bean in the Spring context. 
In this case the migration process will be executed automatically on startup.

```java
@Bean
public Mongobee mongobee(){
  Mongobee runner = new Mongobee("mongodb://YOUR_DB_HOST:27017/DB_NAME");
  runner.setDbName("yourDbName");         // host must be set if not set in URI
  runner.setChangeLogsScanPackage(
       "com.example.yourapp.changelogs"); // the package to be scanned for changesets
  
  return runner;
}
```


### Usage without Spring
Using mongobee without a spring context has similar configuration but you have to remember to run `execute()` method to start a migration process.

```java
Mongobee runner = new Mongobee("mongodb://YOUR_DB_HOST:27017/DB_NAME");
runner.setDbName("yourDbName");         // host must be set if not set in URI
runner.setChangeLogsScanPackage(
     "com.example.yourapp.changelogs"); // package to scan for changesets

runner.execute();         //  ------> starts migration changesets
```

Above examples provide minimal configuration. `Mongobee` object provides some other possibilities (setters) to make the tool more flexible:

```java
runner.setChangelogCollectionName(logColName);   // default is dbchangelog, collection with applied change sets
runner.setLockCollectionName(lockColName);       // default is mongobeelock, collection used during migration process
runner.setEnabled(shouldBeEnabled);              // default is true, migration won't start if set to false
```

MongoDB URI format:
```
mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database[.collection]][?options]]
```
[More about URI](http://mongodb.github.io/mongo-java-driver/3.5/javadoc/)


### Creating change logs

`ChangeLog` contains bunch of `ChangeSet`s. `ChangeSet` is a single task (set of instructions made on a database). In other words `ChangeLog` is a class annotated with `@ChangeLog` and containing methods annotated with `@ChangeSet`.

```java 
package com.example.yourapp.changelogs;

@ChangeLog
public class DatabaseChangelog {
  
  @ChangeSet(order = "001", id = "someChangeId", author = "testAuthor")
  public void importantWorkToDo(DB db){
     // task implementation
  }


}
```
#### @ChangeLog

Class with change sets must be annotated by `@ChangeLog`. There can be more than one change log class but in that case `order` argument should be provided:

```java
@ChangeLog(order = "001")
public class DatabaseChangelog {
  //...
}
```
ChangeLogs are sorted alphabetically by `order` argument and changesets are applied due to this order.

#### @ChangeSet

Method annotated by @ChangeSet is taken and applied to the database. History of applied change sets is stored in a collection called `dbchangelog` (by default) in your MongoDB

##### Annotation parameters:

`order` - string for sorting change sets in one changelog. Sorting in alphabetical order, ascending. It can be a number, a date etc.

`id` - name of a change set, **must be unique** for all change logs in a database

`author` - author of a change set

`runAlways` - _[optional, default: false]_ changeset will always be executed but only first execution event will be stored in dbchangelog collection

##### Defining ChangeSet methods
Method annotated by `@ChangeSet` can have one of the following definition:

```java
@ChangeSet(order = "001", id = "someChangeWithoutArgs", author = "testAuthor")
public void someChange1() {
   // method without arguments can do some non-db changes
}

@ChangeSet(order = "002", id = "someChangeWithMongoDatabase", author = "testAuthor")
public void someChange2(MongoDatabase db) {
  // type: com.mongodb.client.MongoDatabase : original MongoDB driver v. 3.x, operations allowed by driver are possible
  // example: 
  MongoCollection<Document> mycollection = db.getCollection("mycollection");
  Document doc = new Document("testName", "example").append("test", "1");
  mycollection.insertOne(doc);
}

@ChangeSet(order = "003", id = "someChangeWithDb", author = "testAuthor")
public void someChange3(DB db) {
  // This is deprecated in mongo-java-driver 3.x, use MongoDatabase instead
  // type: com.mongodb.DB : original MongoDB driver v. 2.x, operations allowed by driver are possible
  // example: 
  DBCollection mycollection = db.getCollection("mycollection");
  BasicDBObject doc = new BasicDBObject().append("test", "1");
  mycollection .insert(doc);
}

@ChangeSet(order = "004", id = "someChangeWithJongo", author = "testAuthor")
public void someChange4(Jongo jongo) {
  // type: org.jongo.Jongo : Jongo driver can be used, used for simpler notation
  // example:
  MongoCollection mycollection = jongo.getCollection("mycollection");
  mycollection.insert("{test : 1}");
}

@ChangeSet(order = "005", id = "someChangeWithSpringDataTemplate", author = "testAuthor")
public void someChange5(MongoTemplate mongoTemplate) {
  // type: org.springframework.data.mongodb.core.MongoTemplate
  // Spring Data integration allows using MongoTemplate in the ChangeSet
  // example:
  mongoTemplate.save(myEntity);
}

@ChangeSet(order = "006", id = "someChangeWithSpringDataTemplate", author = "testAuthor")
public void someChange5(MongoTemplate mongoTemplate, Environment environment) {
  // type: org.springframework.data.mongodb.core.MongoTemplate
  // type: org.springframework.core.env.Environment
  // Spring Data integration allows using MongoTemplate and Environment in the ChangeSet
}
```

### Using Spring profiles
     
**mongobee** accepts Spring's `org.springframework.context.annotation.Profile` annotation. If a change log or change set class is annotated  with `@Profile`, 
then it is activated for current application profiles.

_Example 1_: annotated change set will be invoked for a `dev` profile
```java
@Profile("dev")
@ChangeSet(author = "testuser", id = "myDevChangest", order = "01")
public void devEnvOnly(DB db){
  // ...
}
```
_Example 2_: all change sets in a changelog will be invoked for a `test` profile
```java
@ChangeLog(order = "1")
@Profile("test")
public class ChangelogForTestEnv{
  @ChangeSet(author = "testuser", id = "myTestChangest", order = "01")
  public void testingEnvOnly(DB db){
    // ...
  } 
}
```

#### Enabling @Profile annotation (option)
      
To enable the `@Profile` integration, please inject `org.springframework.core.env.Environment` to you runner.

```java      
@Bean @Autowired
public Mongobee mongobee(Environment environment) {
  Mongobee runner = new Mongobee(uri);
  runner.setSpringEnvironment(environment)
  //... etc
}
```

#### Configuring Lock 
In order to execute the changelogs, Mongobee needs to manage the lock to ensure only one instance executes a changelog at a time.
By default the lock is reserved 24 hours and, in case the lock is held by another Mongobee instance, will ignore the execution
and no exception will be sent, unless the parameter throwExceptionIfCannotObtainLock is set to true.

There are 3 parameters to configure:

`lockAcquiredForMinutes` - Number of minutes Mongobee will acquire the lock for. It will refresh the lock when is close to be expired anyway. 

`maxTries` - Max tries when the lock is held by another Mongobee instance.

`maxWaitingForLockMinutes` - Max minutes Mongobee will wait for the lock in every try. 

To configure these parameters there are two methods: setLockConfig and `setLockConfig` and `setLockQuickConfig`. Both will set the
parameter throwExceptionIfCannotObtainLock to true.
 ```java      
 @Bean @Autowired
 public Mongobee mongobee(Environment environment) {
   Mongobee runner = new Mongobee(uri);
   runner.setLockConfig(5, 6, 3);
 }
 ```
 or quick config with 3 minutes for lockAcquiredFor, 3 max tries and 4 minutes for maxWaitingForLock
 ```java      
  @Bean @Autowired
  public Mongobee mongobee(Environment environment) {
    Mongobee runner = new Mongobee(uri);
    runner.setLockQuickConfig();
  }
  ```
 
 

## Known issues

##### Mongo java driver conflicts

**mongobee** depends on `mongo-java-driver`. If your application has mongo-java-driver dependency too, there could be a library conflicts in some cases.

**Exception**:
```
com.mongodb.WriteConcernException: { "serverUsed" : "localhost" , 
"err" : "invalid ns to index" , "code" : 10096 , "n" : 0 , 
"connectionId" : 955 , "ok" : 1.0}
```

**Workaround**:

You can exclude mongo-java-driver from **mongobee**  and use your dependency only. Maven example (pom.xml) below:
```xml
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongo-java-driver</artifactId>
    <version>3.0.0</version>
</dependency>

<dependency>
  <groupId>com.github.mongobee</groupId>
  <artifactId>mongobee</artifactId>
  <version>0.9</version>
  <exclusions>
    <exclusion>
      <groupId>org.mongodb</groupId>
      <artifactId>mongo-java-driver</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```


##### Mongo transaction limitations

Due to Mongo limitations, there is no way to provide atomicity at ChangelogSet level. So a Changelog could need 
more than one execution to be finished, as any interruption could happen, leaving the changelog in a inconsistent state.
If that happen, the next time Mongobee is executed will try to finish the changelog execution, but it could already be half executed.

For this reason, the developer in charge of the changelog's design, should make sure that:
 
- **Changelog is dempotent**: As changelog can be interrupted at any time, it will need to be executed again. 
- **Changelog is Backward compatible(If high availability is required)**: While the migration process is taking place, the old version of the software is still running. During this time could happen(and probably will) that the old version of the software is dealing with the new version of the data. Could even happen that the data is a mix between old and new version. So the software must still work regardless the status of the database. In case the developer is aware of this and still decides to provide a non-backward-compatible changeSet, he should know it's a detriment to the high availability.
- **Changelog reduces its execution time in every iteration**: This is more hard to explain. As said a changelog can be interrupted at any time. That means that an specific changelog need to be re-run. In the undesired scenario where the changelog's execution time is grater than the interruption time(culd be Kubernetes initial delay), that changelog won't be ever finished. So the changelog needs to be developed in such a way that every iteration reduces the execution time, so eventually that changelog will finish. 
- **Changeog's execution time is shorter than interruption time**: In case the previous condition cannot be ensured, could be enough if the changelog's execution time is shorter than the interruption time. This is not ideal as the execution time depends on the machine, but in most case could be enough.
