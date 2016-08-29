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

import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.pojo.ClassModel.Builder;
import org.bson.codecs.pojo.entities.BadField;
import org.bson.codecs.pojo.entities.BadMap;
import org.bson.codecs.pojo.entities.BaseGenericType;
import org.bson.codecs.pojo.entities.Complex;
import org.bson.codecs.pojo.entities.Concrete;
import org.bson.codecs.pojo.entities.MultipleParameters;
import org.bson.codecs.pojo.entities.UpperBounds;
import org.bson.codecs.pojo.entities.UpperBoundsChild;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.bson.codecs.pojo.ConventionOptions.DEFAULT_CONVENTIONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClassModelTest {

    @Test
    public void testAlternateId() {
        assertNull(ClassModel.builder(Concrete.class)
                             .map()
                             .build()
                             .getIdField());
        ClassModel classModel = ClassModel.builder(Concrete.class)
                                          .map()
                                          .idField("alternateId")
                                          .build();

        assertEquals("alternateId", classModel.getIdField().getFieldName());
    }

    @Test
    public void testAnalysis() {
        Builder builder = ClassModel.builder(Complex.class)
                                    .map()
                                    .apply(DEFAULT_CONVENTIONS.getConventions());
        ClassModel classModel = builder.build();
        assertFalse(classModel.hasAnnotation(Deprecated.class));

        assertEquals("Complex", classModel.getCollectionName());

        assertFalse(classModel.getField("id").hasAnnotation(Deprecated.class));

        assertNull(classModel.getField("staticField"));

        assertNotNull(classModel.getField("protectedField"));

        assertNull(classModel.getField("transientField"));

        assertEquals("ClassModel<Complex>", classModel.toString());

        try {
            builder.idField("i'm not an actual field");
            fail("Bad field names should throw exceptions");
        } catch (final CodecConfigurationException cce) {
            // yup
        }

        assertEquals("ClassModel<BaseGenericType>", ClassModel.builder(BaseGenericType.class).map().build().toString());
        assertEquals("ClassModel<MultipleParameters>", ClassModel.builder(MultipleParameters.class).map().build().toString());
    }

    @Test
    public void testConcreteContainerTypes() {
        ClassModel classModel = ClassModel.builder(Concrete.class).map()
                                          .apply(DEFAULT_CONVENTIONS.getConventions())
                                          .build();

        assertEquals(LinkedList.class, classModel.getField("linked").getType());

        assertEquals(ConcurrentHashMap.class, classModel.getField("concurrent").getType());

        assertEquals(Collection.class, classModel.getField("collection").getType());

        assertEquals(List.class, classModel.getField("list").getType());

        assertEquals(Map.class, classModel.getField("map").getType());
    }

    @Test
    public void testMappedFieldNames() {
        Builder builder = ClassModel.builder(Concrete.class)
                                    .map();

        builder.field("list").documentFieldName("arrayList");
        assertNotNull(builder.field("list"));
        assertNotNull(builder.field("arrayList"));

        builder.field("arrayList").documentFieldName("values");
        assertNull(builder.field("arrayList"));
        assertNotNull(builder.field("list"));
        assertNotNull(builder.field("values"));
    }

    @Test(expected = CodecConfigurationException.class)
    public void testMappedFieldNamesCollision() {
        ClassModel.builder(Concrete.class)
                  .map()
                  .field("list")
                  .documentFieldName("map");
    }

    @Ignore("Model validation should handle this")
    @Test(expected = CodecConfigurationException.class)
    public void testNonStringMapKeys() {
        ClassModel.builder(BadMap.class)
                  .map()
                  .build()
                  .getField("map")/*
                  .getCodec()*/;
    }

    @Ignore("Model validation should handle this")
    @Test(expected = CodecConfigurationException.class)
    public void testObjectField() {
        ClassModel.builder(BadField.class)
                  .map()
                  .apply(DEFAULT_CONVENTIONS.getConventions())
                  .build()
                  .getField("field")/*
                  .getCodec()*/;
    }

    @Test
    public void testUpperbounds() {
        ClassModel classModel = ClassModel.builder(UpperBounds.class)
                                          .map()
                                          .apply(DEFAULT_CONVENTIONS.getConventions())
                                          .build();
        assertTrue(classModel.isGeneric());
        assertEquals("Number", classModel.getField("id").getType().getSimpleName());

        classModel = ClassModel.builder(UpperBoundsChild.class)
                               .map()
                               .apply(DEFAULT_CONVENTIONS.getConventions())
                               .build();
        assertTrue(classModel.isGeneric());
        assertEquals("Long", classModel.getField("id").getType().getSimpleName());
    }
}
