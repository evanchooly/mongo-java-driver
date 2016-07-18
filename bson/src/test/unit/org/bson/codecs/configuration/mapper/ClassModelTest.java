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

package org.bson.codecs.configuration.mapper;

import org.bson.codecs.Codec;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.configuration.mapper.entities.BadField;
import org.bson.codecs.configuration.mapper.entities.BadMap;
import org.bson.codecs.configuration.mapper.entities.BaseGenericType;
import org.bson.codecs.configuration.mapper.entities.Complex;
import org.bson.codecs.configuration.mapper.entities.Concrete;
import org.bson.codecs.configuration.mapper.entities.MultipleParameters;
import org.bson.codecs.configuration.mapper.entities.UpperBounds;
import org.bson.codecs.configuration.mapper.entities.UpperBoundsChild;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClassModelTest {

    private final CodecRegistry registry = CodecRegistries.fromProviders(new CodecProvider() {
        @Override
        public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
            return Object.class.equals(clazz) ? null : (Codec<T>) new ClassModelCodec<T>(new ClassModel(registry, clazz), registry);
        }
    }, new ValueCodecProvider());

    @Test
    public void testAlternateId() {
        ClassModel classModel = new ClassModel(registry, Concrete.class);

        classModel.getField("id").setIdField(false);
        assertNull(classModel.getField("_id"));

        classModel.getField("alternateId").setIdField(true);
        assertNotNull(classModel.getField("_id"));

        assertEquals(classModel.getField("alternateId"), classModel.getField("_id"));
        assertNotEquals(classModel.getField("id"), classModel.getField("_id"));
    }

    @Test
    public void testAnalysis() {
        ClassModel classModel = new ClassModel(registry, Complex.class);
        assertFalse(classModel.hasAnnotation(Deprecated.class));

        assertEquals("Complex", classModel.getCollectionName());

        assertEquals("id", classModel.getIdField().getFieldName());
        assertEquals("_id", classModel.getIdField().getName());
        assertEquals("Complex#_id:org.bson.types.ObjectId", classModel.getIdField().toString());

        assertFalse(classModel.getIdField().hasAnnotation(Deprecated.class));

        assertNull(classModel.getField("publicField"));

        assertTrue(classModel.getField("protectedField").isIncluded());

        assertFalse(classModel.getField("transientField").isIncluded());

        assertEquals("ClassModel<Complex>", classModel.toString());

        try {
            classModel.setIdField("i'm not an actual field");
            fail("Bad field names should throw exceptions");
        } catch (final CodecConfigurationException cce) {
            // yup
        }

        assertEquals("ClassModel<BaseGenericType>", new ClassModel(registry, BaseGenericType.class).toString());
        assertEquals("ClassModel<MultipleParameters>", new ClassModel(registry, MultipleParameters.class).toString());
    }

    @Test
    public void testConcreteContainerTypes() {
        ClassModel classModel = new ClassModel(registry, Concrete.class);

        assertEquals(LinkedList.class, classModel.getField("linked").getType());

        assertEquals(ConcurrentHashMap.class, classModel.getField("concurrent").getType());

        assertEquals(Collection.class, classModel.getField("collection").getType());
        assertEquals(ArrayList.class, classModel.getField("collection").getCodec().getEncoderClass());

        assertEquals(List.class, classModel.getField("list").getType());
        assertEquals(ArrayList.class, classModel.getField("list").getCodec().getEncoderClass());

        assertEquals(Map.class, classModel.getField("map").getType());
        assertEquals(HashMap.class, classModel.getField("map").getCodec().getEncoderClass());
    }

    @Test
    public void testMappedFieldNames() {
        ClassModel classModel = new ClassModel(registry, Concrete.class);

        classModel.getField("list").setName("arrayList");
        assertNotNull(classModel.getField("list"));
        assertNotNull(classModel.getField("arrayList"));

        classModel.getField("arrayList").setName("values");
        assertNull(classModel.getField("arrayList"));
        assertNotNull(classModel.getField("list"));
        assertNotNull(classModel.getField("values"));

        classModel.getField("list").setName("list");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testMappedFieldNamesCollision() {
        new ClassModel(registry, Concrete.class).getField("list").setName("map");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testNonStringMapKeys() {
        new ClassModel(registry, BadMap.class).getField("map").getCodec();
    }

    @Test(expected = CodecConfigurationException.class)
    public void testObjectField() {
        new ClassModel(registry, BadField.class).getField("field").getCodec();
    }

    @Test(expected = CodecConfigurationException.class)
    public void testRenameId() {
        new ClassModel(registry, Concrete.class).getField("id").setName("iden");
    }

    @Test
    public void testUpperbounds() {
        ClassModel classModel = new ClassModel(registry, UpperBounds.class);
        assertTrue(classModel.isGeneric());
        assertEquals("Number", classModel.getField("id").getType().getSimpleName());

        classModel = new ClassModel(registry, UpperBoundsChild.class);
        assertTrue(classModel.isGeneric());
        assertEquals("Long", classModel.getField("id").getType().getSimpleName());
    }
}
