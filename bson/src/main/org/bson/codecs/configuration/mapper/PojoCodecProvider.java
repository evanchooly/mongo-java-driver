/*
 * Copyright 2008-2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bson.codecs.configuration.mapper;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides Codecs for various POJOs via the ClassModel abstractions.
 *
 * @since 3.4
 */
public final class PojoCodecProvider implements CodecProvider {
    private final Set<Class<?>> registered;

    /**
     * Creates a provider for a given set of classes.
     *
     * @param registered     the classes to use
     */
    PojoCodecProvider(final Set<Class<?>> registered) {
        this.registered = registered;
    }

    /**
     * Creates a Builder so classes can be registered before creating an immutable Provider.
     *
     * @return the Builder
     * @see Builder#register(Class[])
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        return registered.contains(clazz) ? new PojoCodec<T>(new ClassModel(registry, clazz), registry) : null;
    }

    /**
     * A Builder for the PojoCodecProvider
     */
    public static class Builder {
        private final Set<Class<?>> registered = new HashSet<Class<?>>();

        /**
         * Creates the PojoCodecProvider with the classes that have been registered.
         *
         * @return the Provider
         * @see #register(Class...)
         */
        public PojoCodecProvider build() {
            return new PojoCodecProvider(registered);
        }

        /**
         * Registers a class with the builder for inclusion in the Provider.
         *
         * @param classes the classes to register
         * @return this
         */
        public Builder register(final Class<?>... classes) {
            for (Class<?> aClass : classes) {
                registered.add(aClass);
            }
            return this;
        }
    }
}
