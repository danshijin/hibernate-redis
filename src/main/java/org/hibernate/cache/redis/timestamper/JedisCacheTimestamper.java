package org.hibernate.cache.redis.timestamper;

import java.util.Properties;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.redis.jedis.JedisClient;

public interface JedisCacheTimestamper {

    void setSettings(SessionFactoryOptions settings);

    void setProperties(Properties properties);

    void setJedisClient(JedisClient jedisClient);

    /** get next timestamp */
    long next();
}
