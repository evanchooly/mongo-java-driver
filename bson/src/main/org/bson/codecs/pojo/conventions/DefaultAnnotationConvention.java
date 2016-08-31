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
import org.bson.codecs.pojo.conventions.annotations.Discriminator;
import org.bson.codecs.pojo.conventions.annotations.Entity;
import org.bson.codecs.pojo.conventions.annotations.Id;
import org.bson.codecs.pojo.conventions.annotations.Property;

import java.lang.annotation.Annotation;

/**
 * This defines an annotation driven convention allowing for precise, component-level configurations based on an abritrary set of
 * annotations.
 *
 * @since 3.4
 */
public class DefaultAnnotationConvention extends AnnotationConvention {
    @Override
    protected void processClassAnnotation(final ClassModel.Builder model, final Annotation annotation) {
        if (annotation instanceof Entity) {
            Entity entity = (Entity) annotation;

            model.useDiscriminator(entity.useDiscriminator());
            model.collection("".equals(entity.collection())
                          ? model.getType().getSimpleName()
                          : entity.collection());
        } else if (annotation instanceof Discriminator) {
            model.discriminator(((Discriminator) annotation).value());
        }
    }

    @Override
    protected void processFieldAnnotation(final ClassModel.Builder classModel, final Builder fieldModel, final Annotation annotation) {
        if (annotation instanceof Property) {
            Property property = (Property) annotation;
            if (!"".equals(property.value())) {
                fieldModel.documentFieldName(property.value());
            }
            fieldModel.useDiscriminator(property.useDiscriminator());
        } else if (annotation instanceof Id) {
            classModel.idField(fieldModel.getFieldName());
        }
    }

}
