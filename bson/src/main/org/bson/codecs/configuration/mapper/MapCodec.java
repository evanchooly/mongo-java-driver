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

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings("rawtypes")
class MapCodec implements Codec {
    private final Codec codec;
    private final Class encoderClass;

    MapCodec(final Class encoderClass, final Codec codec) {
        this.codec = codec;
        this.encoderClass = encoderClass;
    }

    @Override
    public Object decode(final BsonReader reader, final DecoderContext context) {
        Map<Object, Object> map = null;
        if (reader.getCurrentBsonType().equals(BsonType.NULL)) {
            reader.readNull();
        } else {
            map = FieldModel.createConcreteType(getEncoderClass());
            reader.readStartDocument();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                map.put(reader.readName(), codec.decode(reader, context));
            }
            reader.readEndDocument();
        }
        return map;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void encode(final BsonWriter writer, final Object o, final EncoderContext encoderContext) {
        if (o != null) {
            writer.writeStartDocument();
            for (final Entry<String, ?> entry : ((Map<String, ?>) o).entrySet()) {
                writer.writeName(entry.getKey());
                codec.encode(writer, entry.getValue(), encoderContext);
            }
            writer.writeEndDocument();
        } else {
            writer.writeNull();
        }
    }

    @Override
    public Class getEncoderClass() {
        return encoderClass;
    }
}
