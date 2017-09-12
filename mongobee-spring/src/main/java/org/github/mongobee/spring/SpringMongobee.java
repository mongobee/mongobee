package org.github.mongobee.spring;

import com.github.mongobee.core.Mongobee;
import com.github.mongobee.core.exception.MongobeeChangeSetException;
import com.github.mongobee.core.utils.ChangeService;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
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

  public SpringMongobee() {
  }

  public SpringMongobee(MongoClientURI mongoClientURI) {
    super(mongoClientURI);
  }

  public SpringMongobee(MongoClient mongoClient) {
    super(mongoClient);
  }

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
