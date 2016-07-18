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
package org.bson.codecs.configuration.mapper;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.members.ResolvedField;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

/**
 * Represents a field on a class and stores various metadata such as generic parameters
 *
 * @since 3.4
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class FieldModel {
    private final Field rawField;
    private final ClassModel owner;
    private final CodecRegistry registry;
    private final Class<?> type;
    private final Map<String, ResolvedType> boundTypes = new HashMap<String, ResolvedType>();
    private final String typeName;
    private final List<Class> types;

    private String name;
    private ShouldSerialize shouldSerialize = new DefaultShouldSerialize();
    private Codec<Object> codec;
    private boolean storeNulls;
    private boolean storeEmpties;
    private Boolean useDiscriminator;
    private boolean included = true;
    private boolean idField;
    private List<Annotation> annotations;

    /**
     * Create the FieldModel
     *
     * @param classModel the owning ClassModel
     * @param registry   the codec registry for this codec
     * @param field      the field to model
     */
    FieldModel(final ClassModel classModel, final CodecRegistry registry, final ResolvedField field) {
        this.type = field.getType().getErasedType();
        this.registry = registry;
        owner = classModel;
        rawField = field.getRawMember();
        rawField.setAccessible(true);
        name = field.getName();

        TypeBindings bindings = field.getType().getTypeBindings();
        for (int index = 0; index < bindings.size(); index++) {
            addTypeBinding(bindings.getBoundName(index), bindings.getBoundType(index));
        }
        typeName = getType().equals(Object.class) ? rawField.getGenericType().toString() : null;
        types = FieldModel.extract(field.getType());
        annotations = field.getAnnotations().asList();

        setIncluded(!(field.isFinal() || field.isStatic() || field.isTransient()));
    }

    /**
     * Copy constructor that changes the field type
     *
     * @param classModel the owner
     * @param original   the FieldModel to copy
     * @param type       the new type
     */
    FieldModel(final ClassModel classModel, final FieldModel original, final Class type) {
        this.type = type;
        rawField = original.rawField;
        owner = classModel;
        registry = original.registry;
        types = original.types;
        types.set(types.size() - 1, type);
        typeName = null;
        name = original.name;
        storeNulls = original.storeNulls;
        storeEmpties = original.storeEmpties;
        useDiscriminator = original.useDiscriminator;
        idField = original.idField;
        annotations = original.annotations;
        included = original.included;
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
     * Sets the field on entity with the given value
     *
     * @param entity  the entity to update
     * @param reader  The BsonReader to use
     * @param context the DecoderContext to use
     */
    public void decode(final Object entity, final BsonReader reader, final DecoderContext context) {
        set(entity, getCodec().decode(reader, context));
    }

    /**
     * This method encodes the field value to the BSON writer.
     *
     * @param writer         the BsonWriter to use if this field is included
     * @param entity         the entity from which to pull the value this FieldModel represents
     * @param encoderContext the encoding context
     */
    public void encode(final BsonWriter writer, final Object entity, final EncoderContext encoderContext) {
        if (isIncluded()) {
            Object value = get(entity);
            Codec<Object> fieldCodec = getCodec();
            if (shouldSerialize.evaluate(this, value)) {
                writer.writeName(getName());
                fieldCodec.encode(writer, value, encoderContext);
            }
        }
    }

    /**
     * Gets the value of the field from the given reference.
     *
     * @param entity the entity from which to pull the value
     * @return the value of the field
     */
    public Object get(final Object entity) {
        try {
            return rawField.get(entity);
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
        return rawField.getName();
    }

    /**
     * @return the name of the mapped field
     */
    public String getName() {
        return name;
    }

    /**
     * Suggests a value for the name with a particular weight.
     *
     * @param value the suggested value
     */
    public void setName(final String value) {
        renameField(value);
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
     * @return true if this field is the ID field for its ClassModel
     */
    public boolean isIdField() {
        return idField;
    }

    /**
     * Sets this field as the ID field or not.
     *
     * @param idField true if this field is the ID field
     */
    public void setIdField(final boolean idField) {
        if (idField) {
            this.idField = idField;
            renameField("_id");
        } else if (this.idField) {
            this.idField = false;
            renameField(getFieldName());
        }
    }

    /**
     * @return true if the field should included
     */
    public boolean isIncluded() {
        return included;
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
     * Sets whether to store empty container field values (List/Map/Set) or not.
     *
     * @param storeEmpties true if empty field values should be serialized to MongoDB
     */
    public void setStoreEmpties(final boolean storeEmpties) {
        this.storeEmpties = storeEmpties;
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
     * Sets whether to store null field values or not.
     *
     * @param storeNulls true if null field values should be serialized to MongoDB
     */
    public void setStoreNulls(final boolean storeNulls) {
        this.storeNulls = storeNulls;
    }

    /**
     * Determines if a discriminator should be used while saving this field's value to the database.  Only affects embedded entities.
     *
     * @return true if the discriminator should be used.  null/false otherwise.
     */
    public Boolean isUseDiscriminator() {
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
            rawField.set(entity, value);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Sets whether this MappedType is to be included when de/encoding BSON documents.
     *
     * @param include Whether to include this field in processing or not
     */
    public void setIncluded(final boolean include) {
        this.included = include;
    }

    /**
     * Instructs the {@link Codec} to use the discriminator
     *
     * @param useDiscriminator true if the discriminator should be used.
     */
    public void setUseDiscriminator(final Boolean useDiscriminator) {
        this.useDiscriminator = useDiscriminator;
    }

    @Override
    public String toString() {
        return format("%s#%s:%s", owner.getName(), getName(), getType().getName());
    }

    /**
     * @param typeName the parameterized type name
     * @return type bound for the given name
     */
    ResolvedType getBoundType(final String typeName) {
        return boundTypes.get(typeName);
    }

    /**
     * Binds a type name to an explicit type
     *
     * @param boundName the type/paramater name.  e.g., T
     * @param type      the type to be bound to the given name
     */
    void addTypeBinding(final String boundName, final ResolvedType type) {
        boundTypes.put(boundName, type);
    }

    void renameField(final String newName) {
        if (idField && !"_id".equals(newName)) {
            throw new CodecConfigurationException("ID fields can not be renamed.  They must be mapped as '_id.");
        }
        if (!newName.equals(name)) {
            Map<String, FieldModel> fieldMap = owner.getFieldMap();
            FieldModel fieldModel = fieldMap.get(newName);
            if (fieldModel != null && !equals(fieldModel)) {
                throw new CodecConfigurationException(
                    format("'%s' is already mapped to '%s'", newName, fieldModel.getFieldName()));
            }
            if (!name.equals(getFieldName())) {
                fieldMap.remove(name);
            }
            name = newName;
            fieldMap.put(newName, this);
        }
    }

    Codec<Object> getCodec() {
        if (codec == null) {
            codec = wrap(types);
        }
        return codec;
    }

    static List<Class> extract(final ResolvedType type) {
        List<Class> classes = new ArrayList<Class>();
        Class erasedType = type.getErasedType();
        if (Collection.class.isAssignableFrom(erasedType)) {
            ResolvedType collectionType = type.getTypeParameters().get(0);
            Class<? extends Object> containerClass;
            if (Set.class.equals(erasedType)) {
                containerClass = HashSet.class;
            } else if (List.class.equals(erasedType) || Collection.class.equals(erasedType)) {
                containerClass = ArrayList.class;
            } else {
                containerClass = erasedType;
            }
            classes.add(containerClass);
            classes.addAll(FieldModel.extract(collectionType));
        } else if (Map.class.isAssignableFrom(erasedType)) {
            List<ResolvedType> types = type.getTypeParameters();
            ResolvedType keyType = types.get(0);
            ResolvedType valueType = types.get(1);
            if (!keyType.getErasedType().equals(String.class)) {
                throw new CodecConfigurationException(format("Map key types must be Strings.  Found %s instead.", keyType.getErasedType()));
            }
            Class<?> containerClass;
            if (Map.class.equals(erasedType)) {
                containerClass = HashMap.class;
            } else {
                containerClass = erasedType;
            }
            classes.add(containerClass);
            classes.addAll(FieldModel.extract(valueType));
        } else {
            classes.add(type.getErasedType());
        }

        return classes;
    }

    Codec<Object> wrap(final List<Class> types) {
        Codec<Object> fieldCodec = null;
        Class first = types.get(0);
        List<Class> remainder = types.size() > 1 ? types.subList(1, types.size()) : Collections.<Class>emptyList();
        if (Collection.class.isAssignableFrom(first)) {
            fieldCodec = new CollectionCodec(first, wrap(remainder));
            shouldSerialize = new CollectionShouldSerialize();
        } else if (Map.class.isAssignableFrom(first)) {
            fieldCodec = new MapCodec(first, wrap(remainder));
            shouldSerialize = new MapShouldSerialize();
        } else {
            try {
                fieldCodec = registry.get(first);
                if (fieldCodec instanceof PojoCodec) {
                    fieldCodec = ((PojoCodec<Object>) fieldCodec).specialize(this);
                }
            } catch (final CodecConfigurationException e) {
                throw new CodecConfigurationException(format("Can not find codec for the field '%s' of type '%s'", name,
                                                             first.getSimpleName()), e);
            }
        }

        return fieldCodec;
    }
}
