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

import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * Provides the encoding and decoding logic for a POJO.
 *
 * @param <T> the type to encode/decode
 * @since 3.4
 */
@SuppressWarnings("unchecked")
final class PojoCodec<T> implements CollectibleCodec<T> {
    private final CodecRegistry registry;
    private final ClassModel classModel;
    private final Map<FieldModel, Codec<Object>> fieldCodecs = new HashMap<FieldModel, Codec<Object>>();

    /**
     * Creates a Codec for the ClassModel
     *
     * @param model    the model for this codec
     * @param registry the codec registry for this codec
     */
    PojoCodec(final ClassModel model, final CodecRegistry registry) {
        this.registry = registry;
        this.classModel = model;
    }

    PojoCodec(final PojoCodec<T> original, final FieldModel fieldModel) {
        registry = original.registry;
        classModel = new ClassModel(original.classModel, fieldModel);
    }

    /**
     * Decode an entity applying the passed options.
     *
     * @param reader         the BSON reader
     * @param decoderContext the decoder context
     * @return an instance of the type parameter {@code T}.
     */
    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        if (BsonType.NULL.equals(reader.getCurrentBsonType())) {
            reader.readNull();
            return null;
        } else {
            Codec<T> codec = findActualCodec(reader);
            T entity;
            if (codec instanceof PojoCodec) {
                PojoCodec<T> modelCodec = (PojoCodec<T>) codec;
                entity = modelCodec.createInstance();
                reader.readStartDocument();
                modelCodec.readFields(entity, reader, decoderContext);
                reader.readEndDocument();
            } else {
                entity = codec.decode(reader, decoderContext);
            }

            return entity;
        }
    }

    @Override
    public void encode(final BsonWriter writer, final Object entity, final EncoderContext encoderContext) {
        if (entity == null) {
            writer.writeNull();
        } else {
            Codec<T> codec = (Codec<T>) findActualCodec(entity.getClass());
            if (codec instanceof PojoCodec) {
                writer.writeStartDocument();
                PojoCodec<?> modelCodec = (PojoCodec<?>) codec;
                ClassModel classModel = modelCodec.getClassModel();
                if (classModel.getIdField() != null && classModel.getIdField().get(entity) != null) {
                    encode(classModel.getIdField(), writer, entity, encoderContext);
                }
                if (classModel.isUseDiscriminator()) {
                    writer.writeString("_t", classModel.getDiscriminator());
                }
                for (FieldModel fieldModel : classModel.getFields()) {
                    encode(fieldModel, writer, entity, encoderContext);
                }
                writer.writeEndDocument();
            } else {
                codec.encode(writer, (T) entity, encoderContext);
            }
        }
    }

    /**
     * This method encodes the field value to the BSON writer.
     *
     * @param writer         the BsonWriter to use if this field is included
     * @param entity         the entity from which to pull the value this FieldModel represents
     * @param encoderContext the encoding context
     */
    private void encode(final FieldModel model, final BsonWriter writer, final Object entity, final EncoderContext encoderContext) {
        Object value = model.get(entity);
        Codec<Object> fieldCodec = getCodec(model);
        if (model.isShouldSerialize().evaluate(model, value)) {
            writer.writeName(model.getName());
            fieldCodec.encode(writer, value, encoderContext);
        }
    }

    /**
     * Sets the field on entity with the given value
     *
     * @param entity  the entity to update
     * @param reader  The BsonReader to use
     * @param context the DecoderContext to use
     */
    public void decode(final FieldModel model, final Object entity, final BsonReader reader, final DecoderContext context) {
        model.set(entity, getCodec(model).decode(reader, context));
    }


    private Codec<Object> getCodec(final FieldModel model) {

        Codec<Object> codec = fieldCodecs.get(model);
        if (codec == null) {
            codec = wrap(model, model.getTypes());
            fieldCodecs.put(model, codec);
        }
        return codec;
    }

    private Codec<Object> wrap(final FieldModel model, final List<Class<?>> types) {
        Codec<Object> fieldCodec = null;
        Class<?> first = types.get(0);
        List<Class<?>> remainder = types.size() > 1 ? types.subList(1, types.size()) : Collections.<Class<?>>emptyList();
        if (Collection.class.isAssignableFrom(first)) {
            fieldCodec = new CollectionCodec(first, wrap(model, remainder));
            model.setShouldSerialize(new CollectionShouldSerialize());
        } else if (Map.class.isAssignableFrom(first)) {
            fieldCodec = new MapCodec(first, wrap(model, remainder));
            model.setShouldSerialize(new MapShouldSerialize());
        } else {
            try {
                fieldCodec = (Codec<Object>) registry.get(first);
                if (fieldCodec instanceof PojoCodec) {
                    fieldCodec = ((PojoCodec<Object>) fieldCodec).specialize(model);
                }
            } catch (final CodecConfigurationException e) {
                throw new CodecConfigurationException(format("Can not find codec for the field '%s' of type '%s'", model.getName(),
                                                             first.getSimpleName()), e);
            }
        }

        return fieldCodec;
    }


    @Override
    public Class<T> getEncoderClass() {
        return (Class<T>) classModel.getType();
    }

    @Override
    public T generateIdIfAbsentFromDocument(final T entity) {
        FieldModel idField = classModel.getIdField();
        if (idField.get(entity) == null) {
            idField.set(entity, classModel.getIdGenerator().generate());
        }
        return entity;
    }

    @Override
    public boolean documentHasId(final T entity) {
        return classModel.getIdField().get(entity) != null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public BsonValue getDocumentId(final T entity) {
        FieldModel field = getClassModel().getIdField();
        BsonDocumentWriter writer = new BsonDocumentWriter(new BsonDocument());
        writer.writeStartDocument();
        writer.writeName("_id");
        Codec codec = registry.get(field.getType());
        codec.encode(writer, field.get(entity), EncoderContext.builder().build());
        writer.writeEndDocument();
        return writer.getDocument().get("_id");
    }

    /**
     * @return the ClassModel for this codec
     */
    public ClassModel getClassModel() {
        return classModel;
    }

    @Override
    public String toString() {
        return format("PojoCodec<%s>", classModel);
    }

    private T createInstance() {
        try {
            return (T) classModel.getConstructor().newInstance();
        } catch (InstantiationException e) {
            throw new CodecConfigurationException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new CodecConfigurationException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new CodecConfigurationException(e.getMessage(), e);
        }
    }

    private <K> Codec<K> findActualCodec(final BsonReader reader) {
        reader.mark();
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if ("_t".equals(name)) {
                String className = reader.readString();
                try {
                    reader.reset();
                    return (Codec<K>) findActualCodec(Class.forName(className));
                } catch (final ClassNotFoundException e) {
                    throw new CodecConfigurationException("A mapped class could not be found: " + className);
                }
            } else {
                reader.skipValue();
            }
        }
        reader.reset();
        return (PojoCodec<K>) this;
    }

    private Codec<?> findActualCodec(final Class<?> aClass) {
        return getEncoderClass().equals(aClass) ? this : registry.get(aClass);
    }

    PojoCodec<T> specialize(final FieldModel fieldModel) {
        return new PojoCodec<T>(this, fieldModel);
    }

    void readFields(final T entity, final BsonReader reader, final DecoderContext decoderContext) {
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if ("_t".equals(name)) {
                reader.readString();
            } else {
                FieldModel field = classModel.getField(name);
                if (field != null) {  // TODO:  JAVA-2218
                    decode(field, entity, reader, decoderContext);
                } else {
                    reader.skipValue();
                }
            }
        }
    }
}
