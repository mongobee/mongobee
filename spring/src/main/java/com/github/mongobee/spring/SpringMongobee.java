package com.github.mongobee.spring;

import com.github.mongobee.exception.MongobeeChangeSetException;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClientURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SpringMongobee extends com.github.mongobee.Mongobee implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(SpringMongobee.class);

    private Environment springEnvironment;

    private MongoTemplate mongoTemplate;

    public SpringMongobee() {
        super();
    }

    public SpringMongobee(MongoClientURI mongoClientURI) {
        super(mongoClientURI);
    }

    public SpringMongobee(Mongo mongo) {
        super(mongo);
    }

    public SpringMongobee(String mongoURI) {
        super(mongoURI);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.setChangeService(new SpringChangeService(changeLogsScanPackage, springEnvironment));
        super.execute();
    }

    @Override
    protected Object executeChangeSetMethod(Method changeSetMethod, Object changeLogInstance, DB db) throws IllegalAccessException, InvocationTargetException, MongobeeChangeSetException {
        if (changeSetMethod.getParameterTypes().length == 1
                && changeSetMethod.getParameterTypes()[0].equals(MongoTemplate.class)) {
            logger.debug("method with MongoTemplate argument");

            return changeSetMethod.invoke(changeLogInstance, mongoTemplate != null ? mongoTemplate : new MongoTemplate(db.getMongo(), dbName));
        } else {
            return super.executeChangeSetMethod(changeSetMethod, changeLogInstance, db);
        }
    }

    /**
     * Set Environment object for Spring Profiles (@Profile) integration
     *
     * @param environment org.springframework.core.env.Environment object to inject
     * @return Mongobee object for fluent interface
     */
    public SpringMongobee setSpringEnvironment(Environment environment) {
        this.springEnvironment = environment;
        return this;
    }
    /**
     * Sets pre-configured {@link MongoTemplate} instance to use by the Mongobee
     * @param mongoTemplate instance of the {@link MongoTemplate}
     * @return Mongobee object for fluent interface
     */
    public SpringMongobee setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        return this;
    }

}
