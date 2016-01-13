package com.github.mongobee.jongo;

import com.github.mongobee.exception.MongobeeChangeSetException;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClientURI;
import org.jongo.Jongo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class JongoMongobee extends com.github.mongobee.Mongobee {
    private static final Logger logger = LoggerFactory.getLogger(JongoMongobee.class);

    private Jongo jongo;

    public JongoMongobee() {
        super();
    }

    public JongoMongobee(Mongo mongo) {
        super(mongo);
    }

    public JongoMongobee(String mongoURI) {
        super(mongoURI);
    }

    public JongoMongobee(MongoClientURI mongoClientURI) {
        super(mongoClientURI);
    }

    @Override
    protected Object executeChangeSetMethod(Method changeSetMethod, Object changeLogInstance, DB db) throws IllegalAccessException, InvocationTargetException, MongobeeChangeSetException {
        if (changeSetMethod.getParameterTypes().length == 1
                && changeSetMethod.getParameterTypes()[0].equals(Jongo.class)) {
            logger.debug("method with Jongo argument");

            return changeSetMethod.invoke(changeLogInstance, jongo != null ? jongo : new Jongo(db));
        } else {
            return super.executeChangeSetMethod(changeSetMethod, changeLogInstance, db);
        }
    }

    /**
     * Sets pre-configured {@link Jongo} instance to use by the Mongobee
     * @param jongo {@link Jongo} instance
     * @return Mongobee object for fluent interface
     */
    public JongoMongobee setJongo(Jongo jongo) {
        this.jongo = jongo;
        return this;
    }

}
