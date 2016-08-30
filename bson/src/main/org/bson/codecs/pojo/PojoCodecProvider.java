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
package org.bson.codecs.pojo;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.conventions.Convention;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides Codecs for various POJOs via the ClassModel abstractions.
 *
 * @since 3.4
 */
@SuppressWarnings("rawtypes")
public final class PojoCodecProvider implements CodecProvider {
    private final Map<Class, ClassModel.Builder> builders;
    private final Set<Class<?>> registered;
    private final Set<String> packages;
    private final List<Convention> conventions;

    private PojoCodecProvider(final Map<Class, ClassModel.Builder> builders, final Set<Class<?>> registered, final Set<String> packages,
                             final List<Convention> conventions) {
        this.builders = builders;
        this.registered = registered;
        this.packages = packages;
        this.conventions = conventions;
    }

    /**
     * Creates a Builder so classes can be registered before creating an immutable Provider.  This Builder will use the default options
     * and Conventions.
     *
     * @return the Builder
     * @see Builder#register(Class[])
     */
    public static Builder builder() {
        return builder(ConventionOptions.DEFAULT_CONVENTIONS);
    }

    /**
     * Creates a Builder so classes can be registered before creating an immutable Provider.  This Builder will use the default
     * Conventions configured according to the given options.
     *
     * @param options the options to apply to class mapping
     * @return the Builder
     * @see Builder#register(Class[])
     */
    public static Builder builder(final ConventionOptions options) {
        ConventionOptions conventionOptions = options != null
                                              ? options
                                              : ConventionOptions.DEFAULT_CONVENTIONS;
        return new Builder(conventionOptions.getConventions());
    }

    /**
     * Creates a Builder so classes can be registered before creating an immutable Provider.  This Builder will use the default
     * Conventions configured according to the given options.
     *
     * @param conventions the Conventions to apply to class mapping
     * @return the Builder
     * @see Builder#register(Class[])
     */
    public static Builder builder(final List<Convention> conventions) {
        List<Convention> list = conventions;
        if (list == null || list.isEmpty()) {
            list = ConventionOptions.DEFAULT_CONVENTIONS.getConventions();
        }
        return new Builder(list);
    }

    @Override
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        if (registered.contains(clazz) || packages.contains(clazz.getPackage().getName())) {
            return new PojoCodec<T>(ClassModel.builder(clazz)
                                              .map()
                                              .apply(conventions).build(),
                                    registry);
        }
        ClassModel.Builder builder = builders.get(clazz);
        if (builder != null) {
            return new PojoCodec<T>(builder.apply(conventions).build(), registry);
        }
        return null;
    }

    /**
     * A Builder for the PojoCodecProvider
     */
    @SuppressWarnings("CheckStyle")
    public static final class Builder {
        private final Set<Class<?>> registered = new HashSet<Class<?>>();
        private final Set<String> packages = new HashSet<String>();
        private final List<Convention> conventions;
        private final Map<Class, ClassModel.Builder> builders = new HashMap<Class, ClassModel.Builder>();

        private Builder(final List<Convention> conventions) {
            this.conventions = conventions;
        }

        /**
         * Creates the PojoCodecProvider with the classes that have been registered.
         *
         * @return the Provider
         * @see #register(Class...)
         */
        public PojoCodecProvider build() {
            return new PojoCodecProvider(builders, registered, packages, conventions);
        }

        /**
         * Registers a class with the builder for inclusion in the Provider.  This will allow classes in the given packages to mapped
         * for use with PojoCodecProvider.
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

        public Builder register(final ClassModel.Builder... list) {
            for (ClassModel.Builder builder : list) {
                builders.put(builder.getType(), builder);
            }
            return this;
        }

        /**
         * Registers the packages of the given classes with the builder for inclusion in the Provider.  This will allow classes in the
         * given packages to mapped for use with PojoCodecProvider.
         *
         * @param classes classes in the packages to register
         * @return this
         */
        public Builder registerPackages(final Class... classes) {
            for (Class aClass : classes) {
                packages.add(aClass.getPackage().getName());
            }
            return this;
        }
        /**
         * Registers the packages of the given classes with the builder for inclusion in the Provider.  This will allow classes in the
         * given packages to mapped for use with PojoCodecProvider.
         *
         * @param names the package names to register
         * @return this
         */
        public Builder registerPackages(final String... names) {
            for (String name : names) {
                packages.add(name);
            }
            return this;
        }
    }
}
