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

import com.fasterxml.classmate.AnnotationConfiguration.StdConfiguration;
import com.fasterxml.classmate.AnnotationInclusion;
import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedField;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.conventions.Convention;
import org.bson.codecs.pojo.conventions.DefaultAnnotationConvention;
import org.bson.codecs.pojo.conventions.FieldSelectionConvention;
import org.bson.codecs.pojo.conventions.FieldStorageConvention;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

/**
 * Provides Codecs for various POJOs via the ClassModel abstractions.
 *
 * @since 3.4
 */
@SuppressWarnings("rawtypes")
public final class PojoCodecProvider implements CodecProvider {
    private final List<ClassModel> registered;
    private final List<Convention> conventions;

    /**
     * Creates a provider for a given set of classes.
     *
     * @param registered  the models to use
     * @param conventions the {@link Convention}s to apply to the mapped classes
     */
    public PojoCodecProvider(final List<ClassModel> registered, final List<Convention> conventions) {
        this.registered = registered;
        this.conventions = conventions;
    }

    /**
     * Creates a Builder so classes can be registered before creating an immutable Provider.  This Builder will use the default options
     * and Conventions.
     *
     * @return the Builder
     * @see Builder#register(Class[])
     */
    public static Builder builder() {
        return builder(new ConventionOptions());
    }

    /**
     * Creates a Builder so classes can be registered before creating an immutable Provider.  This Builder will use the default
     * Conventions configured according to the given options.
     *
     * @param options the options to apply to class mapping
     * @return the Builder
     * @see Builder#register(Class[])
     */
    public static Builder builder(final ConventionOptions options) {
        List<Convention> conventions = new ArrayList<Convention>();
        conventions.add(new FieldSelectionConvention());
        FieldStorageConvention convention = new FieldStorageConvention();
        convention.setStoreNulls(options.isStoreNullFields());
        convention.setStoreEmpties(options.isStoreEmptyFields());
        conventions.add(convention);
        conventions.add(options.getCollectionNamingConvention());
        conventions.add(options.getPropertyNamingConvention());
        conventions.add(new DefaultAnnotationConvention());

        return new Builder(conventions);
    }

    /**
     * Creates a Builder so classes can be registered before creating an immutable Provider.  This Builder will use the default
     * Conventions configured according to the given options.
     *
     * @param conventions the Conventions to apply to class mapping
     * @return the Builder
     * @see Builder#register(Class[])
     */
    public static Builder builder(final List<Convention> conventions) {
        return new Builder(conventions);
    }

    @Override
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        return registered.contains(clazz) ? new PojoCodec<T>(new ClassModel(registry, clazz), registry) : null;
    }

    /**
     * A Builder for the PojoCodecProvider
     */
    public static class Builder {
        private final Set<Class<?>> registered = new HashSet<Class<?>>();
        private final List<ClassModel.Builder> pojoBuilders = new ArrayList<ClassModel.Builder>();
        private final List<Convention> conventions;

        private Builder(final List<Convention> conventions) {
            this.conventions = conventions;
        }

        static List<Class> extract(final ResolvedType type) {
            List<Class> classes = new ArrayList<Class>();
            Class erasedType = type.getErasedType();
            if (Collection.class.isAssignableFrom(erasedType)) {
                ResolvedType collectionType = type.getTypeParameters().get(0);
                Class containerClass;
                if (Set.class.equals(erasedType)) {
                    containerClass = HashSet.class;
                } else if (List.class.equals(erasedType) || Collection.class.equals(erasedType)) {
                    containerClass = ArrayList.class;
                } else {
                    containerClass = erasedType;
                }
                classes.add(containerClass);
                classes.addAll(extract(collectionType));
            } else if (Map.class.isAssignableFrom(erasedType)) {
                List<ResolvedType> types = type.getTypeParameters();
                ResolvedType keyType = types.get(0);
                ResolvedType valueType = types.get(1);
                if (!keyType.getErasedType().equals(String.class)) {
                    throw new CodecConfigurationException(format("Map key types must be Strings.  Found %s instead.",
                                                                 keyType.getErasedType()));
                }
                Class<?> containerClass;
                if (Map.class.equals(erasedType)) {
                    containerClass = HashMap.class;
                } else {
                    containerClass = erasedType;
                }
                classes.add(containerClass);
                classes.addAll(extract(valueType));
            } else {
                classes.add(type.getErasedType());
            }

            return classes;
        }

        /**
         * Creates the PojoCodecProvider with the classes that have been registered.
         *
         * @return the Provider
         * @see #register(Class...)
         */
        public PojoCodecProvider build() {
            for (Class<?> type : registered) {
                pojoBuilders.add(map(type));
            }
            List<ClassModel> models = new ArrayList<ClassModel>();
            for (ClassModel.Builder builder : pojoBuilders) {
                for (Convention convention : conventions) {
                    convention.apply(builder);
                }

                builder.build();
            }
            return new PojoCodecProvider(models, conventions);
        }

        /**
         * Registers a class with the builder for inclusion in the Provider.  This method allows for explicit, programmatic configuration
         * of the mapping information for the class.
         *
         * @param type the type to add
         * @return this
         */
        public ClassModel.Builder buildClassModel(final Class<?> type) {
            ClassModel.Builder builder = ClassModel.Builder.builder(type);
            pojoBuilders.add(builder);
            return builder;
        }

        /**
         * Registers a class with the builder for inclusion in the Provider.  Registration will automatically map the classes using the
         * mapping configuration defined on the builder.
         *
         * @param classes the classes to register
         * @return this
         */
        public Builder register(final Class<?>... classes) {
            for (Class<?> aClass : classes) {
                registered.add(aClass);
            }
            return this;
        }

        public Builder registerPackages(final String... packages) {
            return this;
        }

        public Builder registerPackages(final Class... classes) {
            return this;
        }

        private ClassModel.Builder map(final Class<?> type) {
            ClassModel.Builder builder = ClassModel.Builder.builder(type);
            TypeResolver resolver = new TypeResolver();

            MemberResolver memberResolver = new MemberResolver(resolver);
            ResolvedType resolved = resolver.resolve(type);
            ResolvedTypeWithMembers resolvedType =
                memberResolver.resolve(resolved, new StdConfiguration(AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED), null);

            builder.collection(type.getSimpleName());
            builder.discriminator(type.getName());
            builder.useDiscriminator(true);

            for (final ResolvedField field : resolvedType.getMemberFields()) {
                map(builder, field);
            }

            return builder;
        }

        private void map(final ClassModel.Builder builder, final ResolvedField resolvedField) {
            Field rawField = resolvedField.getRawMember();
            Class<?> erasedType = resolvedField.getType().getErasedType();

            FieldModel.Builder fieldModel =
                builder.addField(resolvedField.getName())
                       .type(erasedType, extract(resolvedField.getType()))
                       .typeName(erasedType.equals(Object.class) ? rawField.getGenericType().toString() : null);

            TypeBindings bindings = resolvedField.getType().getTypeBindings();

            for (int index = 0; index < bindings.size(); index++) {
                fieldModel.bindType(bindings.getBoundName(index), bindings.getBoundType(index).getErasedType());
            }
        }
    }
}
