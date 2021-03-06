/*
 * Copyright 2014 Ben Manes. All Rights Reserved.
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
package com.github.benmanes.caffeine.cache;

import static com.github.benmanes.caffeine.cache.testing.HasRemovalNotifications.hasRemovalNotifications;
import static com.github.benmanes.caffeine.cache.testing.HasStats.hasHitCount;
import static com.github.benmanes.caffeine.cache.testing.HasStats.hasLoadFailureCount;
import static com.github.benmanes.caffeine.cache.testing.HasStats.hasLoadSuccessCount;
import static com.github.benmanes.caffeine.cache.testing.HasStats.hasMissCount;
import static com.github.benmanes.caffeine.testing.IsEmptyIterable.deeplyEmpty;
import static com.github.benmanes.caffeine.testing.IsEmptyMap.emptyMap;
import static com.google.common.collect.Maps.immutableEntry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.github.benmanes.caffeine.cache.testing.CacheContext;
import com.github.benmanes.caffeine.cache.testing.CacheProvider;
import com.github.benmanes.caffeine.cache.testing.CacheSpec;
import com.github.benmanes.caffeine.cache.testing.CacheSpec.Implementation;
import com.github.benmanes.caffeine.cache.testing.CacheSpec.Listener;
import com.github.benmanes.caffeine.cache.testing.CacheSpec.Population;
import com.github.benmanes.caffeine.cache.testing.CacheValidationListener;
import com.github.benmanes.caffeine.cache.testing.CheckNoStats;
import com.google.common.collect.ImmutableMap;
import com.google.common.testing.SerializableTester;

/**
 * The test cases for the {@link Cache#asMap()} view and its serializability. These tests do not
 * validate eviction management or concurrency behavior.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
@Listeners(CacheValidationListener.class)
@Test(dataProviderClass = CacheProvider.class)
public final class AsMapTest {
  // Statistics are recorded only for computing methods for loadSuccess and loadFailure

  /* ---------------- is empty / size / clear -------------- */

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void isEmpty(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.isEmpty(), is(context.original().isEmpty()));
    if (map.isEmpty()) {
      assertThat(map, is(emptyMap()));
    }
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void size(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.size(), is(context.original().size()));
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches")
  public void clear(Map<Integer, Integer> map, CacheContext context) {
    map.clear();
    assertThat(map, is(emptyMap()));
    assertThat(map, hasRemovalNotifications(context,
        context.original().size(), RemovalCause.EXPLICIT));
  }

  /* ---------------- contains -------------- */

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void containsKey_null(Map<Integer, Integer> map, CacheContext context) {
    map.containsKey(null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void containsKey_present(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      assertThat(map.containsKey(key), is(true));
    }
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void containsKey_absent(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.containsKey(context.absentKey()), is(false));
  }

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void containsValue_null(Map<Integer, Integer> map, CacheContext context) {
    map.containsValue(null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void containsValue_present(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      assertThat(map.containsValue(context.original().get(key)), is(true));
    }
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void containsValue_absent(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.containsValue(context.absentValue()), is(false));
  }

  /* ---------------- get -------------- */

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void get_null(Map<Integer, Integer> map, CacheContext context) {
    map.get(null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void get_absent(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.get(context.absentKey()), is(nullValue()));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void get_present(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      assertThat(map.get(key), is(context.original().get(key)));
    }
  }

  /* ---------------- get -------------- */

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void getOrDefault_nullKey(Map<Integer, Integer> map, CacheContext context) {
    map.getOrDefault(null, 1);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void getOrDefault_absent(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.getOrDefault(context.absentKey(), null), is(nullValue()));
    assertThat(map.getOrDefault(context.absentKey(), context.absentKey()), is(context.absentKey()));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void getOrDefault_present(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      assertThat(map.getOrDefault(key, context.absentKey()), is(context.original().get(key)));
    }
  }

  /* ---------------- forEach -------------- */

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void forEach_null(Map<Integer, Integer> map, CacheContext context) {
    map.forEach(null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void forEach_scan(Map<Integer, Integer> map, CacheContext context) {
    Map<Integer, Integer> remaining = new HashMap<>(context.original());
    map.forEach(remaining::remove);
    assertThat(remaining, is(emptyMap()));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void forEach_modify(Map<Integer, Integer> map, CacheContext context) {
    // non-deterministic traversal behavior with modifications, but shouldn't become corrupted
    List<Integer> modified = new ArrayList<>();
    map.forEach((key, value) -> {
      Integer newKey = context.lastKey() + key;
      modified.add(newKey); // for weak keys
      map.put(newKey, key);
    });
  }

  /* ---------------- put -------------- */

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void put_nullKey(Map<Integer, Integer> map, CacheContext context) {
    map.put(null, 1);
  }

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void put_nullValue(Map<Integer, Integer> map, CacheContext context) {
    map.put(1, null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void put_nullKeyAndValue(Map<Integer, Integer> map, CacheContext context) {
    map.put(null, null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void put_insert(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.put(context.absentKey(), context.absentValue()), is(nullValue()));
    assertThat(map.get(context.absentKey()), is(context.absentValue()));
    assertThat(map.size(), is(context.original().size() + 1));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.CONSUMING })
  public void put_replace_sameValue(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      Integer value = context.original().get(key);
      assertThat(map.put(key, value), is(value));
      assertThat(map.get(key), is(value));
    }
    int count = context.firstMiddleLastKeys().size();
    assertThat(map.size(), is(context.original().size()));
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.REPLACED));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.CONSUMING })
  public void put_replace_differentValue(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      Integer value = context.original().get(key);
      assertThat(map.put(key, context.absentValue()), is(value));
      assertThat(map.get(key), is(context.absentValue()));
    }

    int count = context.firstMiddleLastKeys().size();
    assertThat(map.size(), is(context.original().size()));
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.REPLACED));
  }

  /* ---------------- putAll -------------- */

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void putAll_null(Map<Integer, Integer> map, CacheContext context) {
    map.putAll(null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void putAll_empty(Map<Integer, Integer> map, CacheContext context) {
    map.putAll(new HashMap<>());
    assertThat(map.size(), is(context.original().size()));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void putAll_insert(Map<Integer, Integer> map, CacheContext context) {
    int startKey = context.original().size() + 1;
    Map<Integer, Integer> entries = IntStream
        .range(startKey, 100 + startKey).boxed()
        .collect(Collectors.toMap(Function.identity(), key -> -key));
    map.putAll(entries);
    assertThat(map.size(), is(100 + context.original().size()));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.CONSUMING })
  public void putAll_replace(Map<Integer, Integer> map, CacheContext context) {
    Map<Integer, Integer> entries = new LinkedHashMap<>(context.original());
    entries.replaceAll((key, value) -> key);
    map.putAll(entries);
    assertThat(map, is(equalTo(entries)));
    assertThat(map, hasRemovalNotifications(context, entries.size(), RemovalCause.REPLACED));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.CONSUMING })
  public void putAll_mixed(Map<Integer, Integer> map, CacheContext context) {
    Map<Integer, Integer> expect = new HashMap<>(context.original());
    Map<Integer, Integer> entries = new HashMap<>();
    for (int i = 0; i < 2 * context.original().size(); i++) {
      int value = ((i % 2) == 0) ? i : (i + 1);
      entries.put(i, value);
    }
    expect.putAll(entries);

    map.putAll(entries);
    assertThat(map, is(equalTo(expect)));
    assertThat(map, hasRemovalNotifications(context, entries.size() / 2, RemovalCause.REPLACED));
  }

  /* ---------------- putIfAbsent -------------- */

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void putIfAbsent_nullKey(Map<Integer, Integer> map, CacheContext context) {
    map.putIfAbsent(null, 2);
  }

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void putIfAbsent_nullValue(Map<Integer, Integer> map, CacheContext context) {
    map.putIfAbsent(1, null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void putIfAbsent_nullKeyAndValue(Map<Integer, Integer> map, CacheContext context) {
    map.putIfAbsent(null, null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
  removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void putIfAbsent_present(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      Integer value = context.original().get(key);
      assertThat(map.putIfAbsent(key, key), is(value));
      assertThat(map.get(key), is(value));
    }
    assertThat(map.size(), is(context.original().size()));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void putIfAbsent_insert(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.putIfAbsent(context.absentKey(), context.absentValue()), is(nullValue()));
    assertThat(map.get(context.absentKey()), is(context.absentValue()));
    assertThat(map.size(), is(context.original().size() + 1));
  }

  /* ---------------- remove -------------- */

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void remove_nullKey(Map<Integer, Integer> map, CacheContext context) {
    map.remove(null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void remove_absent(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.remove(context.absentKey()), is(nullValue()));
    assertThat(map.size(), is(context.original().size()));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void remove_present(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      map.remove(key);
    }
    assertThat(map.size(), is(context.original().size() - context.firstMiddleLastKeys().size()));

    int count = context.firstMiddleLastKeys().size();
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.EXPLICIT));
  }

  /* ---------------- remove conditionally -------------- */

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void removeConditionally_nullKey(Map<Integer, Integer> map, CacheContext context) {
    map.remove(null, 1);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void removeConditionally_nullValue(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.remove(1, null), is(false)); // see ConcurrentHashMap
  }

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void removeConditionally_nullKeyAndValue(Map<Integer, Integer> map, CacheContext context) {
    map.remove(null, null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void removeConditionally_absent(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.remove(context.absentKey(), context.absentValue()), is(false));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void removeConditionally_presentKey(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      assertThat(map.remove(key, key), is(false));
    }
    assertThat(map.size(), is(context.original().size()));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void removeConditionally_presentKeyAndValue(Map<Integer, Integer> map,
      CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      assertThat(map.remove(key, context.original().get(key)), is(true));
    }
    int count = context.firstMiddleLastKeys().size();
    assertThat(map.size(), is(context.original().size() - count));
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.EXPLICIT));
  }

  /* ---------------- replace -------------- */

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void replace_null(Map<Integer, Integer> map, CacheContext context) {
    map.replace(null, 1);
  }

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void replace_nullValue(Map<Integer, Integer> map, CacheContext context) {
    map.replace(1, null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void replace_nullKeyAndValue(Map<Integer, Integer> map, CacheContext context) {
    map.replace(null, null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void replace_absent(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.replace(context.absentKey(), context.absentValue()), is(nullValue()));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void replace_sameValue(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      Integer value = context.original().get(key);
      assertThat(map.replace(key, value), is(value));
      assertThat(map.get(key), is(value));
    }
    assertThat(map.size(), is(context.original().size()));

    int count = context.firstMiddleLastKeys().size();
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.REPLACED));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void replace_differentValue(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      Integer oldValue = context.original().get(key);
      assertThat(map.replace(key, context.absentValue()), is(oldValue));
      assertThat(map.get(key), is(context.absentValue()));
    }
    int count = context.firstMiddleLastKeys().size();
    assertThat(map.size(), is(context.original().size()));
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.REPLACED));
  }

  /* ---------------- replace conditionally -------------- */

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void replaceConditionally_nullKey(Map<Integer, Integer> map, CacheContext context) {
    map.replace(null, 1, 1);
  }

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void replaceConditionally_nullOldValue(Map<Integer, Integer> map, CacheContext context) {
    map.replace(1, null, 1);
  }

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void replaceConditionally_nullNewValue(Map<Integer, Integer> map, CacheContext context) {
    map.replace(1, 1, null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void replaceConditionally_nullKeyAndOldValue(Map<Integer, Integer> map, CacheContext context) {
    map.replace(null, null, 1);
  }

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void replaceConditionally_nullKeyAndNewValue(Map<Integer, Integer> map, CacheContext context) {
    map.replace(null, 1, null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void replaceConditionally_nullOldAndNewValue(Map<Integer, Integer> map, CacheContext context) {
    map.replace(1, null, null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void replaceConditionally_nullKeyAndValues(Map<Integer, Integer> map, CacheContext context) {
    map.replace(null, null, null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void replaceConditionally_absent(Map<Integer, Integer> map, CacheContext context) {
    Integer key = context.absentKey();
    assertThat(map.replace(key, key, key), is(false));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void replaceConditionally_wrongOldValue(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      Integer value = context.original().get(key);
      assertThat(map.replace(key, key, context.absentKey()), is(false));
      assertThat(map.get(key), is(value));
    }
    assertThat(map.size(), is(context.original().size()));

    int count = context.firstMiddleLastKeys().size();
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.REPLACED));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void replaceConditionally_sameValue(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      Integer value = context.original().get(key);
      assertThat(map.replace(key, value, value), is(true));
      assertThat(map.get(key), is(value));
    }
    assertThat(map.size(), is(context.original().size()));

    int count = context.firstMiddleLastKeys().size();
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.REPLACED));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void replaceConditionally_differentValue(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      assertThat(map.replace(key, context.original().get(key), context.absentValue()), is(true));
      assertThat(map.get(key), is(context.absentValue()));
    }
    assertThat(map.size(), is(context.original().size()));

    int count = context.firstMiddleLastKeys().size();
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.REPLACED));
  }

  /* ---------------- replaceAll -------------- */

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void replaceAll_null(Map<Integer, Integer> map, CacheContext context) {
    map.replaceAll(null);
  }

  @CheckNoStats
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void replaceAll_nullValue(Map<Integer, Integer> map, CacheContext context) {
    map.replaceAll((key, value) -> null);
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches")
  public void replaceAll_sameValue(Map<Integer, Integer> map, CacheContext context) {
    map.replaceAll((key, value) -> value);
    assertThat(map, is(equalTo(context.original())));
    assertThat(map, hasRemovalNotifications(context, map.size(), RemovalCause.REPLACED));
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches")
  public void replaceAll_differentValue(Map<Integer, Integer> map, CacheContext context) {
    map.replaceAll((key, value) -> key);
    map.forEach((key, value) -> assertThat(value, is(equalTo(key))));
    assertThat(map, hasRemovalNotifications(context, map.size(), RemovalCause.REPLACED));
  }

  /* ---------------- computeIfAbsent -------------- */

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void computeIfAbsent_nullKey(Map<Integer, Integer> map, CacheContext context) {
    map.computeIfAbsent(null, key -> -key);
  }

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void computeIfAbsent_nullMappingFunction(Map<Integer, Integer> map, CacheContext context) {
    map.computeIfAbsent(context.absentKey(), null);
  }

  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void computeIfAbsent_nullValue(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.computeIfAbsent(context.absentKey(), key -> null), is(nullValue()));
    assertThat(map.size(), is(context.original().size()));
    assertThat(context, both(hasMissCount(1)).and(hasHitCount(0)));
    assertThat(context, both(hasLoadSuccessCount(0)).and(hasLoadFailureCount(1)));
  }

  // FIXME: Requires JDK8 release with JDK-8062841 fix
  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(enabled = false, dataProvider = "caches", expectedExceptions = IllegalStateException.class)
  public void computeIfAbsent_recursive(Map<Integer, Integer> map, CacheContext context) {
    Function<Integer, Integer> mappingFunction = new Function<Integer, Integer>() {
      @Override public Integer apply(Integer key) {
        return map.computeIfAbsent(key, this);
      }
    };
    map.computeIfAbsent(context.absentKey(), mappingFunction);
  }

  // FIXME: Requires JDK8 release with JDK-8062841 fix
  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(enabled = false, dataProvider = "caches", expectedExceptions = IllegalStateException.class)
  public void computeIfAbsent_pingpong(Map<Integer, Integer> map, CacheContext context) {
    Function<Integer, Integer> mappingFunction = new Function<Integer, Integer>() {
      @Override public Integer apply(Integer key) {
        Integer value = context.original().get(key);
        return map.computeIfAbsent(value, this);
      }
    };
    map.computeIfAbsent(context.absentKey(), mappingFunction);
  }

  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void computeIfAbsent_error(Map<Integer, Integer> map, CacheContext context) {
    try {
      map.computeIfAbsent(context.absentKey(), key -> { throw new Error(); });
    } catch (Error e) {}
    assertThat(map, is(equalTo(context.original())));
    assertThat(context, both(hasMissCount(1)).and(hasHitCount(0)));
    assertThat(context, both(hasLoadSuccessCount(0)).and(hasLoadFailureCount(1)));
  }

  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void computeIfAbsent_present(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      Integer value = context.original().get(key);
      assertThat(map.computeIfAbsent(key, k -> { throw new AssertionError(); }), is(value));
    }
    int count = context.firstMiddleLastKeys().size();
    assertThat(map.size(), is(context.original().size()));
    assertThat(context, both(hasMissCount(0)).and(hasHitCount(count)));
    assertThat(context, both(hasLoadSuccessCount(0)).and(hasLoadFailureCount(0)));
  }

  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void computeIfAbsent_absent(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.computeIfAbsent(context.absentKey(),
        key -> context.absentValue()), is(context.absentValue()));
    assertThat(context, both(hasMissCount(1)).and(hasHitCount(0)));
    assertThat(context, both(hasLoadSuccessCount(1)).and(hasLoadFailureCount(0)));
    assertThat(map.get(context.absentKey()), is(context.absentValue()));
    assertThat(map.size(), is(1 + context.original().size()));
  }

  /* ---------------- computeIfPresent -------------- */

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void computeIfPresent_nullKey(Map<Integer, Integer> map, CacheContext context) {
    map.computeIfPresent(null, (key, value) -> -key);
  }

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void computeIfPresent_nullMappingFunction(Map<Integer, Integer> map, CacheContext context) {
    map.computeIfPresent(1, null);
  }

  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void computeIfPresent_nullValue(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      map.computeIfPresent(key, (k, v) -> null);
    }

    int count = context.firstMiddleLastKeys().size();
    assertThat(map.size(), is(context.original().size() - count));
    assertThat(context, both(hasMissCount(0)).and(hasHitCount(0)));
    assertThat(context, both(hasLoadSuccessCount(0)).and(hasLoadFailureCount(count)));
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.EXPLICIT));
  }

  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = StackOverflowError.class)
  public void computeIfPresent_recursive(Map<Integer, Integer> map, CacheContext context) {
    // As we cannot provide immediate checking without an expensive solution, e.g. ThreadLocal,
    // instead we assert that a stack overflow error will occur to inform the developer (vs
    // a live-lock or deadlock alternative).
    BiFunction<Integer, Integer, Integer> mappingFunction =
        new BiFunction<Integer, Integer, Integer>() {
          boolean recursed;

          @Override public Integer apply(Integer key, Integer value) {
            if (recursed) {
              throw new StackOverflowError();
            }
            recursed = true;
            return map.computeIfPresent(key, this);
          }
        };
    map.computeIfPresent(context.firstKey(), mappingFunction);
  }

  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = StackOverflowError.class)
  public void computeIfPresent_pingpong(Map<Integer, Integer> map, CacheContext context) {
    // As we cannot provide immediate checking without an expensive solution, e.g. ThreadLocal,
    // instead we assert that a stack overflow error will occur to inform the developer (vs
    // a live-lock or deadlock alternative).
    BiFunction<Integer, Integer, Integer> mappingFunction =
        new BiFunction<Integer, Integer, Integer>() {
          int recursed;

          @Override public Integer apply(Integer key, Integer value) {
            if (++recursed == 2) {
              throw new StackOverflowError();
            }
            return map.computeIfPresent(context.lastKey(), this);
          }
        };
    map.computeIfPresent(context.firstKey(), mappingFunction);
  }

  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void computeIfPresent_error(Map<Integer, Integer> map, CacheContext context) {
    try {
      map.computeIfPresent(context.firstKey(), (key, value) -> { throw new Error(); });
    } catch (Error e) {}
    assertThat(map, is(equalTo(context.original())));
    assertThat(context, both(hasMissCount(0)).and(hasHitCount(0)));
    assertThat(context, both(hasLoadSuccessCount(0)).and(hasLoadFailureCount(1)));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void computeIfPresent_absent(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.computeIfPresent(context.absentKey(), (key, value) -> value), is(nullValue()));
    assertThat(map.get(context.absentKey()), is(nullValue()));
    assertThat(map.size(), is(context.original().size()));
  }

  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void computeIfPresent_present(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      assertThat(map.computeIfPresent(key, (k, v) -> k), is(key));
    }
    int count = context.firstMiddleLastKeys().size();
    assertThat(context, both(hasMissCount(0)).and(hasHitCount(0)));
    assertThat(context, both(hasLoadSuccessCount(count)).and(hasLoadFailureCount(0)));

    for (Integer key : context.firstMiddleLastKeys()) {
      assertThat(map.get(key), is(key));
    }
    assertThat(map.size(), is(context.original().size()));
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.REPLACED));
  }

  /* ---------------- compute -------------- */

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void compute_nullKey(Map<Integer, Integer> map, CacheContext context) {
    map.compute(null, (key, value) -> -key);
  }

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void compute_nullMappingFunction(Map<Integer, Integer> map, CacheContext context) {
    map.computeIfPresent(1, null);
  }

  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void compute_remove(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      assertThat(map.compute(key, (k, v) -> null), is(nullValue()));
    }

    int count = context.firstMiddleLastKeys().size();
    assertThat(context, both(hasMissCount(0)).and(hasHitCount(0)));
    assertThat(context, both(hasLoadSuccessCount(0)).and(hasLoadFailureCount(count)));

    assertThat(map.size(), is(context.original().size() - count));
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.EXPLICIT));
  }

  // FIXME: Requires JDK8 release with JDK-8062841 fix
  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(enabled = false, dataProvider = "caches", expectedExceptions = StackOverflowError.class)
  public void compute_recursive(Map<Integer, Integer> map, CacheContext context) {
    BiFunction<Integer, Integer, Integer> mappingFunction =
        new BiFunction<Integer, Integer, Integer>() {
          @Override public Integer apply(Integer key, Integer value) {
            return map.compute(key, this);
          }
        };
    map.compute(context.absentKey(), mappingFunction);
  }

  // FIXME: Requires JDK8 release with JDK-8062841 fix
  @CheckNoStats
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(enabled = false, dataProvider = "caches", expectedExceptions = StackOverflowError.class)
  public void compute_pingpong(Map<Integer, Integer> map, CacheContext context) {
    BiFunction<Integer, Integer, Integer> mappingFunction =
        new BiFunction<Integer, Integer, Integer>() {
          @Override public Integer apply(Integer key, Integer value) {
            return map.computeIfPresent(context.lastKey(), this);
          }
        };
    map.computeIfPresent(context.firstKey(), mappingFunction);
  }

  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void compute_error(Map<Integer, Integer> map, CacheContext context) {
    try {
      map.compute(context.absentKey(), (key, value) -> { throw new Error(); });
    } catch (Error e) {}
    assertThat(map, is(equalTo(context.original())));
    assertThat(context, both(hasMissCount(0)).and(hasHitCount(0)));
    assertThat(context, both(hasLoadSuccessCount(0)).and(hasLoadFailureCount(1)));
  }

  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void compute_absent(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.compute(context.absentKey(),
        (key, value) -> context.absentValue()), is(context.absentValue()));
    assertThat(context, both(hasMissCount(0)).and(hasHitCount(0)));
    assertThat(context, both(hasLoadSuccessCount(1)).and(hasLoadFailureCount(0)));
    assertThat(map.get(context.absentKey()), is(context.absentValue()));
    assertThat(map.size(), is(1 + context.original().size()));
  }

  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void compute_sameValue(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      Integer value = context.original().get(key);
      assertThat(map.compute(key, (k, v) -> v), is(value));
    }
    int count = context.firstMiddleLastKeys().size();
    assertThat(context, both(hasMissCount(0)).and(hasHitCount(0)));
    assertThat(context, both(hasLoadSuccessCount(count)).and(hasLoadFailureCount(0)));

    for (Integer key : context.firstMiddleLastKeys()) {
      Integer value = context.original().get(key);
      assertThat(map.get(key), is(value));
    }
    assertThat(map.size(), is(context.original().size()));
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.REPLACED));
  }

  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void compute_differentValue(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      assertThat(map.compute(key, (k, v) -> k), is(key));
    }
    int count = context.firstMiddleLastKeys().size();
    assertThat(context, both(hasMissCount(0)).and(hasHitCount(0)));
    assertThat(context, both(hasLoadSuccessCount(count)).and(hasLoadFailureCount(0)));
    for (Integer key : context.firstMiddleLastKeys()) {
      assertThat(map.get(key), is(key));
    }
    assertThat(map.size(), is(context.original().size()));
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.REPLACED));
  }

  /* ---------------- merge -------------- */

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void merge_nullKey(Map<Integer, Integer> map, CacheContext context) {
    map.merge(null, 1, (key, value) -> -key);
  }

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void merge_nullValue(Map<Integer, Integer> map, CacheContext context) {
    map.merge(1, null, (key, value) -> -key);
  }

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void merge_nullMappingFunction(Map<Integer, Integer> map, CacheContext context) {
    map.merge(1, 1, null);
  }

  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void merge_remove(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      Integer value = context.original().get(key);
      assertThat(map.merge(key, value, (k, v) -> null), is(nullValue()));
    }
    int count = context.firstMiddleLastKeys().size();
    assertThat(context, both(hasMissCount(0)).and(hasHitCount(0)));
    assertThat(context, both(hasLoadSuccessCount(0)).and(hasLoadFailureCount(count)));

    assertThat(map.size(), is(context.original().size() - count));
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.EXPLICIT));
  }

  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches")
  public void merge_recursive(Map<Integer, Integer> map, CacheContext context) {
    BiFunction<Integer, Integer, Integer> mappingFunction =
        new BiFunction<Integer, Integer, Integer>() {
          @Override public Integer apply(Integer key, Integer value) {
            return map.merge(key, -key, this);
          }
        };
    Integer firstValue = context.original().get(context.firstKey());
    Integer value = map.merge(context.absentKey(), firstValue, mappingFunction);
    assertThat(value, is(firstValue));
  }

  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = StackOverflowError.class)
  public void merge_pingpong(Map<Integer, Integer> map, CacheContext context) {
    // As we cannot provide immediate checking without an expensive solution, e.g. ThreadLocal,
    // instead we assert that a stack overflow error will occur to inform the developer (vs
    // a live-lock or deadlock alternative).
    BiFunction<Integer, Integer, Integer> mappingFunction =
        new BiFunction<Integer, Integer, Integer>() {
          int recursed;

          @Override public Integer apply(Integer key, Integer value) {
            if (++recursed == 2) {
              throw new StackOverflowError();
            }
            return map.merge(context.lastKey(), context.original().get(context.lastKey()), this);
          }
        };
    map.merge(context.firstKey(), context.original().get(context.firstKey()), mappingFunction);
  }

  @Test(dataProvider = "caches")
  @CacheSpec(implementation = Implementation.Guava,
      population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void merge_error(Map<Integer, Integer> map, CacheContext context) {
    try {
      map.merge(context.firstKey(), context.original().get(context.firstKey()),
          (key, value) -> { throw new Error(); });
    } catch (Error e) {}
    assertThat(context, both(hasMissCount(0)).and(hasHitCount(0)));
    assertThat(context, both(hasLoadSuccessCount(0)).and(hasLoadFailureCount(1)));
    assertThat(map, is(equalTo(context.original())));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void merge_absent(Map<Integer, Integer> map, CacheContext context) {
    Integer result = map.merge(context.absentKey(), context.absentValue(), (key, value) -> value);
    assertThat(result, is(context.absentValue()));

    assertThat(map.get(context.absentKey()), is(context.absentValue()));
    assertThat(map.size(), is(1 + context.original().size()));
  }

  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void merge_sameValue(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      Integer value = context.original().get(key);
      assertThat(map.merge(key, -key, (k, v) -> k), is(value));
    }
    int count = context.firstMiddleLastKeys().size();
    assertThat(context, both(hasMissCount(0)).and(hasHitCount(0)));
    assertThat(context, both(hasLoadSuccessCount(count)).and(hasLoadFailureCount(0)));
    for (Integer key : context.firstMiddleLastKeys()) {
      Integer value = context.original().get(key);
      assertThat(map.get(key), is(value));
    }
    assertThat(map.size(), is(context.original().size()));
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.REPLACED));
  }

  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void merge_differentValue(Map<Integer, Integer> map, CacheContext context) {
    for (Integer key : context.firstMiddleLastKeys()) {
      assertThat(map.merge(key, key, (k, v) -> k + v), is(0));
    }
    int count = context.firstMiddleLastKeys().size();
    assertThat(context, both(hasMissCount(0)).and(hasHitCount(0)));
    assertThat(context, both(hasLoadSuccessCount(count)).and(hasLoadFailureCount(0)));

    for (Integer key : context.firstMiddleLastKeys()) {
      assertThat(map.get(key), is(0));
    }
    assertThat(map.size(), is(context.original().size()));
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.REPLACED));
  }

  /* ---------------- equals / hashCode -------------- */

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void equals_null(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.equals(null), is(false));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void equals_self(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.equals(map), is(true));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void equals(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.equals(context.original()), is(true));
    assertThat(context.original().equals(map), is(true));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void hashCode(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.hashCode(), is(equalTo(context.original().hashCode())));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void hashCode_self(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.hashCode(), is(equalTo(map.hashCode())));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = Population.EMPTY,
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void equalsAndHashCodeFail_empty(Map<Integer, Integer> map, CacheContext context) {
    Map<Integer, Integer> other = ImmutableMap.of(1, -1, 2, -2, 3, -3);
    assertThat(map.equals(other), is(false));
    assertThat(other.equals(map), is(false));
    assertThat(map.hashCode(), is(not(equalTo(other.hashCode()))));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = { Population.SINGLETON, Population.PARTIAL, Population.FULL },
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void equalsAndHashCodeFail_present(Map<Integer, Integer> map, CacheContext context) {
    Map<Integer, Integer> other = ImmutableMap.of(1, -1, 2, -2, 3, -3);
    assertThat(map.equals(other), is(false));
    assertThat(other.equals(map), is(false));
    assertThat(map.hashCode(), is(not(equalTo(other.hashCode()))));

    Map<Integer, Integer> empty = ImmutableMap.of();
    assertThat(map.equals(empty), is(false));
    assertThat(empty.equals(map), is(false));
    assertThat(map.hashCode(), is(not(equalTo(empty.hashCode()))));
  }

  /* ---------------- toString -------------- */

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void toString(Map<Integer, Integer> map, CacheContext context) {
    String toString = map.toString();
    if (!context.original().toString().equals(toString)) {
      for (Entry<Integer, Integer> entry : map.entrySet()) {
        assertThat(toString, containsString(entry.getKey() + "=" + entry.getValue()));
      }
    }
  }

  /* ---------------- Key Set -------------- */

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void keySetToArray_null(Map<Integer, Integer> map, CacheContext context) {
    map.keySet().toArray(null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void keySetToArray(Map<Integer, Integer> map, CacheContext context) {
    int length = context.original().size();

    Integer[] ints = map.keySet().toArray(new Integer[length]);
    assertThat(ints.length, is(length));
    assertThat(Arrays.asList(ints).containsAll(context.original().keySet()), is(true));

    Object[] array = map.keySet().toArray();
    assertThat(array.length, is(length));
    assertThat(Arrays.asList(array).containsAll(context.original().keySet()), is(true));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = Population.EMPTY,
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void keySet_whenEmpty(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.keySet(), is(deeplyEmpty()));
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = UnsupportedOperationException.class)
  public void keySet_addNotSupported(Map<Integer, Integer> map, CacheContext context) {
    map.keySet().add(1);
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches")
  public void keySet_clear(Map<Integer, Integer> map, CacheContext context) {
    map.keySet().clear();
    assertThat(map, is(emptyMap()));
    int count = context.original().size();
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.EXPLICIT));
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches")
  public void keySet(Map<Integer, Integer> map, CacheContext context) {
    Set<Integer> keys = map.keySet();
    assertThat(keys.contains(new Object()), is(false));
    assertThat(keys.remove(new Object()), is(false));
    assertThat(keys, hasSize(context.original().size()));
    for (Integer key : keys) {
      assertThat(keys.contains(key), is(true));
      assertThat(keys.remove(key), is(true));
      assertThat(keys.remove(key), is(false));
      assertThat(keys.contains(key), is(false));
    }
    assertThat(map, is(emptyMap()));
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches")
  public void keySet_iterator(Map<Integer, Integer> map, CacheContext context) {
    int iterations = 0;
    for (Iterator<Integer> i = map.keySet().iterator(); i.hasNext();) {
      assertThat(map.containsKey(i.next()), is(true));
      iterations++;
      i.remove();
    }
    assertThat(map, hasRemovalNotifications(context, iterations, RemovalCause.EXPLICIT));
    assertThat(iterations, is(context.original().size()));
    assertThat(map, is(emptyMap()));
  }

  @CheckNoStats
  @CacheSpec(population = Population.EMPTY,
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = IllegalStateException.class)
  public void keyIterator_noElement(Map<Integer, Integer> map, CacheContext context) {
    map.keySet().iterator().remove();
  }

  @CheckNoStats
  @CacheSpec(population = Population.EMPTY,
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NoSuchElementException.class)
  public void keyIterator_noMoreElements(Map<Integer, Integer> map, CacheContext context) {
    map.keySet().iterator().next();
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches")
  public void keySpliterator(Map<Integer, Integer> map, CacheContext context) {
    int[] count = new int[1];
    map.keySet().spliterator().forEachRemaining(key -> count[0]++);
    assertThat(count[0], is(map.size()));
  }

  /* ---------------- Values -------------- */

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void valuesToArray_null(Map<Integer, Integer> map, CacheContext context) {
    map.values().toArray(null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void valuesToArray(Map<Integer, Integer> map, CacheContext context) {
    int length = context.original().size();

    Integer[] ints = map.values().toArray(new Integer[length]);
    assertThat(ints.length, is(length));
    assertThat(Arrays.asList(ints).containsAll(context.original().values()), is(true));

    Object[] array = map.values().toArray();
    assertThat(array.length, is(length));
    assertThat(Arrays.asList(array).containsAll(context.original().values()), is(true));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = Population.EMPTY,
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void values_empty(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.values(), is(deeplyEmpty()));
  }

  @CheckNoStats
  @CacheSpec(population = Population.EMPTY,
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = UnsupportedOperationException.class)
  public void values_addNotSupported(Map<Integer, Integer> map, CacheContext context) {
    map.values().add(1);
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches")
  public void values_clear(Map<Integer, Integer> map, CacheContext context) {
    map.values().clear();
    assertThat(map, is(emptyMap()));
    int count = context.original().size();
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.EXPLICIT));
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches")
  public void values(Map<Integer, Integer> map, CacheContext context) {
    Collection<Integer> values = map.values();
    assertThat(values.contains(new Object()), is(false));
    assertThat(values.remove(new Object()), is(false));
    assertThat(values, hasSize(context.original().size()));
    for (Integer key : values) {
      assertThat(values.contains(key), is(true));
      assertThat(values.remove(key), is(true));
      assertThat(values.remove(key), is(false));
      assertThat(values.contains(key), is(false));
    }
    assertThat(map, is(emptyMap()));
    int count = context.original().size();
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.EXPLICIT));
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches")
  public void valueIterator(Map<Integer, Integer> map, CacheContext context) {
    int iterations = 0;
    for (Iterator<Integer> i = map.values().iterator(); i.hasNext();) {
      assertThat(map.containsValue(i.next()), is(true));
      iterations++;
      i.remove();
    }
    assertThat(map, hasRemovalNotifications(context, iterations, RemovalCause.EXPLICIT));
    assertThat(iterations, is(context.original().size()));
    assertThat(map, is(emptyMap()));
  }

  @CheckNoStats
  @CacheSpec(population = Population.EMPTY,
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = IllegalStateException.class)
  public void valueIterator_noElement(Map<Integer, Integer> map, CacheContext context) {
    map.values().iterator().remove();
  }

  @CheckNoStats
  @CacheSpec(population = Population.EMPTY,
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NoSuchElementException.class)
  public void valueIterator_noMoreElements(Map<Integer, Integer> map, CacheContext context) {
    map.values().iterator().next();
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches")
  public void valueSpliterator(Map<Integer, Integer> map, CacheContext context) {
    int[] count = new int[1];
    map.values().spliterator().forEachRemaining(key -> count[0]++);
    assertThat(count[0], is(map.size()));
  }

  /* ---------------- Entry Set -------------- */

  @CheckNoStats
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  public void entrySetToArray_null(Map<Integer, Integer> map, CacheContext context) {
    map.entrySet().toArray(null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void entriesToArray(Map<Integer, Integer> map, CacheContext context) {
    int length = context.original().size();

    Object[] ints = map.entrySet().toArray(new Object[length]);
    assertThat(ints.length, is(length));
    assertThat(Arrays.asList(ints).containsAll(context.original().entrySet()), is(true));

    Object[] array = map.entrySet().toArray();
    assertThat(array.length, is(length));
    assertThat(Arrays.asList(array).containsAll(context.original().entrySet()), is(true));
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(population = Population.EMPTY,
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  public void entrySet_empty(Map<Integer, Integer> map, CacheContext context) {
    assertThat(map.entrySet(), is(deeplyEmpty()));
  }

  @CheckNoStats
  @CacheSpec(population = Population.EMPTY, removalListener = Listener.DEFAULT)
  @Test(dataProvider = "caches", expectedExceptions = UnsupportedOperationException.class)
  public void entrySet_addIsNotSupported(Map<Integer, Integer> map, CacheContext context) {
    try {
      map.entrySet().add(immutableEntry(1, 2));
    } finally {
      assertThat(map.size(), is(0));
    }
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches")
  public void entrySet_clear(Map<Integer, Integer> map, CacheContext context) {
    map.entrySet().clear();
    assertThat(map, is(emptyMap()));
    int count = context.original().size();
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.EXPLICIT));
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches")
  public void entrySet(Map<Integer, Integer> map, CacheContext context) {
    Set<Entry<Integer, Integer>> entries = map.entrySet();
    assertThat(entries.contains(new Object()), is(false));
    assertThat(entries.remove(new Object()), is(false));
    assertThat(entries, hasSize(context.original().size()));
    for (Entry<Integer, Integer> entry : entries) {
      assertThat(entries.contains(entry), is(true));
      assertThat(entries.remove(entry), is(true));
      assertThat(entries.remove(entry), is(false));
      assertThat(entries.contains(entry), is(false));
    }
    assertThat(map, is(emptyMap()));
    int count = context.original().size();
    assertThat(map, hasRemovalNotifications(context, count, RemovalCause.EXPLICIT));
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches")
  public void entryIterator(Map<Integer, Integer> map, CacheContext context) {
    int iterations = 0;
    for (Iterator<Entry<Integer, Integer>> i = map.entrySet().iterator(); i.hasNext();) {
      Entry<Integer, Integer> entry = i.next();
      assertThat(map, hasEntry(entry.getKey(), entry.getValue()));
      iterations++;
      i.remove();
    }
    assertThat(map, hasRemovalNotifications(context, iterations, RemovalCause.EXPLICIT));
    assertThat(iterations, is(context.original().size()));
    assertThat(map, is(emptyMap()));
  }

  @CheckNoStats
  @CacheSpec(population = Population.EMPTY,
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = IllegalStateException.class)
  public void entryIterator_noElement(Map<Integer, Integer> map, CacheContext context) {
    map.entrySet().iterator().remove();
  }

  @CheckNoStats
  @CacheSpec(population = Population.EMPTY,
      removalListener = { Listener.DEFAULT, Listener.REJECTING })
  @Test(dataProvider = "caches", expectedExceptions = NoSuchElementException.class)
  public void entryIterator_noMoreElements(Map<Integer, Integer> map, CacheContext context) {
    map.entrySet().iterator().next();
  }

  @CacheSpec
  @CheckNoStats
  @Test(dataProvider = "caches")
  public void entrySetSpliterator(Map<Integer, Integer> map, CacheContext context) {
    int[] count = new int[1];
    map.entrySet().spliterator().forEachRemaining(key -> count[0]++);
    assertThat(count[0], is(map.size()));
  }

  /* ---------------- WriteThroughEntry -------------- */

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(implementation = Implementation.Caffeine,
      population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void writeThroughEntry(Map<Integer, Integer> map, CacheContext context) {
    Entry<Integer, Integer> entry = map.entrySet().iterator().next();

    entry.setValue(3);
    assertThat(map.get(entry.getKey()), is(3));
    assertThat(map.size(), is(context.original().size()));
    assertThat(map, hasRemovalNotifications(context, 1, RemovalCause.REPLACED));
  }

  @CheckNoStats
  @Test(dataProvider = "caches", expectedExceptions = NullPointerException.class)
  @CacheSpec(implementation = Implementation.Caffeine,
      population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void writeThroughEntry_null(Map<Integer, Integer> map, CacheContext context) {
    map.entrySet().iterator().next().setValue(null);
  }

  @CheckNoStats
  @Test(dataProvider = "caches")
  @CacheSpec(implementation = Implementation.Caffeine,
      population = { Population.SINGLETON, Population.PARTIAL, Population.FULL })
  public void writeThroughEntry_serialize(Map<Integer, Integer> map, CacheContext context) {
    Entry<Integer, Integer> entry = map.entrySet().iterator().next();
    Object copy = SerializableTester.reserialize(entry);
    assertThat(entry, is(equalTo(copy)));
  }
}
