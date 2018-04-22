package com.github.mongobee;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.dao.ChangeEntryDao;
import com.github.mongobee.exception.MongobeeChangeSetException;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.github.mongobee.exception.MongobeeConnectionException;
import com.github.mongobee.exception.MongobeeException;
import com.github.mongobee.exception.MongobeeLockException;
import com.github.mongobee.lock.LockChecker;
import com.github.mongobee.lock.LockRepository;
import com.github.mongobee.utils.ChangeService;
import com.github.mongobee.utils.TimeUtils;
import com.github.mongobee.utils.proxy.PreInterceptor;
import com.github.mongobee.utils.proxy.ProxyFactory;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.jongo.Jongo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mongodb.ServerAddress.defaultHost;
import static com.mongodb.ServerAddress.defaultPort;
import static org.springframework.util.StringUtils.hasText;

/**
 * Mongobee runner
 *
 * @author lstolowski
 * @since 26/07/2014
 */
public class Mongobee implements InitializingBean {
  private static final Logger logger = LoggerFactory.getLogger(Mongobee.class);

  private static final String DEFAULT_CHANGELOG_COLLECTION_NAME = "dbchangelog";
  private static final String DEFAULT_LOCK_COLLECTION_NAME = "mongobeelock";
  private static final long DEFAULT_LOCK_ACTIVE_MILLIS = 24 * 60 * 60 * 1000;//24 hours by default
  private static final long DEFAULT_LOCK_MAX_WAIT_MILLIS = 2 * 60 * 1000;//2 minutes by default
  private static final int DEFAULT_LOCK_MAX_TRIES = 1;//1 by default
  private static final Set<String>
      PROXY_CREATOR_METHODS =
      new HashSet<>(Arrays.asList("getCollection", "getCollectionFromString", "getDatabase", "toString"));
  private static final Set<String>
      UNCHECKED_PROXY_METHODS =
      new HashSet<>(Arrays.asList("getCollection", "getCollectionFromString", "getDatabase", "toString"));

  private TimeUtils timeUtils;
  private ProxyFactory proxyFactory;
  private ChangeEntryDao dao;
  private LockRepository lockRepository;
  private LockChecker lockChecker;
  private ChangeService service;

  private boolean enabled = true;
  private String changeLogsScanPackage;
  private MongoClientURI mongoClientURI;
  private String dbName;
  private Environment springEnvironment;
  private boolean throwExceptionIfCannotObtainLock = false;

  private MongoTemplate mongoTemplate;
  private Jongo jongo;
  private MongoClient mongoClient;

  //Proxies
  private MongoDatabase mongoDatabaseProxy;
  private DB dbProxy;
  private Jongo jongoProxy;
  private MongoTemplate mongoTemplateProxy;

  /**
   * <p>Simple constructor with default configuration of host (localhost) and port (27017). Although
   * <b>the database name need to be provided</b> using {@link Mongobee#setDbName(String)} setter.</p>
   * <p>It is recommended to use constructors with MongoURI</p>
   */
  public Mongobee() {
    this(new MongoClientURI("mongodb://" + defaultHost() + ":" + defaultPort() + "/"));
  }

  /**
   * <p>Constructor takes db.mongodb.MongoClientURI object as a parameter.
   * </p><p>For more details about MongoClientURI please see com.mongodb.MongoClientURI docs
   * </p>
   *
   * @param mongoClientURI uri to your db
   * @see MongoClientURI
   */
  public Mongobee(MongoClientURI mongoClientURI) {
    this.mongoClientURI = mongoClientURI;
    this.setDbName(mongoClientURI.getDatabase());
    initDependencies();
  }

  /**
   * <p>Constructor takes db.mongodb.MongoClient object as a parameter.
   * </p><p>For more details about <tt>MongoClient</tt> please see com.mongodb.MongoClient docs
   * </p>
   *
   * @param mongoClient database connection client
   * @see MongoClient
   */
  public Mongobee(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
    initDependencies();
  }

  //Ideally this beans are injected from outside
  private void initDependencies() {
    this.timeUtils = new TimeUtils();
    this.dao = new ChangeEntryDao(DEFAULT_CHANGELOG_COLLECTION_NAME, DEFAULT_LOCK_COLLECTION_NAME);
    this.lockRepository = new LockRepository(DEFAULT_LOCK_COLLECTION_NAME);
    this.lockChecker = new LockChecker(lockRepository, timeUtils)
        .setLockAcquiredForMillis(DEFAULT_LOCK_ACTIVE_MILLIS)
        .setLockMaxTries(DEFAULT_LOCK_MAX_TRIES)
        .setLockMaxWaitMillis(DEFAULT_LOCK_MAX_WAIT_MILLIS);
    PreInterceptor preInterceptor = new PreInterceptor() {
      @Override
      public void before() {
        try {
          lockChecker.ensureLockDefault();
        } catch (MongobeeLockException e) {
          throw new RuntimeException(e);
        }
      }
    };

    proxyFactory = new ProxyFactory(
        preInterceptor,
        PROXY_CREATOR_METHODS,
        UNCHECKED_PROXY_METHODS);

    service = new ChangeService();
  }

  /**
   * <p>Mongobee runner. Correct MongoDB URI should be provided.</p>
   * <p>The format of the URI is:
   * <pre>
   *   mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database[.collection]][?options]]
   * </pre>
   * <ul>
   * <li>{@code mongodb://} Required prefix</li>
   * <li>{@code username:password@} are optional.  If given, the driver will attempt to login to a database after
   * connecting to a database server. For some authentication mechanisms, only the username is specified and the password is not,
   * in which case the ":" after the username is left off as well.</li>
   * <li>{@code host1} Required.  It identifies a server address to connect to. More than one host can be provided.</li>
   * <li>{@code :portX} is optional and defaults to :27017 if not provided.</li>
   * <li>{@code /database} the name of the database to login to and thus is only relevant if the
   * {@code username:password@} syntax is used. If not specified the "admin" database will be used by default.
   * <b>Mongobee will operate on the database provided here or on the database overriden by setter setDbName(String).</b>
   * </li>
   * <li>{@code ?options} are connection options. For list of options please see com.mongodb.MongoClientURI docs</li>
   * </ul>
   * <p>For details, please see com.mongodb.MongoClientURI
   *
   * @param mongoURI with correct format
   * @see MongoClientURI
   */

  public Mongobee(String mongoURI) {
    this(new MongoClientURI(mongoURI));
    initDependencies();
  }

  /**
   * For Spring users: executing mongobee after bean is created in the Spring context
   *
   * @throws Exception exception
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    execute();
  }

  /**
   * Executing migration
   *
   * @throws MongobeeException exception
   */
  public void execute() throws MongobeeException {
    if (!isEnabled()) {
      logger.info("Mongobee is disabled. Exiting.");
      return;
    }

    validateConfig();
    initialize();

    try {
      lockChecker.acquireLockDefault();
      executeMigration();
    } catch (MongobeeLockException lockEx) {

      if (throwExceptionIfCannotObtainLock) {
        logger.error(lockEx.getMessage());
        throw new MongobeeException(lockEx.getMessage());
      }
      logger.warn(lockEx.getMessage());
      logger.warn("Mongobee did not acquire process lock. EXITING WITHOUT RUNNING DATA MIGRATION");

    } finally {
      lockChecker.releaseLockDefault();//we do it anyway, it's idempotent
      logger.info("Mongobee has finished his job.");
    }

  }

  private void executeMigration() throws MongobeeException {
    logger.info("Mongobee starting the data migration sequence..");

    for (Class<?> changelogClass : service.fetchChangeLogs()) {

      Object changelogInstance = null;
      try {
        changelogInstance = service.createInstance(changelogClass);
        List<Method> changesetMethods = service.fetchChangeSets(changelogInstance.getClass());

        for (Method changesetMethod : changesetMethods) {
          ChangeEntry changeEntry = service.createChangeEntry(changesetMethod);

          try {
            if (dao.isNewChange(changeEntry)) {
              executeChangeSetMethod(changesetMethod, changelogInstance);
              dao.save(changeEntry);
              logger.info(changeEntry + " applied");
            } else if (service.isRunAlwaysChangeSet(changesetMethod)) {
              executeChangeSetMethod(changesetMethod, changelogInstance);
              logger.info(changeEntry + " reapplied");
            } else {
              logger.info(changeEntry + " passed over");
            }
          } catch (MongobeeChangeSetException e) {
            logger.error(e.getMessage());
          }
        }
      } catch (NoSuchMethodException e) {
        throw new MongobeeException(e.getMessage(), e);
      } catch (IllegalAccessException e) {
        throw new MongobeeException(e.getMessage(), e);
      } catch (InvocationTargetException e) {
        Throwable targetException = e.getTargetException();
        throw new MongobeeException(targetException.getMessage(), e);
      } catch (InstantiationException e) {
        throw new MongobeeException(e.getMessage(), e);
      }

    }
  }

  private Object executeChangeSetMethod(Method changeSetMethod, Object changeLogInstance)
      throws IllegalAccessException, InvocationTargetException, MongobeeChangeSetException {
    if (changeSetMethod.getParameterTypes().length == 1
        && changeSetMethod.getParameterTypes()[0].equals(DB.class)) {
      logger.debug("method with DB argument");

      return changeSetMethod.invoke(changeLogInstance, dbProxy);
    } else if (changeSetMethod.getParameterTypes().length == 1
        && changeSetMethod.getParameterTypes()[0].equals(Jongo.class)) {
      logger.debug("method with Jongo argument");

      return changeSetMethod.invoke(changeLogInstance, jongoProxy);
    } else if (changeSetMethod.getParameterTypes().length == 1
        && changeSetMethod.getParameterTypes()[0].equals(MongoTemplate.class)) {
      logger.debug("method with MongoTemplate argument");

      return changeSetMethod.invoke(changeLogInstance, mongoTemplateProxy);
    } else if (changeSetMethod.getParameterTypes().length == 2
        && changeSetMethod.getParameterTypes()[0].equals(MongoTemplate.class)
        && changeSetMethod.getParameterTypes()[1].equals(Environment.class)) {
      logger.debug("method with MongoTemplate and environment arguments");

      return changeSetMethod.invoke(changeLogInstance, mongoTemplateProxy, springEnvironment);
    } else if (changeSetMethod.getParameterTypes().length == 1
        && changeSetMethod.getParameterTypes()[0].equals(MongoDatabase.class)) {
      logger.debug("method with DB argument");

      return changeSetMethod.invoke(changeLogInstance, this.mongoDatabaseProxy);
    } else if (changeSetMethod.getParameterTypes().length == 0) {
      logger.debug("method with no params");

      return changeSetMethod.invoke(changeLogInstance);
    } else {
      throw new MongobeeChangeSetException("ChangeSet method " + changeSetMethod.getName() +
          " has wrong arguments list. Please see docs for more info!");
    }
  }

  private void validateConfig() throws MongobeeConfigurationException {
    if (!hasText(dbName)) {
      throw new MongobeeConfigurationException("DB name is not set. It should be defined in MongoDB URI or via setter");
    }
    if (!hasText(changeLogsScanPackage)) {
      throw new MongobeeConfigurationException("Scan package for changelogs is not set: use appropriate setter");
    }
  }

  /**
   * @return true if an execution is in progress, in any process.
   * @throws MongobeeConnectionException exception
   */
  public boolean isExecutionInProgress() throws MongobeeConnectionException {
    return lockChecker.isLockHeld();
  }

  /**
   * Used DB name should be set here or via MongoDB URI (in a constructor)
   *
   * @param dbName database name
   * @return Mongobee object for fluent interface
   */
  public Mongobee setDbName(String dbName) {
    this.dbName = dbName;
    return this;
  }

  /**
   * Sets uri to MongoDB
   *
   * @param mongoClientURI object with defined mongo uri
   * @return Mongobee object for fluent interface
   */
  public Mongobee setMongoClientURI(MongoClientURI mongoClientURI) {
    this.mongoClientURI = mongoClientURI;
    return this;
  }

  /**
   * Package name where @ChangeLog-annotated classes are kept.
   *
   * @param changeLogsScanPackage package where your changelogs are
   * @return Mongobee object for fluent interface
   */
  public Mongobee setChangeLogsScanPackage(String changeLogsScanPackage) {
    this.changeLogsScanPackage = changeLogsScanPackage;
    return this;
  }

  /**
   * @return true if Mongobee runner is enabled and able to run, otherwise false
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Feature which enables/disables Mongobee runner execution
   *
   * @param enabled Mongobee will run only if this option is set to true
   * @return Mongobee object for fluent interface
   */
  public Mongobee setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * <p>Feature which enables/disables waiting for lock if it's already acquired by another process</p>
   * <p>It is disable if setLockMaxTries is called with a number different than 1</p>
   * <p>
   * <p>If waitForLock is false, will set maxTries to 1. If it's true and waitForLock is less than 2, it will be
   * set to 2</p>
   *
   * @param waitForLock Mongobee will wait for lock when it's already obtained, if this option is set to true.
   * @return Mongobee object for fluent interface
   * @deprecated use setLockConfig or setLockQuickConfig
   */
  @Deprecated
  public Mongobee setWaitForLock(boolean waitForLock) {
    if (!waitForLock) {
      this.lockChecker.setLockMaxTries(1);
    } else if (this.lockChecker.getLockMaxTries() <= 1) {
      this.lockChecker.setLockMaxTries(2);
    }
    return this;
  }

  /**
   * Waiting time for acquiring lock if maxTries is greater than 1
   *
   * @param changeLogLockWaitTime Waiting time in minutes for acquiring lock
   * @return Mongobee object for fluent interface
   * @deprecated use setLockConfig or setLockQuickConfig
   */
  @Deprecated
  public Mongobee setChangeLogLockWaitTime(long changeLogLockWaitTime) {
    this.lockChecker.setLockMaxWaitMillis(timeUtils.minutesToMillis(changeLogLockWaitTime));
    return this;
  }

  /**
   * Poll rate for acquiring lock if waitForLock is true
   *
   * @param changeLogLockPollRate Poll rate in seconds for acquiring lock
   * @return Mongobee object for fluent interface
   * @deprecated use setLockConfig or setLockQuickConfig
   */
  public Mongobee setChangeLogLockPollRate(long changeLogLockPollRate) {
    return this;
  }

  /**
   * Feature which enables/disables throwing MongobeeLockException if Mongobee can not obtain lock
   *
   * @param throwExceptionIfCannotObtainLock Mongobee will throw MongobeeLockException if lock can not be obtained
   * @return Mongobee object for fluent interface
   */
  public Mongobee setThrowExceptionIfCannotObtainLock(boolean throwExceptionIfCannotObtainLock) {
    this.throwExceptionIfCannotObtainLock = throwExceptionIfCannotObtainLock;
    return this;
  }

  /**
   * Set up the lock with minimal configuration. This implies Mongobee will throw an exception if cannot obtains the lock.
   *
   * @param lockAcquiredForMinutes   Acquired time in minutes
   * @param maxWaitingForLockMinutes max time in minutes to wait for the lock in each try.
   * @param maxTries                 number of tries
   * @return Mongobee object for fluent interface
   */
  public Mongobee setLockConfig(long lockAcquiredForMinutes, long maxWaitingForLockMinutes, int maxTries) {
    this.lockChecker
        .setLockAcquiredForMillis(timeUtils.minutesToMillis(lockAcquiredForMinutes))
        .setLockMaxWaitMillis(timeUtils.minutesToMillis(maxWaitingForLockMinutes))
        .setLockMaxTries(maxTries);
    this.setThrowExceptionIfCannotObtainLock(true);
    return this;
  }

  /**
   * Set up the lock with default configuration to wait for it and through an exception when cannot obtain it.
   *
   * @return Mongobee object for fluent interface
   */
  public Mongobee setLockQuickConfig() {
    setLockConfig(3, 4, 3);
    return this;
  }

  /**
   * Set Environment object for Spring Profiles (@Profile) integration
   *
   * @param environment org.springframework.core.env.Environment object to inject
   * @return Mongobee object for fluent interface
   */
  public Mongobee setSpringEnvironment(Environment environment) {
    this.springEnvironment = environment;
    return this;
  }

  /**
   * Sets pre-configured {@link MongoTemplate} instance to use by the Mongobee
   *
   * @param mongoTemplate instance of the {@link MongoTemplate}
   * @return Mongobee object for fluent interface
   */
  public Mongobee setMongoTemplate(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
    return this;
  }

  /**
   * Sets pre-configured {@link MongoTemplate} instance to use by the Mongobee
   *
   * @param jongo {@link Jongo} instance
   * @return Mongobee object for fluent interface
   */
  public Mongobee setJongo(Jongo jongo) {
    this.jongo = jongo;
    return this;
  }

  /**
   * Overwrites a default mongobee changelog collection hardcoded in DEFAULT_CHANGELOG_COLLECTION_NAME.
   * <p>
   * CAUTION! Use this method carefully - when changing the name on a existing system,
   * your changelogs will be executed again on your MongoDB instance
   *
   * @param changelogCollectionName a new changelog collection name
   * @return Mongobee object for fluent interface
   */
  public Mongobee setChangelogCollectionName(String changelogCollectionName) {
    this.dao.setChangelogCollectionName(changelogCollectionName);
    return this;
  }

  /**
   * Overwrites a default mongobee lock collection hardcoded in DEFAULT_LOCK_COLLECTION_NAME
   *
   * @param lockCollectionName a new lock collection name
   * @return Mongobee object for fluent interface
   */
  public Mongobee setLockCollectionName(String lockCollectionName) {
    this.lockRepository.setLockCollectionName(lockCollectionName);
    return this;
  }

  /**
   * Closes the Mongo instance used by Mongobee.
   * This will close either the connection Mongobee was initiated with or that which was internally created.
   */
  public void close() {
    getMongoClient().close();
  }

  MongoClient getMongoClient() {
    return mongoClient;
  }

  private void initialize() throws MongobeeConfigurationException, MongobeeConnectionException {
    //Ensuring mongoClient
    if (this.getMongoClient() == null) {
      this.mongoClient = new MongoClient(mongoClientURI);
      this.dbName = (!hasText(dbName)) ? mongoClientURI.getDatabase() : dbName;
    }

    //Validating dbName
    if (!hasText(dbName)) {
      throw new MongobeeConfigurationException("DB name is not set. Should be defined in MongoDB URI or via setter");
    }

    final MongoDatabase mongoDatabase = getMongoClient().getDatabase(dbName);
    final DB db = getMongoClient().getDB(dbName);

    //Initializing injections
    this.dao.connectMongoDb(mongoDatabase, db);
    this.lockChecker.initialize(mongoDatabase);
    this.service.setChangeLogsBasePackage(changeLogsScanPackage);
    this.service.setEnvironment(springEnvironment);
    this.mongoDatabaseProxy = proxyFactory.createProxyFromOriginal(mongoDatabase);
    this.dbProxy = proxyFactory.createProxyFromOriginal(db);
    this.jongoProxy = proxyFactory.createProxyFromOriginal(jongo != null ? jongo : new Jongo(db));
    this.mongoTemplateProxy =
        proxyFactory.createProxyFromOriginal(mongoTemplate != null ? mongoTemplate : new MongoTemplate(
            db.getMongo(), dbName));
  }

}
