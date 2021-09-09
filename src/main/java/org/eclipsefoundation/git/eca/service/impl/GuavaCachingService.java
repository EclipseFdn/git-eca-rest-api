/*******************************************************************************
 * Copyright (C) 2020 Eclipse Foundation
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
/* Copyright (c) 2019 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html,
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipsefoundation.git.eca.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipsefoundation.git.eca.service.CachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * <p>
 * Simple caching service for caching objects in an in-memory cache, implemented
 * using the Google Guava cache mechanism. Cache size and time to live are
 * configured within the MicroProfile configuration.
 * </p>
 * 
 * <p>
 * Guava cache is inherently thread safe, so no synchronization needs to be done
 * on access.
 * </p>
 * 
 * @author Martin Lowe
 * @param <T> the type of object cached by this instance of the service
 *
 */
@ApplicationScoped
public class GuavaCachingService implements CachingService {
	private static final Logger LOGGER = LoggerFactory.getLogger(GuavaCachingService.class);

	@ConfigProperty(name = "cache.max.size", defaultValue = "10000")
	long maxSize;
	@ConfigProperty(name = "cache.ttl.write.seconds", defaultValue = "900")
	long ttlWrite;

	// actual cache object
	private Map<Class<?>, Cache<String, ?>> caches;
	private Map<String, Long> ttl;

	@PostConstruct
	public void init() {
		this.ttl = new HashMap<>();
		this.caches = new HashMap<>();
	}

	@Override
	public <T> Optional<T> get(String cacheKey, Callable<? extends T> callable, Class<T> clazz) {
		Objects.requireNonNull(cacheKey);
		Objects.requireNonNull(callable);
		try {
			// multi cache support
			@SuppressWarnings("unchecked")
			Cache<String, T> c = (Cache<String, T>) caches.computeIfAbsent(clazz, key -> createCache());
			
			// get entry, and enter a ttl as soon as it returns
			T data = c.get(cacheKey, callable);
			if (data != null) {
				ttl.putIfAbsent(cacheKey,
						System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(ttlWrite, TimeUnit.SECONDS));
			}
			return Optional.of(c.get(cacheKey, callable));
		} catch (InvalidCacheLoadException | UncheckedExecutionException e) {
			LOGGER.error("Error while retrieving fresh value for cachekey: {}", cacheKey, e);
		} catch (Exception e) {
			LOGGER.error("Error while retrieving value of callback", e);
		}
		return Optional.empty();
	}
	
	private <T> Cache<String, T> createCache() {
		return CacheBuilder
			.newBuilder()
			.maximumSize(maxSize)
			.expireAfterWrite(ttlWrite, TimeUnit.SECONDS)
			.removalListener(not -> ttl.remove(not.getKey()))
			.build();
	}

	@Override
	public Optional<Long> getExpiration(String cacheKey) {
		return Optional.ofNullable(ttl.get(cacheKey));
	}
	
	@Override
	public Set<String> getCacheKeys() {
		// create a set and return all keys
		Set<String> out = new HashSet<>();
		caches.values().stream().forEach(c -> out.addAll(c.asMap().keySet()));
		return out;
	}

	@Override
	public void remove(String key) {
		caches.values().stream().forEach(c -> c.invalidate(key));
	}

	@Override
	public void removeAll() {
		caches.values().stream().forEach(Cache::invalidateAll);
	}

	@Override
	public long getMaxAge() {
		return ttlWrite;
	}
}
