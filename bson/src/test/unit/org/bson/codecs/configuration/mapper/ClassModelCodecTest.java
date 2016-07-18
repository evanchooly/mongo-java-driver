/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bson.codecs.configuration.mapper;

import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonObjectId;
import org.bson.BsonString;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.configuration.mapper.entities.BaseGenericType;
import org.bson.codecs.configuration.mapper.entities.Complex;
import org.bson.codecs.configuration.mapper.entities.ComplexCtor;
import org.bson.codecs.configuration.mapper.entities.Concrete;
import org.bson.codecs.configuration.mapper.entities.ContainerTypes;
import org.bson.codecs.configuration.mapper.entities.IntChild;
import org.bson.codecs.configuration.mapper.entities.MappedEntity;
import org.bson.codecs.configuration.mapper.entities.StringChild;
import org.junit.Assert;
import org.junit.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ClassModelCodecTest {

    private CodecProvider codecProvider;

    @Test
    public void testConcreteContainerTypes() {
        CodecRegistry registry = getCodecRegistry(Concrete.class);

        Concrete concrete = new Concrete();

        Map<String, Double> map = new HashMap<String, Double>();
        map.put("one", 1D);
        map.put("eleven", 11D);
        concrete.setMap(map);

        List<Integer> list = asList(1, 14, 44, 57);
        concrete.setList(list);

        BsonDocument document = new BsonDocument();
        Codec<Concrete> codec = registry.get(Concrete.class);
        codec.encode(new BsonDocumentWriter(document), concrete, EncoderContext.builder().build());

        Concrete decoded = codec.decode(new BsonDocumentReader(document), DecoderContext.builder().build());

        assertEquals(concrete, decoded);
    }

    @Test
    public void testNestedGenerics() {
        CodecRegistry registry = getCodecRegistry(ContainerTypes.class, BaseGenericType.class, IntChild.class);

        ContainerTypes types = new ContainerTypes();
        types.setList(asList(new IntChild(42), new IntChild(36)));
        types.setDoubleList(asList(types.getList(), types.getList()));
        types.setTripleList(asList(types.getDoubleList(), types.getDoubleList()));

        Collection<IntChild> collection = asList(new IntChild(42), new IntChild(36));
        types.setCollection(collection);
        Collection<Collection<? extends BaseGenericType<? extends Object>>> lists = asList(collection, types.getList());
        types.setDoubleCollection(lists);
        Collection<Collection<Collection<? extends BaseGenericType<? extends Object>>>> triple = asList(lists, lists);
        types.setTripleCollection(triple);

        types.setSet(new HashSet(asList(new IntChild(42), new IntChild(36))));
        types.setDoubleSet(new HashSet(asList(types.getSet(), types.getSet())));
        types.setTripleSet(new HashSet(asList(types.getDoubleSet(), types.getDoubleSet())));

        Map<String, BaseGenericType<?>> map = new HashMap<String, BaseGenericType<?>>();
        map.put("map", new IntChild(42));
        types.setMap(map);

        Map<String, Map<String, BaseGenericType<?>>> map2 = new HashMap<String, Map<String, BaseGenericType<?>>>();
        map2.put("map of map", map);
        types.setDoubleMap(map2);

        Map<String, Map<String, Map<String, BaseGenericType<?>>>> map3
            = new HashMap<String, Map<String, Map<String, BaseGenericType<?>>>>();
        map3.put("map of map of map", map2);
        types.setTripleMap(map3);

        BsonDocument document = new BsonDocument();
        Codec<ContainerTypes> codec = registry.get(ContainerTypes.class);
        codec.encode(new BsonDocumentWriter(document), types, EncoderContext.builder().build());

        ContainerTypes decoded = codec.decode(new BsonDocumentReader(document), DecoderContext.builder().build());

        assertEquals(types, decoded);

    }
    @Test
    public void testDecode() {
        MappedEntity entity = new MappedEntity(800L, 12, "Bond", "James Bond");

        BsonDocument document = new BsonDocument("age", new BsonInt64(800))
            .append("faves", new BsonInt32(12))
            .append("name", new BsonString("Bond"))
            .append("fullName", new BsonString("James Bond"));
        CodecRegistry codecRegistry = getCodecRegistry(MappedEntity.class);

        MappedEntity decoded = codecRegistry
            .get(MappedEntity.class)
            .decode(new BsonDocumentReader(document), DecoderContext.builder().build());

        assertEquals(entity, decoded);
    }

    @Test
    public void testEmbeddedSettings() {
        CodecRegistry registry = getCodecRegistry(Complex.class, IntChild.class, StringChild.class, BaseGenericType.class);
        Complex complex = new Complex();
        complex.setIntChild(new IntChild(42));
        BsonDocument document = new BsonDocument();

        ClassModelCodec<Complex> complexCodec = (ClassModelCodec<Complex>) registry.get(Complex.class);
        complexCodec.getClassModel().getField("intChild").setUseDiscriminator(false);

        complexCodec.encode(new BsonDocumentWriter(document), complex, EncoderContext.builder().build());
        assertEquals(null, document.getDocument("intChild").get("_t"));
    }

    @Test
    public void testExtraFields() {
        CodecRegistry registry = getCodecRegistry(Complex.class, IntChild.class, StringChild.class, BaseGenericType.class);
        Complex complex = new Complex();
        complex.setIntChild(new IntChild(42));
        BsonDocument bsonDocument = new BsonDocument();

        Codec<Complex> complexCodec = registry.get(Complex.class);
        complexCodec.encode(new BsonDocumentWriter(bsonDocument), complex, EncoderContext.builder().build());
        bsonDocument.put("extra", new BsonString("I'm an extra field"));

        Complex decode = complexCodec.decode(new BsonDocumentReader(bsonDocument), DecoderContext.builder().build());

        assertEquals(complex, decode);
    }

    @Test
    public void testGenerics() {
        CodecRegistry registry = getCodecRegistry(Complex.class, BaseGenericType.class, IntChild.class, StringChild.class);

        ClassModelCodec<IntChild> intChildCodec = (ClassModelCodec<IntChild>) registry.get(IntChild.class);
        ClassModelCodec<StringChild> stringChildCodec = (ClassModelCodec<StringChild>) registry.get(StringChild.class);
        ClassModelCodec<Complex> complexCodec = (ClassModelCodec<Complex>) registry.get(Complex.class);

        BsonDocument document = new BsonDocument();
        intChildCodec.encode(new BsonDocumentWriter(document), new IntChild(42), EncoderContext.builder().build());
        assertEquals(42, document.getInt32("t").intValue());

        document = new BsonDocument();
        stringChildCodec.encode(new BsonDocumentWriter(document), new StringChild("I'm a child!"), EncoderContext.builder().build());
        assertEquals("I'm a child!", document.getString("t").getValue());

        document = new BsonDocument();
        Complex complex = new Complex(new IntChild(100), new StringChild("what what?"), new BaseGenericType<String>("so tricksy!"));
        complexCodec.encode(new BsonDocumentWriter(document), complex, EncoderContext.builder().build());
        BsonDocument child = document.getDocument("intChild");
        assertNotNull(child);
        assertEquals(100, child.getInt32("t").intValue());

        child = document.getDocument("stringChild");
        assertNotNull(child);
        assertEquals("what what?", child.getString("t").getValue());

        child = document.getDocument("baseType");
        assertNotNull(child);
        assertEquals("so tricksy!", child.getString("t").getValue());
        assertEquals(BaseGenericType.class.getName(), child.getString("_t").getValue());

        Complex decode = complexCodec.decode(new BsonDocumentReader(document), DecoderContext.builder().build());
        assertEquals(complex, decode);

        Complex custom = new Complex(new IntChild(1234), new StringChild("Another round!"),
                                     new BaseGenericType<String>("Mongo just pawn in game of life"));

        document = new BsonDocument();

        complexCodec.encode(new BsonDocumentWriter(document), custom, EncoderContext.builder().build());
        decode = complexCodec.decode(new BsonDocumentReader(document), DecoderContext.builder().build());

        assertEquals(custom, decode);
        assertEquals(1234, custom.getIntChild().getT().intValue());
        assertEquals("Another round!", custom.getStringChild().getT());
        assertEquals("Mongo just pawn in game of life", custom.getBaseType().getT());
    }

    @Test
    public void testIdHandling() {
        CodecRegistry registry = getCodecRegistry(Concrete.class);

        ClassModelCodec<Concrete> concreteCodec = (ClassModelCodec<Concrete>) registry.get(Concrete.class);

        Concrete concrete = new Concrete();
        assertFalse(concreteCodec.documentHasId(concrete));

        concreteCodec.generateIdIfAbsentFromDocument(concrete);
        assertNotNull(concrete.getId());
        assertTrue(concreteCodec.documentHasId(concrete));
        assertTrue(concreteCodec.getDocumentId(concrete) instanceof BsonObjectId);
    }

    @Test(expected = CodecConfigurationException.class)
    public void testNoSuitableConstructor() {
        CodecRegistry codecRegistry = getCodecRegistry(ComplexCtor.class);

        BsonDocument document = new BsonDocument();
        Codec<ComplexCtor> complexCtorCodec = codecRegistry.get(ComplexCtor.class);
        complexCtorCodec.encode(new BsonDocumentWriter(document), new ComplexCtor(42),
                                EncoderContext.builder().build());
        complexCtorCodec.decode(new BsonDocumentReader(document), DecoderContext.builder().build());
    }

    @Test(expected = BsonInvalidOperationException.class)
    public void testNullObjectWrites() {
        CodecRegistry codecRegistry = getCodecRegistry(Concrete.class);
        Codec<Concrete> codec = codecRegistry.get(Concrete.class);

        codec.encode(new BsonDocumentWriter(new BsonDocument()), null, EncoderContext.builder().build());
    }

    @Test(expected = CodecConfigurationException.class)
    public void testPartialMapping() {
        CodecRegistry registry = getCodecRegistry(Complex.class, StringChild.class, BaseGenericType.class, ContainerTypes.class);

        Codec<Complex> complexCodec = registry.get(Complex.class);
        BsonDocument document = new BsonDocument();
        Complex complex = new Complex(new IntChild(100), new StringChild("what what?"), new BaseGenericType<String>("so tricksy!"));
        complexCodec.encode(new BsonDocumentWriter(document), complex, EncoderContext.builder().build());
    }

    @Test
    public void testProvider() {
        CodecRegistry codecRegistry = getCodecRegistry(MappedEntity.class);

        assertTrue(codecRegistry.get(MappedEntity.class) instanceof ClassModelCodec);
        try {
            codecRegistry.get(Color.class);
            Assert.fail("The get should throw an exception on an unknown class.");
        } catch (final CodecConfigurationException e) {
            // expected
        }
    }

    @Test
    public void testRoundTrip() {
        MappedEntity entity = new MappedEntity(800L, 12, "Bond", "James Bond");

        CodecRegistry codecRegistry = getCodecRegistry(MappedEntity.class);

        BsonDocument document = new BsonDocument();
        BsonDocumentWriter writer = new BsonDocumentWriter(document);
        Codec<MappedEntity> codec = codecRegistry.get(MappedEntity.class);
        codec.encode(writer, entity, EncoderContext.builder().build());
        MappedEntity decoded = codec.decode(new BsonDocumentReader(document), DecoderContext.builder().build());

        assertEquals(entity, decoded);
    }

    @Test
    public void testStoreEmpties() {
        CodecRegistry registry = getCodecRegistry(ContainerTypes.class, BaseGenericType.class);
        BsonDocument bsonDocument = new BsonDocument();
        ContainerTypes containerTypes = new ContainerTypes();
        containerTypes.setList(new ArrayList<BaseGenericType<?>>());
        containerTypes.setMap(new HashMap<String, BaseGenericType<?>>());

        ClassModelCodec<ContainerTypes> codec = (ClassModelCodec<ContainerTypes>) registry.get(ContainerTypes.class);

        codec.encode(new BsonDocumentWriter(bsonDocument), containerTypes, EncoderContext.builder().build());

        assertEquals(bsonDocument.keySet().toString(), 1, bsonDocument.size());
        bsonDocument.clear();
        codec.getClassModel().getField("map").setStoreEmpties(true);
        codec.getClassModel().getField("list").setStoreEmpties(true);
        codec.encode(new BsonDocumentWriter(bsonDocument), containerTypes, EncoderContext.builder().build());
        assertEquals(3, bsonDocument.size());

        ContainerTypes decoded = codec.decode(new BsonDocumentReader(bsonDocument), DecoderContext.builder().build());
        assertNotNull(decoded.getMap());
        assertNotNull(decoded.getList());
    }

    @Test
    public void testStoreNulls() {
        ContainerTypes containerTypes = new ContainerTypes();
        CodecRegistry codecRegistry = getCodecRegistry(Complex.class, IntChild.class, StringChild.class, BaseGenericType.class,
                                                       ContainerTypes.class);
        ClassModelCodec<ContainerTypes> codec = (ClassModelCodec<ContainerTypes>) codecRegistry.get(ContainerTypes.class);
        assertEquals("ClassModelCodec<ClassModel<ContainerTypes>>", codec.toString());
        assertEquals("ClassModelCodec<ClassModel<BaseGenericType>>", codecRegistry.get(BaseGenericType.class).toString());

        BsonDocument document = new BsonDocument();
        codec.encode(new BsonDocumentWriter(document), containerTypes, EncoderContext.builder().build());
        assertEquals(1, document.size());

        codec.getClassModel().getField("id").setStoreNulls(true);
        codec.getClassModel().getField("map").setStoreNulls(true);
        codec.getClassModel().getField("list").setStoreNulls(true);
        codec.getClassModel().getField("mixed").setStoreNulls(true);
        document = new BsonDocument();
        codec.encode(new BsonDocumentWriter(document), containerTypes, EncoderContext.builder().build());
        assertEquals(4, document.size());

        ContainerTypes decoded = codec.decode(new BsonDocumentReader(document), DecoderContext.builder().build());
        assertNull(decoded.getMixed());
        assertNull(decoded.getMap());
        assertNull(decoded.getList());

        ClassModelCodec<Complex> complexCodec = (ClassModelCodec<Complex>) codecRegistry.get(Complex.class);
        complexCodec.getClassModel().getField("stringChild").setStoreNulls(true);

        document = new BsonDocument();
        complexCodec.encode(new BsonDocumentWriter(document), new Complex(), EncoderContext.builder().build());
        assertEquals(2, document.size());
        complexCodec.decode(new BsonDocumentReader(document), DecoderContext.builder().build());
    }

    @Test(expected = CodecConfigurationException.class)
    public void testUnknownClass() {
        CodecRegistry registry = getCodecRegistry(Complex.class, IntChild.class, StringChild.class, BaseGenericType.class,
                                                  ContainerTypes.class);

        BsonDocument document = new BsonDocument("_t", new BsonString("i'm not a real class"));
        registry.get(Complex.class).decode(new BsonDocumentReader(document), DecoderContext.builder().build());

    }

    private CodecRegistry getCodecRegistry(final Class<?> clazz, final Class<?>... classes) {
        List<Class<?>> list = new ArrayList<Class<?>>(asList(classes));
        list.add(clazz);
        codecProvider = ClassModelCodecProvider.builder()
                                               .register(clazz)
                                               .register(classes)
                                               .build();

        return CodecRegistries.fromProviders(codecProvider,
                                             new ValueCodecProvider());
    }

}
