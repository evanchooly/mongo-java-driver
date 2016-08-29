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

import com.fasterxml.classmate.AnnotationConfiguration.StdConfiguration;
import com.fasterxml.classmate.AnnotationInclusion;
import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedField;
import org.bson.codecs.IdGenerator;
import org.bson.codecs.ObjectIdGenerator;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.pojo.FieldModel.Builder;
import org.bson.codecs.pojo.conventions.Convention;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final Class<?> type;
    private final boolean generic;
    private final FieldModel idField;
    private final String collectionName;
    private final Boolean useDiscriminator;
    private final String discriminator;
    private final Constructor<?> constructor;
    private final IdGenerator idGenerator;

    /**
     * Construct a ClassModel for the given Class.
     *
     * @param aClass   the Class to model
     */
    private ClassModel(final Class<?> aClass, final boolean generic, final String collectionName,
                       final Boolean useDiscriminator, final String discriminator, final IdGenerator idGenerator,
                       final FieldModel idField, final List<FieldModel> fields) {
        this.generic = generic;
        this.idField = idField;
        this.collectionName = collectionName;
        this.useDiscriminator = useDiscriminator;
        this.discriminator = discriminator;
        this.idGenerator = idGenerator;
        this.type = aClass;

        try {
            constructor = getType().getConstructor();
            constructor.setAccessible(true);
        } catch (final NoSuchMethodException e) {
            throw new CodecConfigurationException("No zero arugment constructor was found for the type " + getType().getName());
        }

        for (FieldModel field : fields) {
            addField(field);
        }
    }

    /*
     * Copy constructor that uses a {@link FieldModel} to specialize any unbound type parameters.
     *
     * @param original    the template to copy
     * @param sourceField the localized type bindings as found on a field
     */
    ClassModel(final ClassModel original, final FieldModel sourceField) {
        type = original.getType();
        generic = original.generic;
        idGenerator = original.idGenerator;
        collectionName = original.collectionName != null ? original.collectionName : getName();
        discriminator = original.discriminator != null ? original.discriminator : getType().getName();
        constructor = original.constructor;
        idField = original.idField;
        for (FieldModel fieldModel : original.fields) {
            Class<?> boundType = sourceField.getBoundType(fieldModel.getTypeName());
            if (boundType != null) {
                fieldModel = new FieldModel(fieldModel, boundType);
            }
            addField(fieldModel);
        }

        useDiscriminator = sourceField.isUseDiscriminator();
    }
    /**
     * Creates a Builder to build and configure a ClassModel
     *
     * @param type     the type to model
     * @return the builder
     */
    public static Builder builder(final Class<?> type) {
        return new Builder(type);
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

    /**
     * @return the generator to use when creating IDs automatically
     */
    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    void addField(final FieldModel model) {
        fieldMap.put(model.getName(), model);
        fields.add(model);
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
     * Gets the value for the discriminator.
     *
     * @return the discriminator value
     */
    public String getDiscriminator() {
        return discriminator;
    }

    /**
     * Retrieves a specific field from the model.
     *
     * @param name the field's name
     * @return the field
     */
    public FieldModel getField(final String name) {
        return idField != null && name.equals(idField.getName()) ? idField : fieldMap.get(name);
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
     * @return true if for any named type paramater, the bound type is {@link Object}
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

    Constructor<?> getConstructor() {
        return constructor;
    }

    @Override
    public String toString() {
        return format("ClassModel<%s>", getName());
    }

    /**
     * This class provides a Builder for configuring mapping data about a type.
     */
    public static final class Builder {
//        private Builder parent;
        private final Class<?> type;
        private final Map<String, FieldModel.Builder> fields = new LinkedHashMap<String, FieldModel.Builder>();
        private final List<Annotation> annotations;

        private String collection;
        private boolean useDiscriminator = true;
        private String discriminator;
        private IdGenerator idGenerator = new ObjectIdGenerator();
        private String idField;

/*
        private Builder(final Builder parent, final Class<?> type) {
            this(parent.registry, type);
            this.parent = parent;
        }
*/

        private Builder(final Class<?> type) {
            this.type = type;
            collection = type.getSimpleName();
            annotations = asList(type.getAnnotations());
        }

        static List<Class<?>> extract(final ResolvedType type) {
            List<Class<?>> classes = new ArrayList<Class<?>>();
            Class<?> erasedType = type.getErasedType();
            if (Collection.class.isAssignableFrom(erasedType)) {
                ResolvedType collectionType = type.getTypeParameters().get(0);
                Class<?> containerClass;
                if (Set.class.equals(erasedType)) {
                    containerClass = HashSet.class;
                } else if (List.class.equals(erasedType) || Collection.class.equals(erasedType)) {
                    containerClass = ArrayList.class;
                } else {
                    containerClass = erasedType;
                }
                classes.add(containerClass);
                classes.addAll(extract(collectionType));
            } else if (Map.class.isAssignableFrom(erasedType)) {
                List<ResolvedType> types = type.getTypeParameters();
                ResolvedType keyType = types.get(0);
                ResolvedType valueType = types.get(1);
                if (!keyType.getErasedType().equals(String.class)) {
                    throw new CodecConfigurationException(format("Map key types must be Strings.  Found %s instead.",
                                                                 keyType.getErasedType()));
                }
                Class<?> containerClass;
                if (Map.class.equals(erasedType)) {
                    containerClass = HashMap.class;
                } else {
                    containerClass = erasedType;
                }
                classes.add(containerClass);
                classes.addAll(extract(valueType));
            } else {
                classes.add(type.getErasedType());
            }

            return classes;
        }

        /**
         * Returns the builder for the named FieldModel
         *
         * @param name the field to lookup
         * @return the builder
         */
        public FieldModel.Builder field(final String name) {
            FieldModel.Builder builder = fields.get(name);
            if (builder == null) {
                for (FieldModel.Builder field : fields.values()) {
                    if (field.getDocumentFieldName().equals(name)) {
                        return field;
                    }
                }
            }
            return builder;
        }

        /**
         * Gets a field by the given name.
         *
         * @param name the name of the field to find
         * @return the field
         */
        public FieldModel.Builder getField(final String name) {
            return fields.get(name);
        }

        FieldModel.Builder removeField(final String name) {
            return fields.remove(name);
        }
        FieldModel.Builder addField(final String name, final FieldModel.Builder field) {
            return fields.put(name, field);
        }

        /**
         * @return the name of the type being modeled
         */
        public String getTypeName() {
            return type.getSimpleName();
        }

        /**
         * Adds a new field
         *
         * @param name the name of the new field
         * @return the builder to configure the field being modeled
         */
        public FieldModel.Builder addField(final String name) {
            return FieldModel.builder(this, name);
        }

        /**
         * Adds a new field
         *
         * @param field the new field
         * @return the builder to configure the field being modeled
         */
        public FieldModel.Builder addField(final Field field) {
            return FieldModel.builder(this, field);
        }

        /**
         * @return the annotations on the modeled type
         */
        public List<Annotation> getAnnotations() {
            return annotations;
        }

        /**
         * @return the fields on the modeled type
         */
        public List<FieldModel.Builder> getFields() {
            return new ArrayList<FieldModel.Builder>(fields.values());
        }

        /**
         * @return the type
         */
        public Class<?> getType() {
            return type;
        }

        /**
         * Sets the collection to be used when storing instances of the modeled type
         *
         * @param value the collection name
         * @return this
         */
        public Builder collection(final String value) {
            this.collection = value;
            return this;
        }

        /**
         * Sets the discriminator to be used when storing instances of the modeled type
         *
         * @param value the discriminator value
         * @return this
         */
        public Builder discriminator(final String value) {
            discriminator = value;
            return this;
        }

        /**
         * Sets the discriminator to be used when storing instances of the modeled type
         *
         * @param value the discriminator value
         * @return this
         */
        public Builder useDiscriminator(final Boolean value) {
            useDiscriminator = value;
            return this;
        }

/*
        public Builder subclass(final Class<?> type) {
            return new Builder(this, type);
        }
*/

        /**
         * Designates a field as the ID field for this type.  If another field is currently marked as the ID field, that setting is
         * cleared in favor of the named field.
         *
         * @param name the name of the ID field
         * @return this
         * @throws CodecConfigurationException if the named field can not be found
         */
        public Builder idField(final String name) {
            FieldModel.Builder field = fields.get(name);
            if (field != null) {
                idField = name;
            } else {
                throw new CodecConfigurationException(format("The named field '%s' can not be found on '%s'.", name, this.type.getName()));
            }
            return this;
        }

        /**
         * Specifies the generator to use when generating new IDs automatically when saving to the database.
         *
         * @param generator the generator to use
         * @return this
         */
        public Builder idGenerator(final IdGenerator generator) {
            idGenerator = generator;
            return this;
        }
        /**
         * This method automatically discovers all the necessary information for mapping this type and its fields.
         *
         * @return this
         */
        public Builder map() {
            TypeResolver resolver = new TypeResolver();

            MemberResolver memberResolver = new MemberResolver(resolver);
            ResolvedType resolved = resolver.resolve(type);
            ResolvedTypeWithMembers resolvedType =
                memberResolver.resolve(resolved, new StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED), null);

            collection(type.getSimpleName());
            discriminator(type.getName());
            useDiscriminator(true);

            for (final ResolvedField field : resolvedType.getMemberFields()) {
                FieldModel.Builder builder = map(field);
                fields.put(builder.getFieldName(), builder);
            }

            return this;
        }

        private FieldModel.Builder map(final ResolvedField resolvedField) {
            Field rawField = resolvedField.getRawMember();
            Class<?> erasedType = resolvedField.getType().getErasedType();

            FieldModel.Builder fieldBuilder =
                addField(rawField)
                    .type(erasedType, extract(resolvedField.getType()))
                    .typeName(erasedType.equals(Object.class) ? rawField.getGenericType().toString() : null)
                    .annotations(resolvedField.getAnnotations().asArray());

            TypeBindings bindings = resolvedField.getType().getTypeBindings();

            for (int index = 0; index < bindings.size(); index++) {
                fieldBuilder.bindType(bindings.getBoundName(index), bindings.getBoundType(index).getErasedType());
            }

            return fieldBuilder;
        }

        /**
         * Applies the list of Conventions to this ClassModel
         *
         * @param conventions the conventions to apply
         * @return this
         */
        public Builder apply(final List<Convention> conventions) {
            if (conventions != null) {
                for (Convention convention : conventions) {
                    convention.apply(this);
                }
            }
            return this;
        }

        /**
         * Creates a new ClassModel instance based on the mapping data provided.
         *
         * @return the new instance
         */
        public ClassModel build() {
            boolean generic = false;
            List<FieldModel> fieldModels = new ArrayList<FieldModel>();
            FieldModel idFieldModel = null;
            for (FieldModel.Builder field : fields.values()) {
                if (field.isIncluded()) {
                    FieldModel model = field.build();
                    generic |= model.getTypeName() != null;
                    if (model.getFieldName().equals(idField)) {
                        idFieldModel = model;
                    } else {
                        fieldModels.add(model);
                    }
                }
            }
            return new ClassModel(type, generic, collection, useDiscriminator, discriminator, idGenerator, idFieldModel, fieldModels);
        }

        @Override
        public String toString() {
            return format("ClassModel.Builder{type=%s, collection=%s}", type, collection);
        }
    }
}
