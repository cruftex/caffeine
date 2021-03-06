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
package com.github.benmanes.caffeine.cache.tracing.async;

import java.io.Closeable;

import com.github.benmanes.caffeine.cache.tracing.TraceEvent;
import com.lmax.disruptor.EventHandler;

/**
 * An event handler that writes events to an output sink, such as a log file.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
public interface LogEventHandler extends EventHandler<TraceEvent>, Closeable {}
