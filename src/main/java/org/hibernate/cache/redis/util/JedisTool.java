/*
 * Copyright 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hibernate.cache.redis.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.redis.DataSourceContextHolderKey;
import org.hibernate.cache.redis.jedis.JedisClient;
import org.hibernate.cache.redis.timestamper.JedisCacheTimestamper;
import org.hibernate.cache.redis.timestamper.JedisCacheTimestamperJvmImpl;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Protocol;
import redis.clients.util.Pool;

/**
 * Jedis Helper class
 *
 * @author 배성혁 sunghyouk.bae@gmail.com
 * @since 13. 5. 2. 오전 1:53
 */
public final class JedisTool {

    private static final String EXPIRE_IN_SECONDS = "redis.expiryInSeconds";
    private static final String EXPIRY_PROPERTY_PREFIX = EXPIRE_IN_SECONDS + ".";
    private static final String FILE_URL_PREFIX = "file:";
    public static final String TIMESTAMPER_PROPERTY_KEY = "redis.timestamper.class";
    public static final Class<?> DEFAULT_DYNAMIC_KEY_CLASS = DataSourceContextHolderKey.class;
    public static final String DYNAMIC_KEY_CLASS = "redis.dynamicKey.class";
    public static final Class<?> DEFAULT_TIMESTAMPER_CLASS = JedisCacheTimestamperJvmImpl.class;
    
    
    private static final Logger log = LoggerFactory.getLogger(JedisTool.class);

    private JedisTool() { }

    /**
     * create {@link org.hibernate.cache.redis.jedis.JedisClient} instance.
     */
    public static JedisClient createJedisClient(Properties props) {
        log.info("Creating JedisClient.");

        return new JedisClient(createJedisPool(props), getDefaultExpireInSeconds(props), getSourceContextHolderKey(props));
    }

    /**
     * create {@link redis.clients.util.Pool<Jedis>} instance.
     */
    public static Pool<Jedis> createJedisPool(Properties props) {

        String host = props.getProperty("redis.host", "localhost");
        Integer port = Integer.decode(props.getProperty("redis.port", String.valueOf(Protocol.DEFAULT_PORT)));
        Integer timeout = Integer.decode(props.getProperty("redis.timeout", String.valueOf(Protocol.DEFAULT_TIMEOUT))); // msec
        String password = props.getProperty("redis.password", null);
        Integer database = Integer.decode(props.getProperty("redis.database", String.valueOf(Protocol.DEFAULT_DATABASE)));
        String sentinelsString = props.getProperty("redis.sentinels", null);
        Set<String> sentinels = sentinelsString != null ? new HashSet<String>(Arrays.asList(sentinelsString.split(","))) : null;
        String masterName = props.getProperty("redis.masterName", null);

        JedisPoolConfig jedisPoolConfig = createJedisPoolConfig();

        Pool<Jedis> pool;
        if (sentinels != null && !sentinels.isEmpty() && masterName != null) {
            log.info("Creating JedisSentinelPool. masterName=[{}], sentinels=[{}], timeout=[{}], database=[{}]",
                     masterName, sentinels, timeout, database);
            pool = new JedisSentinelPool(masterName, sentinels, jedisPoolConfig, timeout, password, database);
        } else{
            log.info("Creating JedisPool. host=[{}], port=[{}], timeout=[{}], database=[{}]",
                     host, port, timeout, database);

            pool = new JedisPool(jedisPoolConfig, host, port, timeout, password, database);
        }
        return pool;
    }

    public static JedisCacheTimestamper createTimestamper(SessionFactoryOptions settings, Properties properties, JedisClient jedisClient) {
        String timestamperClazzName = properties.getProperty(TIMESTAMPER_PROPERTY_KEY, DEFAULT_TIMESTAMPER_CLASS.getName());

        JedisCacheTimestamper timestamper;
        try {
            Class<?> clazz = Class.forName(timestamperClazzName);
            timestamper = (JedisCacheTimestamper) clazz.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        timestamper.setSettings(settings);
        timestamper.setProperties(properties);
        timestamper.setJedisClient(jedisClient);

        return timestamper;
    }

    private static JedisPoolConfig createJedisPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(256);
        poolConfig.setMinIdle(2);
        return poolConfig;
    }

    public static Properties loadCacheProperties(final Properties props) {
        Properties cacheProps = new Properties();
        cacheProps.putAll(props); // start with the properties we got

        Properties fileSystemProperties = new Properties();
        String cachePath = props.getProperty(Environment.CACHE_PROVIDER_CONFIG,
                                             "hibernate-redis.properties");

        InputStream is = null;
        try {
            log.info("Loading cache properties... path=[{}]", cachePath);

            if (cachePath.startsWith(FILE_URL_PREFIX)) {
                // load from file
                is = new FileInputStream(new File(new URI(cachePath)));
            } else {
                // load from resources stream
                is = JedisTool.class.getClassLoader().getResourceAsStream(cachePath);
            }
            fileSystemProperties.load(is);
        } catch (Exception e) {
            log.warn("Fail to load cache properties. cachePath=" + cachePath, e);
        } finally {
            if (is != null) {
                try { is.close(); } catch (Exception ignored) { }
            }
        }
        // now add and override with any settings from the cache_provider_config
        cacheProps.putAll(fileSystemProperties);
        return cacheProps;
    }

    /**
     * Get expire time out for the specified region
     *
     * @param props      properties containing expiration settings
     * @param regionName region name defined at Entity
     * @return expiry in seconds
     */
    public static int getExpireInSeconds(final Properties props, final String regionName) {
        int defaultExpiry = getDefaultExpireInSeconds(props);
        if (props == null)
            return defaultExpiry;
        int expireInSeconds = Integer.valueOf(props.getProperty(EXPIRY_PROPERTY_PREFIX + regionName, String.valueOf(defaultExpiry)));
        log.debug("getExpireInSeconds. regionName=[{}], expireInSeconds=[{}]",
                regionName, expireInSeconds);
        return expireInSeconds;
    }

    /**
     * Get the default expire time from the supplied properties
     *
     * @param props   properties containing expiration settings
     * @return expiry in seconds
     */
    private static int getDefaultExpireInSeconds(final Properties props) {
        if (props == null)
            return JedisClient.DEFAULT_EXPIRY_IN_SECONDS;

        return Integer.decode(props.getProperty(EXPIRE_IN_SECONDS, String.valueOf(JedisClient.DEFAULT_EXPIRY_IN_SECONDS)));
    }
    
    /**
     * Get the default expire time from the supplied properties
     *
     * @param props   properties containing expiration settings
     * @return expiry in seconds
     */
    private static DataSourceContextHolderKey getSourceContextHolderKey(final Properties props) {
    	
    	if (props == null)
            return null;
    	
    	String className = props.getProperty(DYNAMIC_KEY_CLASS, DEFAULT_DYNAMIC_KEY_CLASS.getName());
    	try {
			return (DataSourceContextHolderKey) Class.forName(className).newInstance();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
    }
}
