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

package org.hibernate.cache.redis.strategy;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.redis.regions.RedisNaturalIdRegion;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * ReadWriteRedisNaturalIdRegionAccessStrategy
 *
 * @author sunghyouk.bae@gmail.com
 * @since 13. 4. 5. 오후 11:13
 */
public class ReadWriteRedisNaturalIdRegionAccessStrategy
        extends AbstractReadWriteRedisAccessStrategy<RedisNaturalIdRegion>
        implements NaturalIdRegionAccessStrategy {

    /**
     * Creates a read/write cache access strategy around the given cache region.
     */
    public ReadWriteRedisNaturalIdRegionAccessStrategy(RedisNaturalIdRegion region, SessionFactoryOptions settings) {
        super(region, settings);
    }

    @Override
    public NaturalIdRegion getRegion() {
        return region;
    }

    @Override
    public boolean insert(SharedSessionContractImplementor session, Object key, Object value) {
        region.put(key, value);
        return true;
    }

    @Override
    public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value) {
        region.put(key, value);
        return true;
    }

    @Override
    public boolean update(SharedSessionContractImplementor session, Object key, Object value) {
        region.put(key, value);
        return true;
    }

    @Override
    public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, SoftLock lock) {
        region.put(key, value);
        return true;
    }
    
    @Override
	public Object generateCacheKey(Object[] naturalIdValues, EntityPersister persister, SharedSessionContractImplementor session) {
		return DefaultCacheKeysFactory.createNaturalIdKey(naturalIdValues, persister, session);
	}

	@Override
	public Object[] getNaturalIdValues(Object cacheKey) {
		return DefaultCacheKeysFactory.getNaturalIdValues(cacheKey);
	}
}
