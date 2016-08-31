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
 * Defines a convention to apply to the fields on a ClassModel.  This class is a convenience class to provide the iteration over the
 * ClassModel leaving only the field level logic to be implemented.  Users are free, of course, to implement {@link Convention} directly
 * if something more custom is needed.
 *
 * @since 3.4
 */
public abstract class FieldConvention implements Convention {
    @Override
    public void apply(final ClassModel.Builder model) {
        for (final Builder fieldModel : model.getFields()) {
            applyToField(model, fieldModel);
        }
    }

    /**
     * Apples the logic of the convention to the given FieldModel
     *
     * @param model the parent ClassModel.Builder
     * @param fieldModel the model to update
     */
    protected abstract void applyToField(final ClassModel.Builder model, final Builder fieldModel);

}
