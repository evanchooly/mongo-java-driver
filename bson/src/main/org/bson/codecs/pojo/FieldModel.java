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

import org.bson.codecs.configuration.CodecConfigurationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Represents a field on a class and stores various metadata such as generic parameters
 *
 * @since 3.4
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class FieldModel {
    private final Field field;
    private final Class<?> type;
    private final Map<String, Class<?>> boundTypes;
    private final String typeName;
    private final List<Class<?>> types;
    private final List<Annotation> annotations;
    private final String name;
    private final boolean storeNulls;
    private final boolean storeEmpties;
    private final boolean useDiscriminator;

    private ShouldSerialize shouldSerialize = new DefaultShouldSerialize();

    /**
     * Create the FieldModel
     *
     * @param field
     * @param name
     * @param type
     * @param storeNulls
     * @param storeEmpties
     * @param useDiscriminator
     * @param annotations
     */
    FieldModel(final Field field, final String name, final Class<?> type, final List<Class<?>> types,
               final String typeName, final boolean storeNulls, final boolean storeEmpties, final Boolean useDiscriminator,
               final List<Annotation> annotations, final Map<String, Class<?>> boundTypes) {
        this.type = type;
        this.types = types;
        this.boundTypes = boundTypes;
        this.field = field;
        this.storeNulls = storeNulls;
        this.storeEmpties = storeEmpties;
        this.useDiscriminator = useDiscriminator;
        this.name = name;
        this.annotations = annotations;
        this.typeName = typeName;

        if (field != null) {
            this.field.setAccessible(true);
        }
    }

    /*
     * Copy constructor that changes the field type
     *
     * @param classModel the owner
     * @param original   the FieldModel to copy
     * @param type       the new type
     */
    FieldModel(final FieldModel original, final Class type) {
        this.type = type;
        boundTypes = original.boundTypes;
        field = original.field;
        types = original.types;
        types.set(types.size() - 1, type);
        typeName = null;
        name = original.name;
        storeNulls = original.storeNulls;
        storeEmpties = original.storeEmpties;
        useDiscriminator = original.useDiscriminator;
        annotations = original.annotations;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> T createConcreteType(final Class encoderClass) {
        try {
            return (T) encoderClass.newInstance();
        } catch (final Exception e) {
            throw new CodecConfigurationException(e.getMessage(), e);
        }
    }

    /**
     * Creates a new Builder to specify the mapping data of a field
     *
     * @param name    the name of the field
     * @return the Builder instance
     */
    static Builder builder(final ClassModel.Builder parent, final String name) {
        return new Builder(parent, name);
    }

    /**
     * Creates a new Builder to specify the mapping data of a field
     *
     * @param field    the field
     * @return the Builder instance
     */
    static Builder builder(final ClassModel.Builder parent, final Field field) {
        return new Builder(parent, field);
    }

    /**
     * Gets the value of the field from the given reference.
     *
     * @param entity the entity from which to pull the value
     * @return the value of the field
     */
    public Object get(final Object entity) {
        try {
            return field.get(entity);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Returns the annotations defined on this field.
     *
     * @return the annotations
     */
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    /**
     * @return the unmapped field name as defined in the source file.
     */
    public String getFieldName() {
        return field.getName();
    }

    /**
     * @return the name of the mapped field
     */
    public String getName() {
        return name;
    }

    ShouldSerialize isShouldSerialize() {
        return shouldSerialize;
    }

    /**
     * @return the backing class for the MappedType
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * @return the name of the type mapped by this model
     */
    public String getTypeName() {
        return typeName;
    }

    public List<Class<?>> getTypes() {
        return types;
    }

    /**
     * @param annotation the annotation to find
     * @return true if the field is marked with the given annotation
     */
    public boolean hasAnnotation(final Class<? extends Annotation> annotation) {
        for (final Annotation a : getAnnotations()) {
            if (a.annotationType().equals(annotation)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Returns whether to store empty container field values (List/Map/Set) or not.
     *
     * @return true if empty field values should be serialized to MongoDB
     */
    public boolean isStoreEmpties() {
        return storeEmpties;
    }

    /**
     * Returns whether to store null field values or not.
     *
     * @return true if null field values should be serialized to MongoDB
     */
    public boolean isStoreNulls() {
        return storeNulls;
    }

    /**
     * Determines if a discriminator should be used while saving this field's value to the database.  Only affects embedded entities.
     *
     * @return true if the discriminator should be used.  false otherwise.
     */
    public boolean isUseDiscriminator() {
        return useDiscriminator;
    }

    /**
     * Sets the field with the given value.
     *
     * @param entity the entity to update
     * @param value  the value of the field
     */
    public void set(final Object entity, final Object value) {
        try {
            field.set(entity, value);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    void setShouldSerialize(final ShouldSerialize shouldSerialize) {
        this.shouldSerialize = shouldSerialize;
    }

    @Override
    public String toString() {
        return format("%s#%s:%s", field.getDeclaringClass().getName(), getName(), getType().getName());
    }

    /**
     * @param typeName the parameterized type name
     * @return type bound for the given name
     */
    Class<?> getBoundType(final String typeName) {
        return boundTypes.get(typeName);
    }

    /**
     * This class provides a Builder for configuring mapping data about a field on a type.
     */
    @SuppressWarnings({"rawtypes", "CheckStyle"})
    public static class Builder {
        private final ClassModel.Builder parent;
        private final String fieldName;
        private String typeName;
        private final Map<String, Class<?>> boundTypes = new HashMap<String, Class<?>>();
        private List<Annotation> annotations;

        private Field javaField;
        private String documentFieldName;
        private Class type;
        private List<Class<?>> parameters;
        private boolean included = true;
        private boolean useDiscriminator = true;
        private boolean storeEmptyFields = true;
        private boolean storeNullFields = true;


        Builder(final ClassModel.Builder parent, final String fieldName) {
            this.parent = parent;
            this.fieldName = fieldName;

            parent.addField(fieldName, this);
            documentFieldName(fieldName);

            annotations = emptyList();
        }

        Builder(final ClassModel.Builder parent, final Field javaField) {
            this(parent, javaField.getName());

            this.javaField = javaField;
            type = javaField.getType();
            annotations = asList(javaField.getAnnotations());
        }

        public Builder bindType(final String name, final Class<?> type) {
            boundTypes.put(name, type);
            return this;
        }

        /**
         * Sets the name of the field in the document stored in the database.
         *
         * @param name the name to use in the document
         *
         * @return this
         */
        public Builder documentFieldName(final String name) {
            if (!name.equals(fieldName) && parent.getField(name) != null) {
                throw new CodecConfigurationException(format("A naming collision has been found on %s between '%s' and '%s'",
                                                             parent.getType().getName(), fieldName, parent.getField(name).fieldName));
            }

            if (documentFieldName != null && !documentFieldName.equals(fieldName)) {
                parent.removeField(documentFieldName);
            }
            this.documentFieldName = name;
            parent.addField(documentFieldName, this);
            return this;
        }

        /**
         * @return the mapped name to use when storing this field in a document
         */
        public String getDocumentFieldName() {
            return documentFieldName;
        }

        public Builder storeEmptyFields(final boolean storeEmptyFields) {
            this.storeEmptyFields = storeEmptyFields;
            return this;
        }

        public Builder storeNullFields(final boolean storeNullFields) {
            this.storeNullFields = storeNullFields;
            return this;
        }

        public Builder type(final Class type) {
            this.type = type;
            return this;
        }

        public Builder type(final Class type, final List<Class<?>> parameters) {
            this.type = type;
            this.parameters = new ArrayList<Class<?>>(parameters);
            return this;
        }

        public Builder include(final boolean include) {
            this.included = include;
            return this;
        }
        public boolean isIncluded() {
            return this.included;
        }

        public Builder useDiscriminator(final boolean useDiscriminator) {
            this.useDiscriminator = useDiscriminator;
            return this;
        }

        public Builder typeName(final String name) {
            typeName = name;
            return this;
        }

        public Builder annotations(final Annotation[] annotations) {
            this.annotations = asList(annotations);
            return this;
        }

        /**
         * @return true if this field is marked as final
         */
        public boolean isFinal() {
            return javaField != null && Modifier.isFinal(javaField.getModifiers());
        }

        /**
         * @return true if this field is marked as transient
         */
        public boolean isTransient() {
            return javaField != null && Modifier.isTransient(javaField.getModifiers());
        }

        /**
         * @return true if this field is marked as static
         */
        public boolean isStatic() {
            return javaField != null && Modifier.isStatic(javaField.getModifiers());
        }

        public String getFieldName() {
            return fieldName;
        }

        public List<Annotation> annotations() {
            return annotations;
        }

        public FieldModel build() {
            return new FieldModel(
                javaField,
                documentFieldName,
                type,
                parameters,
                typeName,
                storeNullFields,
                storeEmptyFields,
                useDiscriminator,
                annotations,
                boundTypes
            );
        }

        @Override
        public String toString() {
            return format("FieldModel.Builder{javaField=%s}", javaField);
        }
    }
}
