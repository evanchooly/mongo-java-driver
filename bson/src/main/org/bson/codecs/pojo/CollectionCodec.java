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

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.Collection;

@SuppressWarnings({"rawtypes", "unchecked"})
class CollectionCodec implements Codec {
    private final Codec codec;
    private final Class encoderClass;

    CollectionCodec(final Class encoderClass, final Codec codec) {
        this.codec = codec;
        this.encoderClass = encoderClass;
    }

    @Override
    public Object decode(final BsonReader reader, final DecoderContext context) {
        Collection collection = null;
        if (reader.getCurrentBsonType().equals(BsonType.NULL)) {
            reader.readNull();
        } else {
            reader.readStartArray();

            collection = FieldModel.createConcreteType(getEncoderClass());
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                collection.add(codec.decode(reader, context));
            }
            reader.readEndArray();
        }
        return collection;
    }

    @Override
    public void encode(final BsonWriter writer, final Object o, final EncoderContext encoderContext) {
        if (o != null) {
            writer.writeStartArray();
            for (final Object object : (Collection<?>) o) {
                codec.encode(writer, object, encoderContext);
            }
            writer.writeEndArray();
        } else {
            writer.writeNull();
        }
    }

    @Override
    public Class getEncoderClass() {
        return encoderClass;
    }
}
