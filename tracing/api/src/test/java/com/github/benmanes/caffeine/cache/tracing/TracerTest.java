/*
 * Copyright 2015 Ben Manes. All Rights Reserved.
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
package com.github.benmanes.caffeine.cache.tracing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.testng.annotations.Test;

/**
 * @author ben.manes@gmail.com (Ben Manes)
 */
public final class TracerTest {

  @Test
  public void serviceLoader() {
    Tracer defaultTracer = Tracer.getDefault();
    assertThat(defaultTracer, is(Tracer.disabled()));
  }

  @Test
  public void disabledTracer() {
    Tracer tracer = Tracer.disabled();
    long id = tracer.register("abc");
    tracer.recordRead(id, new Object());
    tracer.recordWrite(id, new Object(), 1);
    tracer.recordDelete(id, new Object());
  }
}
