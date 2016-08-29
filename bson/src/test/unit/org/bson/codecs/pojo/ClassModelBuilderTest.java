/*
 * Copyright (c) 2008-2016 MongoDB, Inc.
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

import org.bson.codecs.Codec;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel.Builder;
import org.bson.codecs.pojo.entities.Address;
import org.bson.codecs.pojo.entities.ZipCode;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ClassModelBuilderTest {
    @Test
    public void builder() throws NoSuchFieldException {
        PojoCodecProvider.Builder builder = PojoCodecProvider.builder();

        Builder address = ClassModel.builder(Address.class)
                                    .collection("addresses");
        address.addField(Address.class.getDeclaredField("street"))
            .type(String.class)
            .documentFieldName("st4");

        address.addField(Address.class.getDeclaredField("city"))
            .type(String.class);
        address.addField(Address.class.getDeclaredField("state"))
            .type(String.class)
            .documentFieldName("st");
        address.addField(Address.class.getDeclaredField("zip"))
            .type(ZipCode.class);

        Builder zip = ClassModel.builder(ZipCode.class);
        zip.addField(ZipCode.class.getDeclaredField("number"))
            .type(Integer.class);
        zip.addField(ZipCode.class.getDeclaredField("extended"))
            .type(Integer.class)
            .documentFieldName("plus");

        PojoCodecProvider provider = builder
            .register(address, zip)
            .build();

        CodecRegistry registry = CodecRegistries.fromProviders(provider, new ValueCodecProvider());

        Codec<Address> addressCodec = registry.get(Address.class);
        assertTrue(addressCodec instanceof PojoCodec);
        assertTrue(addressCodec.getEncoderClass().equals(Address.class));
    }
}
