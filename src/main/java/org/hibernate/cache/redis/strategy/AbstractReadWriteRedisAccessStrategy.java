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
import org.hibernate.cache.redis.regions.RedisTransactionalDataRegion;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Superclass for all Redis specific read/write AccessStrategy implementations.
 *
 * @param <T> the type of the enclosed cache region
 * @author sunghyouk.bae@gmail.com
 * @since 13. 4. 5. 오후 10:07
 */
public class AbstractReadWriteRedisAccessStrategy<T extends RedisTransactionalDataRegion>
        extends AbstractRedisAccessStrategy<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractReadWriteRedisAccessStrategy.class);

    /**
     * Creates a read/write cache access strategy around the given cache region.
     */
    public AbstractReadWriteRedisAccessStrategy(T region, SessionFactoryOptions settings) {
        super(region, settings);
    }

    /**
     * Returns <code>null</code> if the item is not readable.  Locked items are not readable, nor are items created
     * after the start of this transaction.
     */
    public final Object get(SharedSessionContractImplementor session, Object key, long txTimestamp) {
        return region.get(key);
    }

    @Override
    public final boolean putFromLoad(SharedSessionContractImplementor session, Object key,
                                     Object value,
                                     long txTimestamp,
                                     Object version,
                                     boolean minimalPutOverride) {
        region.put(key, value);
        return true;
    }

    /**
     * Soft-lock a cache item.
     */
    public final SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) {
        region.remove(key);
        return null;
    }

    /**
     * Soft-unlock a cache item.
     */
    public final void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) {
        region.remove(key);
    }
    


}