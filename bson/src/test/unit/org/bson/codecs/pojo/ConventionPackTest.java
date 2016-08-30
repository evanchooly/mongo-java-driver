/*
 * Copyright (c) 2008-2015 MongoDB, Inc.
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
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider.Builder;
import org.bson.codecs.pojo.conventions.naming.SnakeCasePropertyNamingConvention;
import org.bson.codecs.pojo.entities.Address;
import org.bson.codecs.pojo.entities.BaseGenericType;
import org.bson.codecs.pojo.entities.Comment;
import org.bson.codecs.pojo.entities.Complex;
import org.bson.codecs.pojo.entities.ContainerTypes;
import org.bson.codecs.pojo.entities.IntChild;
import org.bson.codecs.pojo.entities.MappedEntity;
import org.bson.codecs.pojo.entities.NamedStringChild;
import org.bson.codecs.pojo.entities.Person;
import org.bson.codecs.pojo.entities.Post;
import org.bson.codecs.pojo.entities.StringChild;
import org.bson.codecs.pojo.entities.ZipCode;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

@SuppressWarnings("unchecked")
public class ConventionPackTest {

    @Test
    public void testCollectionNesting() {
        CodecRegistry registry = getCodecRegistry();
        ContainerTypes types = new ContainerTypes();
        List<? extends BaseGenericType<?>> list1 = Arrays.<BaseGenericType<?>>asList(new IntChild(1), new IntChild(2));
        List<? extends BaseGenericType<?>> list2 = Arrays.<BaseGenericType<?>>asList(new IntChild(3), new StringChild("Dee"));
        List<List<? extends BaseGenericType<?>>> doubleList = asList(list1, list2);
        types.setDoubleList(doubleList);
        roundTrip(registry, types);

        types = new ContainerTypes();
        List<List<List<? extends BaseGenericType<?>>>> tripleList = asList(doubleList, doubleList);
        types.setTripleList(tripleList);
        roundTrip(registry, types);
    }

    @Test
    public void testCollections() {
        CodecRegistry registry = getCodecRegistry();
        ContainerTypes types = new ContainerTypes();
        types.setList(asList(new IntChild(1), new IntChild(2), new IntChild(3), new StringChild("Dee")));

        roundTrip(registry, types);
    }

    @Test
    public void testCustomConventions() {
        PojoCodecProvider codecProvider = PojoCodecProvider
            .builder(ConventionOptions.builder()
                    .propertyNaming(new SnakeCasePropertyNamingConvention())
                    .build())
            .register(MappedEntity.class)
            .build();
        CodecRegistry registry = fromProviders(codecProvider, new ValueCodecProvider());

        Codec<MappedEntity> codec = registry.get(MappedEntity.class);
        BsonDocument document = new BsonDocument();
        BsonDocumentWriter writer = new BsonDocumentWriter(document);
        codec.encode(writer, new MappedEntity(102L, 0, "Scrooge", "Ebenezer Scrooge"), EncoderContext.builder().build());
        Assert.assertEquals(document.getNumber("age").longValue(), 102L);
        Assert.assertEquals(document.getNumber("faves").intValue(), 0);
        Assert.assertEquals(document.getString("name").getValue(), "Scrooge");
        Assert.assertEquals(document.getString("full_name").getValue(), "Ebenezer Scrooge");
        Assert.assertFalse(document.containsKey("debug"));
    }

    @Test
    public void testDefaultConventions() {
        PojoCodecProvider codecProvider = PojoCodecProvider
            .builder()
            .register(MappedEntity.class)
            .build();
        CodecRegistry registry = fromProviders(codecProvider, new ValueCodecProvider());

        Codec<MappedEntity> codec = registry.get(MappedEntity.class);
        BsonDocument document = new BsonDocument();
        BsonDocumentWriter writer = new BsonDocumentWriter(document);
        codec.encode(writer, new MappedEntity(102L, 0, "Scrooge", "Ebenezer Scrooge"), EncoderContext.builder().build());
        Assert.assertEquals(document.getNumber("age").longValue(), 102L);
        Assert.assertEquals(document.getNumber("faves").intValue(), 0);
        Assert.assertEquals(document.getString("name").getValue(), "Scrooge");
        Assert.assertEquals(document.getString("fullName").getValue(), "Ebenezer Scrooge");
        Assert.assertFalse(document.containsKey("debug"));
    }

    @Test
    public void testDiscriminators() {
        PojoCodecProvider provider = PojoCodecProvider.builder(ConventionOptions.builder()
                                                                   .storeEmptyFields(true)
                                                                   .storeNullFields(true)
                                                              .build())
                                                      .register(Post.class, Comment.class)
                                                      .build();
        CodecRegistry registry = fromProviders(provider, new ValueCodecProvider());

        PojoCodec<Post> codec = (PojoCodec<Post>) registry.get(Post.class);

        Post post = new Post();
        post.setBody("i'm the body");
        post.setPosted(new Date());
        post.setTitle("i'm the title");
        List<Comment> comments = asList(new Comment(new Date(), "comment 1"), new Comment(new Date(), "comment 2"));
        post.setComments(comments);
        post.setBare(comments);

        BsonDocument document = new BsonDocument();
        codec.encode(new BsonDocumentWriter(document), post, EncoderContext.builder().build());
        Assert.assertNotNull(document.get("_t"));

        Post decoded = codec.decode(new BsonDocumentReader(document), DecoderContext.builder().build());

        Assert.assertEquals(post, decoded);
        Assert.assertNull(((List<BsonDocument>) document.get("bare")).get(0).get("_t"));
    }

    @Test
    public void testEmbeddedEntities() {
        PojoCodecProvider codecProvider = PojoCodecProvider.builder()
                                                           .register(Person.class)
                                                           .register(Address.class)
                                                           .register(ZipCode.class)
                                                           .build();
        CodecRegistry registry = fromProviders(codecProvider, new ValueCodecProvider());

        Codec<Person> personCodec = registry.get(Person.class);
        Codec<Address> addressCodec = registry.get(Address.class);
        Codec<ZipCode> zipCodeCodec = registry.get(ZipCode.class);
        BsonDocument personDocument = new BsonDocument();
        BsonDocument addressDocument = new BsonDocument();
        BsonDocument zipDocument = new BsonDocument();

        ZipCode zip = new ZipCode(12345, 1234);
        Address address = new Address("1000 Quiet Lane", "Whispering Pines", "HA", zip);
        Person entity = new Person("Bob", "Ross", address);

        zipCodeCodec.encode(new BsonDocumentWriter(zipDocument), zip, EncoderContext.builder().build());
        Assert.assertEquals(zipDocument.getInt32("number").getValue(), 12345);
        Assert.assertEquals(zipDocument.getInt32("extended").getValue(), 1234);

        addressCodec.encode(new BsonDocumentWriter(addressDocument), address, EncoderContext.builder().build());
        Assert.assertEquals(addressDocument.getString("street").getValue(), "1000 Quiet Lane");
        Assert.assertEquals(addressDocument.getString("city").getValue(), "Whispering Pines");
        Assert.assertEquals(addressDocument.getString("state").getValue(), "HA");
        Assert.assertEquals(addressDocument.getDocument("zip"), zipDocument);

        personCodec.encode(new BsonDocumentWriter(personDocument), entity, EncoderContext.builder().build());
        Assert.assertEquals(personDocument.getString("firstName").getValue(), "Bob");
        Assert.assertEquals(personDocument.getString("lastName").getValue(), "Ross");
        Assert.assertEquals(personDocument.getDocument("home"), addressDocument);

        Assert.assertEquals(entity, personCodec.decode(new BsonDocumentReader(personDocument), DecoderContext.builder().build()));
    }

    @Test
    public void testMapNesting() {
        ContainerTypes types = new ContainerTypes();
        HashMap<String, BaseGenericType<?>> map1 = new HashMap<String, BaseGenericType<?>>();
        for (int i = 0; i < 10; i++) {
            map1.put(i + "", new IntChild(10 - i));
        }
        HashMap<String, BaseGenericType<?>> map2 = new HashMap<String, BaseGenericType<?>>();
        for (int i = 0; i < 10; i++) {
            map2.put(i + "", new IntChild(i));
        }
        HashMap<String, Map<String, BaseGenericType<?>>> map = new HashMap<String, Map<String, BaseGenericType<?>>>();
        map.put("map1", map1);
        map.put("map2", map2);
        types.setDoubleMap(map);

        roundTrip(getCodecRegistry(), types);

        Map<String, Map<String, Map<String, BaseGenericType<?>>>> bigMap
            = new HashMap<String, Map<String, Map<String, BaseGenericType<?>>>>();
        bigMap.put("uber", map);
        types = new ContainerTypes();
        types.setTripleMap(bigMap);

        roundTrip(getCodecRegistry(), types);
    }

    @Test
    public void testMaps() {
        ContainerTypes types = new ContainerTypes();
        HashMap<String, BaseGenericType<?>> map = new HashMap<String, BaseGenericType<?>>();
        for (int i = 0; i < 10; i++) {
            map.put(i + "", new IntChild(10 - i));
        }
        types.setMap(map);

        roundTrip(getCodecRegistry(ContainerTypes.class, BaseGenericType.class, IntChild.class), types);
    }

    @Test
    public void testMixedNesting() {
        ContainerTypes types = new ContainerTypes();
        Map<String, List<Set<? extends BaseGenericType<?>>>> mixed = new HashMap<String, List<Set<? extends BaseGenericType<?>>>>();
        Set<? extends BaseGenericType<?>> set1 = new HashSet<BaseGenericType<?>>(
            asList(new IntChild(1), new IntChild(2), new IntChild(3), new StringChild("Dee")));
        Set<? extends BaseGenericType<?>> set2 = new HashSet<BaseGenericType<?>>(Collections.singletonList(new StringChild("Dee")));
        mixed.put("first", asList(set1, set2));
        mixed.put("swapped", asList(set2, set1, set2));
        types.setMixed(mixed);

        roundTrip(getCodecRegistry(), types);
    }

    @Test
    public void testPolymorphism() {
        CodecRegistry registry = getCodecRegistry();

        BsonDocument document = new BsonDocument();
        StringChild bruce = new NamedStringChild("string child", "Bruce");
        Complex complex = new Complex(new IntChild(1), new StringChild("Kung Pow"), bruce);
        registry.get(Complex.class).encode(new BsonDocumentWriter(document), complex, EncoderContext.builder().build());
        Complex decoded = registry.get(Complex.class).decode(new BsonDocumentReader(document), DecoderContext.builder().build());

        Assert.assertTrue("The baseType field should be a NamedStringChild", decoded.getBaseType() instanceof NamedStringChild);
        Assert.assertEquals(complex, decoded);
    }

    @Test
    public void testSets() {
        CodecRegistry registry = getCodecRegistry();
        ContainerTypes types = new ContainerTypes();
        Set<? extends BaseGenericType<?>> set = new HashSet<BaseGenericType<?>>(
            asList(new IntChild(1), new IntChild(2), new IntChild(3), new StringChild("Dee")));
        types.setSet(set);
        roundTrip(registry, types);

        types = new ContainerTypes();
        Set<Set<? extends BaseGenericType<?>>> doubleSet = new HashSet<Set<? extends BaseGenericType<?>>>();
        doubleSet.add(set);
        doubleSet.add(set);
        types.setDoubleSet(doubleSet);
        roundTrip(registry, types);

        types = new ContainerTypes();
        Set<Set<Set<? extends BaseGenericType<?>>>> tripleSet = new HashSet<Set<Set<? extends BaseGenericType<?>>>>();
        tripleSet.add(doubleSet);
        tripleSet.add(doubleSet);
        types.setTripleSet(tripleSet);
    }

    @Test
    public void validations() {
        PojoCodecProvider codecProvider = PojoCodecProvider
            .builder()
            .register(Post.class)
            .register(Comment.class)
            .build();
        CodecRegistry registry = fromProviders(codecProvider, new ValueCodecProvider());

        registry.get(Post.class);
    }

    private <T> void roundTrip(final CodecRegistry registry, final T object) {
        DecoderContext decoderContext = DecoderContext.builder().build();
        EncoderContext encoderContext = EncoderContext.builder().build();

        BsonDocument document = new BsonDocument();
        Class<T> klass = (Class<T>) object.getClass();
        registry.get(klass).encode(new BsonDocumentWriter(document), object, encoderContext);
        Object decoded = registry.get(klass).decode(new BsonDocumentReader(document), decoderContext);
        Assert.assertEquals(object, decoded);
    }

    CodecRegistry getCodecRegistry(final Class<?>... classes) {
        Builder builder = PojoCodecProvider
            .builder();
        for (final Class<?> aClass : classes) {
            builder.register(aClass);
        }
        if (classes.length == 0) {
            builder.register(BaseGenericType.class);
            builder.register(IntChild.class);
            builder.register(StringChild.class);
            builder.register(NamedStringChild.class);
            builder.register(Complex.class);
            builder.register(ContainerTypes.class);
            builder.register(Post.class);
            builder.register(Comment.class);
        }
        return fromProviders(builder.build(), new ValueCodecProvider());
    }
}
