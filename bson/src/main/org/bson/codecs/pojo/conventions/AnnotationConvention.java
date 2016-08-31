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
import org.bson.codecs.pojo.conventions.annotations.Entity;

import java.lang.annotation.Annotation;

/**
 * This defines an annotation driven convention allowing for precise, component-level configurations based on an abritrary set of
 * annotations.
 *
 * @since 3.4
 */
public abstract class AnnotationConvention implements Convention {
    @Override
    public void apply(final ClassModel.Builder model) {
        for (final Annotation annotation : model.getAnnotations()) {
            processClassAnnotation(model, annotation);
        }

        for (final Builder field : model.getFields()) {
            for (final Annotation annotation : field.annotations()) {
                processFieldAnnotation(model, field, annotation);
            }
        }
    }

    protected abstract void processClassAnnotation(final ClassModel.Builder model, final Annotation annotation);

    protected abstract void processFieldAnnotation(final ClassModel.Builder classModel, final Builder fieldModel,
                                                   final Annotation annotation);
}
