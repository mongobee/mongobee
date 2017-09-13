package org.github.mongobee.spring;

import com.github.mongobee.core.Mongobee;
import com.github.mongobee.core.exception.MongobeeChangeSetException;
import com.github.mongobee.core.utils.ChangeService;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.github.mongobee.spring.utils.SpringChangeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Spring Mongobee runner
 *
 * @author j-coll
 */
public class SpringMongobee extends Mongobee implements InitializingBean {
  private static final Logger logger = LoggerFactory.getLogger(SpringMongobee.class);

  private Environment springEnvironment;
  private MongoTemplate mongoTemplate;

  /**
   * <p>Simple constructor with default configuration of host (localhost) and port (27017). Although
   * <b>the database name need to be provided</b> using {@link Mongobee#setDbName(String)} setter.</p>
   * <p>It is recommended to use constructors with MongoURI</p>
   */
  public SpringMongobee() {
  }

  /**
   * <p>Constructor takes db.mongodb.MongoClientURI object as a parameter.
   * </p><p>For more details about MongoClientURI please see com.mongodb.MongoClientURI docs
   * </p>
   *
   * @param mongoClientURI uri to your db
   * @see MongoClientURI
   */
  public SpringMongobee(MongoClientURI mongoClientURI) {
    super(mongoClientURI);
  }

  /**
   * <p>Constructor takes db.mongodb.MongoClient object as a parameter.
   * </p><p>For more details about <tt>MongoClient</tt> please see com.mongodb.MongoClient docs
   * </p>
   *
   * @param mongoClient database connection client
   * @see MongoClient
   */
  public SpringMongobee(MongoClient mongoClient) {
    super(mongoClient);
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
   * @see com.mongodb.MongoClientURI
   */
  public SpringMongobee(String mongoURI) {
    super(mongoURI);
  }

  @Override
  protected ChangeService newChangeService() {
    return new SpringChangeService(changeLogsScanPackage, springEnvironment);
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

  @Override
  protected Object executeChangeSetMethod(Method changeSetMethod, Object changeLogInstance, DB db, MongoDatabase mongoDatabase) throws IllegalAccessException, InvocationTargetException, MongobeeChangeSetException {
    if (changeSetMethod.getParameterTypes().length == 1
        && changeSetMethod.getParameterTypes()[0].equals(MongoTemplate.class)) {
      logger.debug("method with MongoTemplate argument");

      return changeSetMethod.invoke(changeLogInstance, mongoTemplate != null ? mongoTemplate : new MongoTemplate(db.getMongo(), dbName));
    } else if (changeSetMethod.getParameterTypes().length == 2
        && changeSetMethod.getParameterTypes()[0].equals(MongoTemplate.class)
        && changeSetMethod.getParameterTypes()[1].equals(Environment.class)) {
      logger.debug("method with MongoTemplate and environment arguments");

      return changeSetMethod.invoke(changeLogInstance, mongoTemplate != null ? mongoTemplate : new MongoTemplate(db.getMongo(), dbName), springEnvironment);
    } else {
      return super.executeChangeSetMethod(changeSetMethod, changeLogInstance, db, mongoDatabase);
    }
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


}
