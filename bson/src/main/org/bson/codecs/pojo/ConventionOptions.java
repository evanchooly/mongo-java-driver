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

import org.bson.codecs.pojo.conventions.AnnotationConvention;
import org.bson.codecs.pojo.conventions.Convention;
import org.bson.codecs.pojo.conventions.DefaultAnnotationConvention;
import org.bson.codecs.pojo.conventions.FieldSelectionConvention;
import org.bson.codecs.pojo.conventions.FieldStorageConvention;
import org.bson.codecs.pojo.conventions.naming.ClassNameCollectionNamingConvention;
import org.bson.codecs.pojo.conventions.naming.CollectionNamingConvention;
import org.bson.codecs.pojo.conventions.naming.JavaFieldNamingConvention;
import org.bson.codecs.pojo.conventions.naming.PropertyNamingConvention;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("CheckStyle")
public class ConventionOptions {
    public static final ConventionOptions DEFAULT_CONVENTIONS = builder().build();

    private final boolean useNamedIdField;
    private final boolean storeEmptyFields;
    private final boolean storeNullFields;
    private final boolean storeFinalFields;
    private final boolean storeTransientFields;
    private final boolean storeStaticFields;
    private final CollectionNamingConvention collectionNamingConvention;
    private final PropertyNamingConvention propertyNamingConvention;
    private final AnnotationConvention annotationConvention;

    private ConventionOptions(final boolean storeEmptyFields, final boolean storeNullFields, final boolean storeFinalFields,
                              final boolean storeTransientFields, final boolean storeStaticFields, final boolean useNamedIdField,
                              final CollectionNamingConvention collectionNamingConvention,
                              final PropertyNamingConvention propertyNamingConvention, final AnnotationConvention annotationConvention) {
        this.storeEmptyFields = storeEmptyFields;
        this.storeNullFields = storeNullFields;
        this.storeFinalFields = storeFinalFields;
        this.storeTransientFields = storeTransientFields;
        this.storeStaticFields = storeStaticFields;
        this.useNamedIdField = useNamedIdField;
        this.collectionNamingConvention = collectionNamingConvention;
        this.propertyNamingConvention = propertyNamingConvention;
        this.annotationConvention = annotationConvention;
    }

    public Convention getCollectionNamingConvention() {
        return collectionNamingConvention;
    }

    public Convention getPropertyNamingConvention() {
        return propertyNamingConvention;
    }

    public boolean isStoreEmptyFields() {
        return storeEmptyFields;
    }

    public boolean isStoreFinalFields() {
        return storeFinalFields;
    }

    public boolean isStoreNullFields() {
        return storeNullFields;
    }

    public boolean isStoreStaticFields() {
        return storeStaticFields;
    }

    public boolean isStoreTransientFields() {
        return storeTransientFields;
    }

    public boolean isUseNamedIdField() {
        return useNamedIdField;
    }

    protected List<Convention> getConventions() {
        List<Convention> conventions = new ArrayList<Convention>();

        conventions.add(new FieldSelectionConvention()
                            .storeFinal(storeFinalFields)
                            .storeStatic(storeStaticFields)
                            .storeTransient(storeTransientFields));

        conventions.add(new FieldStorageConvention()
                            .storeNulls(storeNullFields)
                            .storeEmpties(storeEmptyFields)
                            .useNamedIdField(useNamedIdField));

        conventions.add(collectionNamingConvention);
        conventions.add(propertyNamingConvention);
        conventions.add(annotationConvention);
        return conventions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean useNamedIdField = true;
        private boolean storeEmptyFields;
        private boolean storeNullFields;
        private boolean storeFinalFields;
        private boolean storeTransientFields;
        private boolean storeStaticFields;
        private CollectionNamingConvention collectionNamingConvention = new ClassNameCollectionNamingConvention();
        private PropertyNamingConvention propertyNamingConvention = new JavaFieldNamingConvention();
        private AnnotationConvention annotationConvention = new DefaultAnnotationConvention();

        private Builder() {}

        public ConventionOptions build() {
            return new ConventionOptions(storeEmptyFields, storeNullFields, storeFinalFields, storeTransientFields, storeStaticFields,
                                         useNamedIdField, collectionNamingConvention, propertyNamingConvention, annotationConvention);
        }

        public Builder annotations(final AnnotationConvention convention) {
            annotationConvention = convention;
            return this;
        }

        public Builder collectionNaming(final CollectionNamingConvention impl) {
            collectionNamingConvention = impl;
            return this;
        }

        public Builder propertyNaming(final PropertyNamingConvention impl) {
            propertyNamingConvention = impl;
            return this;
        }

        public Builder storeEmptyFields(final boolean store) {
            storeEmptyFields = store;
            return this;
        }

        public Builder storeFinalFields(final boolean store) {
            storeFinalFields = store;
            return this;
        }

        public Builder storeNullFields(final boolean store) {
            storeNullFields = store;
            return this;
        }

        public Builder storeStaticFields(final boolean store) {
            storeStaticFields = store;
            return this;
        }

        public Builder storeTransientFields(final boolean store) {
            storeTransientFields = store;
            return this;
        }

        public Builder useNamedIdField(final boolean store) {
            useNamedIdField = store;
            return this;
        }

    }
}
