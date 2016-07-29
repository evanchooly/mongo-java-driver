/*
 * Copyright (c) 2008-2016 MongoDB, Inc.
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

import org.bson.codecs.pojo.conventions.Convention;
import org.bson.codecs.pojo.conventions.naming.ClassNameCollectionNamingConvention;
import org.bson.codecs.pojo.conventions.naming.CollectionNamingConvention;
import org.bson.codecs.pojo.conventions.naming.JavaFieldNamingConvention;
import org.bson.codecs.pojo.conventions.naming.PropertyNamingConvention;

public class ConventionOptions {
    public enum CollectionNaming {
        CLASS_NAME(ClassNameCollectionNamingConvention.class),
        CUSTOM(null);

        private final Class<? extends CollectionNamingConvention> conventionClass;

        CollectionNaming(final Class<? extends CollectionNamingConvention> conventionClass) {
            this.conventionClass = conventionClass;
        }

    }
    public enum PropertyNaming {
        FIELD_NAME(JavaFieldNamingConvention.class),
        CUSTOM(null);

        private final Class<? extends PropertyNamingConvention> conventionClass;

        PropertyNaming(final Class<? extends PropertyNamingConvention> conventionClass) {
            this.conventionClass = conventionClass;
        }

    }
    private boolean storeEmptyFields;

    private boolean storeNullFields;
    private CollectionNaming collectionNaming;
    private PropertyNaming propertyNaming;
    private Class<? extends CollectionNamingConvention> collectionNamingConvention;

    private Class<? extends PropertyNamingConvention> propertyNamingConvention;
    public ConventionOptions storeEmptyFields(final boolean store) {
        storeEmptyFields = store;
        return this;
    }

    public ConventionOptions storeNullFields(final boolean store) {
        storeNullFields = store;
        return this;
    }

    public ConventionOptions collectionNamingStrategy(final CollectionNaming strategy) {
        if (strategy == CollectionNaming.CUSTOM) {
            throw new IllegalArgumentException("An implementation must be given with CUSTOM");
        }
        return collectionNamingStrategy(strategy, strategy.conventionClass);
    }

    public ConventionOptions collectionNamingStrategy(final CollectionNaming strategy,
                                                      final Class<? extends CollectionNamingConvention> impl) {
        this.collectionNaming = strategy;
        collectionNamingConvention = impl;
        return this;
    }

    public ConventionOptions propertyNamingStrategy(final PropertyNaming strategy) {
        if (strategy == PropertyNaming.CUSTOM) {
            throw new IllegalArgumentException("An implementation must be given with CUSTOM");
        }
        return propertyNamingStrategy(strategy, strategy.conventionClass);
    }

    public ConventionOptions propertyNamingStrategy(final PropertyNaming strategy, final Class<? extends PropertyNamingConvention> impl) {
        this.propertyNaming = strategy;
        propertyNamingConvention = impl;
        return this;
    }

    public boolean isStoreEmptyFields() {
        return storeEmptyFields;
    }


    public boolean isStoreNullFields() {
        return storeNullFields;
    }

    public CollectionNaming getCollectionNaming() {
        return collectionNaming;
    }

    public PropertyNaming getPropertyNaming() {
        return propertyNaming;
    }

    public Convention getCollectionNamingConvention() {
        try {
            return collectionNamingConvention.newInstance();
        } catch (Exception e) {
            throw new MappingException(e);
        }
    }

    public Convention getPropertyNamingConvention() {
        try {
            return propertyNamingConvention.newInstance();
        } catch (Exception e) {
            throw new MappingException(e);
        }
    }
}
