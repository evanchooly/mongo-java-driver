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
import org.bson.codecs.IdGenerator;
import org.bson.codecs.ObjectIdGenerator;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;

import java.lang.reflect.InvocationTargetException;

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
    private IdGenerator idFactory = new ObjectIdGenerator();

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
        idFactory = original.idFactory;
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
                if (classModel.isUseDiscriminator()) {
                    writer.writeString("_t", classModel.getDiscriminator());
                }
                for (FieldModel fieldModel : classModel.getFields()) {
                    fieldModel.encode(writer, entity, encoderContext);
                }
                writer.writeEndDocument();
            } else {
                codec.encode(writer, (T) entity, encoderContext);
            }
        }
    }

    @Override
    public Class<T> getEncoderClass() {
        return (Class<T>) classModel.getType();
    }

    @Override
    public T generateIdIfAbsentFromDocument(final T entity) {
        FieldModel idField = classModel.getIdField();
        if (idField.get(entity) == null) {
            idField.set(entity, idFactory.generate());
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
        return String.format("PojoCodec<%s>", classModel);
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
        return getEncoderClass().equals(aClass) ? this : (Codec<?>) registry.get(aClass);
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
                    field.decode(entity, reader, decoderContext);
                } else {
                    reader.skipValue();
                }
            }
        }
    }
}
