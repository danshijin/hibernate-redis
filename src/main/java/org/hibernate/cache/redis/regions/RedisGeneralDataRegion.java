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

package org.hibernate.cache.redis.regions;

import java.util.Properties;

import org.hibernate.cache.redis.jedis.JedisClient;
import org.hibernate.cache.redis.strategy.RedisAccessStrategyFactory;
import org.hibernate.cache.redis.timestamper.JedisCacheTimestamper;
import org.hibernate.cache.spi.GeneralDataRegion;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RedisGeneralDataRegion
 *
 * @author sunghyouk.bae@gmail.com
 * @since 13. 4. 5. 오후 9:00
 */
public abstract class RedisGeneralDataRegion extends RedisDataRegion implements GeneralDataRegion {

    private static final Logger log = LoggerFactory.getLogger(RedisGeneralDataRegion.class);

    protected RedisGeneralDataRegion(RedisAccessStrategyFactory accessStrategyFactory,
                                     JedisClient redis,
                                     String regionName,
                                     Properties props,
                                     JedisCacheTimestamper timestamper) {
        super(accessStrategyFactory, redis, regionName, props, timestamper);
    }

    @Override
    public Object get(SharedSessionContractImplementor session, Object key) {
        if (key == null) return null;
        try {
            Object value = redis.get(getName(), key, getExpireInSeconds());
            log.trace("get cache item... key=[{}], value=[{}]", key, value);
            return value;
        } catch (Exception e) {
            log.warn("Fail to get cache item... key=" + key, e);
            return null;
        }
    }

    @Override
    public void put(SharedSessionContractImplementor session, Object key, Object value) {
        try {
            redis.set(getName(), key, value, getExpireInSeconds());
        } catch (Exception e) {
            log.warn("Fail to put cache item... key=" + key, e);
        }
    }

    @Override
    public void evict(Object key) {
        try {
            redis.del(getName(), key);
        } catch (Exception e) {
            log.warn("Fail to remove cache item... key=" + key, e);
        }
    }

    @Override
    public void evictAll() {
        try {
            redis.deleteRegion(getName());
        } catch (Exception e) {
            log.warn("Fail to remove cache items... region=" + getName(), e);
        }
    }
}
