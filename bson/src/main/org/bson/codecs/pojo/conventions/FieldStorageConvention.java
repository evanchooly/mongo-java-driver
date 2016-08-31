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

package org.bson.codecs.pojo.conventions;

import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.FieldModel.Builder;
import org.bson.codecs.pojo.conventions.annotations.Id;

/**
 * Determines if null and empty values should be stored in MongoDB or not.
 *
 * @since 3.4
 */
public class FieldStorageConvention extends FieldConvention {
    private boolean useNamedIdField;
    private boolean storeNulls;
    private boolean storeEmpties;

    /**
     * Designates that fields named 'id' or '_id' should be used as the ID field for a type.  When this value is true, the use of
     * {@link Id} is optional.  However, fields marked with {@link Id} will override this setting.
     *
     * @param value true if fields named 'id' or '_id' should be used as the ID field for a type
     * @return this
     */
    public FieldStorageConvention useNamedIdField(final boolean value) {
        useNamedIdField = value;
        return this;
    }

    /**
     * Sets whether to store null field values or not.
     *
     * @param value true if null field values should be serialized to MongoDB
     * @return this
     */
    public FieldStorageConvention storeNulls(final boolean value) {
        storeNulls = value;
        return this;
    }

    /**
     * Sets whether to store empty field values or not.
     *
     * @param value true if empty field values should be serialized to MongoDB
     * @return this
     */
    public FieldStorageConvention storeEmpties(final boolean value) {
        storeEmpties = value;
        return this;
    }

    /**
     * Returns whether to store null field values or not.
     * @return true if null field values should be serialized to MongoDB
     */
    public Boolean isStoreNulls() {
        return storeNulls;
    }

    /**
     * Returns whether to store empty container field values (List/Map/Set) or not.
     * @return true if empty field values should be serialized to MongoDB
     */
    public Boolean isStoreEmpties() {
        return storeEmpties;
    }

    /**
     * @return true if fields named 'id' or '_id' should be used as the ID field for a type
     */
    public boolean isUseNamedIdField() {
        return useNamedIdField;
    }

    @Override
    protected void applyToField(final ClassModel.Builder model, final Builder fieldModel) {
        fieldModel.storeNullFields(storeNulls);
        fieldModel.storeEmptyFields(storeEmpties);
        String fieldName = fieldModel.getFieldName();
        if (useNamedIdField && ("id".equals(fieldName) || "_id".equals(fieldName))) {
            model.idField(fieldName);
        }
    }
}
