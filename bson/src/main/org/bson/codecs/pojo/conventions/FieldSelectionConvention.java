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

/**
 * Determines if static, transient, and final fields should be stored in MongoDB or not.  The default is to exclude all three cases.
 *
 * @since 3.4
 */
public class FieldSelectionConvention extends FieldConvention {
    private boolean storeFinal;
    private boolean storeTransient;
    private boolean storeStatic;

    /**
     * Sets whether to store final field values or not.
     *
     * @return true if final field values should be serialized to MongoDB
     */
    public boolean isStoreFinal() {
        return storeFinal;
    }

    /**
     * Sets whether to store static field values or not.
     *
     * @return true if static field values should be serialized to MongoDB
     */
    public boolean isStoreStatic() {
        return storeStatic;
    }

    /**
     * @return true if transient field values should be serialized to MongoDB
     */
    public boolean isStoreTransient() {
        return storeTransient;
    }

    /**
     * Sets whether to store final field values or not.
     *
     * @param store true if final field values should be serialized to MongoDB
     * @return this
     */
    public FieldSelectionConvention storeFinal(final boolean store) {
        this.storeFinal = store;
        return this;
    }

    /**
     * Sets whether to store static field values or not.
     *
     * @param store true if static field values should be serialized to MongoDB
     * @return this
     */
    public FieldSelectionConvention storeStatic(final boolean store) {
        this.storeStatic = store;
        return this;
    }

    /**
     * Sets whether to store transient field values or not.
     *
     * @param store true if transient field values should be serialized to MongoDB
     * @return this
     */
    public FieldSelectionConvention storeTransient(final boolean store) {
        this.storeTransient = store;
        return this;
    }

    @Override
    protected void applyToField(final ClassModel.Builder model, final Builder fieldModel) {
        if (!storeFinal && fieldModel.isFinal()) {
            fieldModel.include(false);
        }
        if (!storeTransient && fieldModel.isTransient()) {
            fieldModel.include(false);
        }
        if (!storeStatic && fieldModel.isStatic()) {
            fieldModel.include(false);
        }
    }
}
