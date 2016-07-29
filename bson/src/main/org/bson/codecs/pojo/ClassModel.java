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

import com.fasterxml.classmate.ResolvedType;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * This class represents the various generics and field metadata of a class for use in mapping data to and from the database.
 *
 * @since 3.4
 */
public final class ClassModel {
    private final Map<String, FieldModel> fieldMap = new HashMap<String, FieldModel>();
    private final List<FieldModel> fields = new ArrayList<FieldModel>();
    private  Class<?> type;
    private boolean generic;
    private FieldModel idField;
    private String collectionName;
    private Boolean useDiscriminator = true;
    private String discriminator;
    private Constructor<?> constructor;

    /**
     * Construct a ClassModel for the given Class.
     *
     * @param registry the registry to use for deferred lookups for codecs for the fields.
     * @param aClass   the Class to model
     */
    ClassModel(final CodecRegistry registry, final Class<?> aClass) {
/*
        TypeResolver resolver = new TypeResolver();
        this.type = aClass;

        MemberResolver memberResolver = new MemberResolver(resolver);
        ResolvedType resolved = resolver.resolve(getType());
        ResolvedTypeWithMembers type =
            memberResolver.resolve(resolved, new StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED), null);

        setCollectionName(getName());
        setDiscriminator(getType().getName());
        setUseDiscriminator(true);

        for (final ResolvedField field : type.getMemberFields()) {
            addField(new FieldModel(this, registry, field));
        }
*/
    }

    /**
     * Copy constructor that uses a {@link FieldModel} to specialize any unbound type parameters.
     *
     * @param original    the template to copy
     * @param sourceField the localized type bindings as found on a field
     */
    ClassModel(final ClassModel original, final FieldModel sourceField) {
        type = original.getType();
        useDiscriminator = original.useDiscriminator != null ? original.useDiscriminator : useDiscriminator;
        collectionName = original.collectionName != null ? original.collectionName : collectionName;
        discriminator = original.discriminator != null ? original.discriminator : discriminator;
        constructor = original.constructor;
        idField = original.idField;
        for (FieldModel fieldModel : original.fields) {
            ResolvedType boundType = sourceField.getBoundType(fieldModel.getTypeName());
            if (boundType != null) {
                fieldModel = new FieldModel(this, fieldModel, boundType.getErasedType());
            }
            addField(fieldModel);
        }
        if (sourceField.isUseDiscriminator() != null) {
            setUseDiscriminator(sourceField.isUseDiscriminator());
        }
    }

    /**
     * Gets the annotation of the specified type.
     *
     * @param <T>    the type of the annotation
     * @param aClass the annotation type to find
     * @return the annotation on the MappedType or null if it doesn't exist
     */
    public <T extends Annotation> T getAnnotation(final Class<T> aClass) {
        return getType().getAnnotation(aClass);
    }

    /**
     * Gets the annotations on this type.
     *
     * @return the annotations on the class
     */
    public Annotation[] getAnnotations() {
        return getType().getAnnotations();
    }

    void addField(final FieldModel model) {
        fieldMap.put(model.getName(), model);
        fields.add(model);
        if (model.getTypeName() != null) {
            generic = true;
        }
        if (idField == null
            && ("id".equals(model.getFieldName())
            || "_id".equals(model.getFieldName())
            || "_id".equals(model.getName()))) {
            idField = model;
            idField.setIdField(true);
        }
    }

    /**
     * Returns the collection name to use when de/encoding BSON documents.
     *
     * @return the collection name
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * @return the backing class for the MappedType
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Suggests a new value for the collection name.
     *
     * @param collectionName the new collection name to suggest
     */
    public void setCollectionName(final String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * Gets the value for the discriminator.
     *
     * @return the discriminator value
     */
    public String getDiscriminator() {
        return discriminator;
    }

    /**
     * Sets a new value for the discriminator.
     *
     * @param discriminator the new discriminator value to suggest
     */
    public void setDiscriminator(final String discriminator) {
        this.discriminator = discriminator;
    }

    /**
     * Retrieves a specific field from the model.
     *
     * @param name the field's name
     * @return the field
     */
    public FieldModel getField(final String name) {
        return fieldMap.get(name);
    }

    /**
     * Returns all the fields on this model
     *
     * @return the list of fields
     */
    public List<FieldModel> getFields() {
        return Collections.unmodifiableList(fields);
    }

    /**
     * Returns the {@link FieldModel} mapped as the ID field for this ClassModel
     *
     * @return the FieldModel for the ID
     */
    public FieldModel getIdField() {
        return idField;
    }

    /**
     * Sets the FieldModel by name for the ID of this ClassModel
     *
     * @param name the name of the field to use as the ID
     */
    public void setIdField(final String name) {
        FieldModel fieldModel = fieldMap.get(name);
        if (fieldModel == null) {
            throw new CodecConfigurationException(format("No such field '%s' for type %s", name, getType().getName()));
        }
        this.idField = fieldModel;
    }

    /**
     * Returns the name of the class represented by this ClassModel
     *
     * @return the name
     */
    public String getName() {
        return getType().getSimpleName();
    }

    /**
     * Checks if this class is annotated with the given annotation.
     *
     * @param aClass the annotation to check for
     * @return true if the class is annotated with this type
     */
    public boolean hasAnnotation(final Class<? extends Annotation> aClass) {
        return getAnnotation(aClass) != null;
    }

    /**
     * Determines if this ClassModel has any unbound type parameters.
     *
     * @return true if for any named type paramater, the bound type is {@link java.lang.Object}
     */
    public boolean isGeneric() {
        return generic;
    }

    /**
     * If true, the discriminator for this type is included when serializing this type.
     *
     * @return true if the discriminator should be included
     */
    public Boolean isUseDiscriminator() {
        return useDiscriminator;
    }

    /**
     * If true, the discriminator for this type is included when serializing this type.
     *
     * @param useDiscriminator true if the discriminator should be included
     */
    public void setUseDiscriminator(final Boolean useDiscriminator) {
        this.useDiscriminator = useDiscriminator;
    }

    Constructor<?> getConstructor() {
        if (constructor == null) {
            try {
                constructor = getType().getConstructor();
                constructor.setAccessible(true);
            } catch (final NoSuchMethodException e) {
                throw new CodecConfigurationException("No zero arugment constructor was found for the type " + getType().getName());
            }
        }
        return constructor;
    }

    Map<String, FieldModel> getFieldMap() {
        return fieldMap;
    }

    @Override
    public String toString() {
        return format("ClassModel<%s>", getName());
    }

    @SuppressWarnings("CheckStyle")
    public static class ClassModelBuilder {
        private ClassModelBuilder parent;
        private final Class<?> type;
        private final List<FieldModel.FieldModelBuilder> fields = new ArrayList<FieldModel.FieldModelBuilder>();
        private String collection;
        private Boolean useDiscriminator = true;
        private String discriminator;
        private List<Annotation> annotations;

        public static ClassModelBuilder builder(final Class<?> type) {
            return new ClassModelBuilder(type);
        }

        ClassModelBuilder(final ClassModelBuilder parent, final Class<?> type) {
            this(type);
            this.parent = parent;
        }

        ClassModelBuilder(final Class<?> type) {
            this.type = type;
            collection = type.getSimpleName();
            annotations = asList(type.getAnnotations());
        }

        public String getTypeName() {
            return type.getSimpleName();
        }

        public FieldModel.FieldModelBuilder addField(final String name) {
            FieldModel.FieldModelBuilder field = new FieldModel.FieldModelBuilder(this, type, name);
            fields.add(field);
            return field;
        }

        public List<Annotation> getAnnotations() {
            return annotations;
        }

        public List<FieldModel.FieldModelBuilder> getFields() {
            return fields;
        }

        public Class<?> getType() {
            return type;
        }

        public ClassModelBuilder collection(final String value) {
            this.collection = value;
            return this;
        }

        public ClassModelBuilder discriminator(final String value) {
            return this;
        }
        public ClassModelBuilder useDiscriminator(final Boolean value) {
            return this;
        }

        public ClassModelBuilder subclass(final Class<?> type) {
            return new ClassModelBuilder(this, type);
        }

        public ClassModel build() {
            throw new UnsupportedOperationException();
        }
    }
}
