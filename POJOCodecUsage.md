POJO Codec Configuration
======

In Morphia, class mapping is done lazily and so can be transparently done at runtime.  Alternately, users can call `morphia.map(SomeClass.class)` to explicitly enumerate the classes to map.  There's also a method to map an entire package.  

PojoCodec
-----
The entry point for configuring the driver to work with user defined POJOs is the `PojoCodec`.  When creating a provider, a `Builder` is provided through which classes may be registered.  Classes passed to the `register()` method are mapped according to the default logic as defined by the driver.  The models built by this mapping are immutable and can not be changed once passed on to the `PojoCodecProvider`.  

For these definitions there is `PojoCodecBuilder#buildModel(Class)`.  This method returns a `ClassModelBuilder` that allows for programmatic addition of fields and mapping data.  The API for `PojoCodecProvider.PojoCodecProviderBuilder` is as follows:

```java
    class PojoCodecProviderBuilder {
		PojoCodecProviderBuilder register(Class<?>... classes)
		PojoCodecProviderBuilder registerPackages(Class... classes) 
		PojoCodecProviderBuilder registerPackages(String... packages) 
		PojoCodecProviderBuilder setConventionPack(ConventionPack conventionPack)
		ClassModelBuilder buildClassModel(Class<?> type)

		PojoCodecProvider build()
}
```

1.  `register()` will accept any number of `Class` references and apply default mapping logic to them.  Classes can also be registered by package.  
2. `registerPackages(String...)` takes the names of the packages to map.  Alternately, `registerPackages(Class...)` will use the package of the given class to discover other packages to map.  In either case, every class found in the packages specified will be mapped. 
3. `buildClassModel(Class<?>)` allows developers to explicitly configure mapping data for a class.  `build()` is called once class mapping is done and the configurations are ready to be passed in to a `CodecRegistry`.

ClassModelBuilder
----
The `ClassModelBuilder` provides the API defining the mapping logic for the underlying `ClassModel` used by the `PojoCodec`.  The API for `ClassModelBuilder` looks like this:

```java
class ClassModelBuilder {
	FieldModelBuilder addField(String name) 
	ClassModelBuilder collection(String value) 
	ClassModelBuilder discriminator(String value) 
	List<Annotation> getAnnotations() 
	List<FieldModelBuilder> getFields() 
	Class<?> getType() 
	String getTypeName() 
	ClassModelBuilder subclass(Class<?> type) 
	ClassModelBuilder useDiscriminator(Boolean value) 

	ClassModel build() 
}
```

1. The `collection()` setting determines which collection this entity will be mapped to.  If unset, the default mapping would to a collection named with the entity's class name.  
2. The `id` value specifies which field on the entity is the ID field.  
3. For polymorphic queries and collections, discriminators are used to distinguish one type from another.  The `discriminatorName()` setting determines what field in the document stored in MongoDB stores this information.  
4. `discriminatorValue()` determines what value is used to denote the type of the entity.  For example, this discriminator might be stored in a field called `_t` and hold the fully qualified class name of the entity, e.g., `com.acme.Widget`.
5. `build()` is used the `PojoCodec` builders to create the underlying model used by the codec.  It should never be necessary to call this method manually.

When building type hierarchies, building each `ClassModel` individually would result in much duplication.  To avoid this, there is the `subclass()` method.  The method returns a new `ClassModelBuilder` instance representing the subclass.  For these instances, values such as the ID and discriminator fields will be "inherited" by the new `ClassModelBuilder` so these settings need not be duplicated.  The collection name will also be inherited resulting in a single collection for the entire hierarchy. All of these settings can be overriden but care should be taken when changing the `discriminatorName()` value.  Changing this value or `collection()` on subtypes will interfere with polymorphic queries.

FieldModelBuilder
-----
`FieldModelBuilder` instances allow for configuring mapping information about the fields on an entity.

```java
class FieldModelBuilder {
	FieldModelBuilder documentFieldName(String name)
	FieldModelBuilder idField(boolean idField) 
	FieldModelBuilder include(boolean include) 
	FieldModelBuilder storeEmptyFields(boolean storeEmptyFields) 
	FieldModelBuilder storeNullFields(boolean storeNullFields) 
	FieldModelBuilder type(Class type) 
	FieldModelBuilder type(Class type, List<Class> parameters) 
	FieldModelBuilder typeName(String name) 
	FieldModelBuilder useDiscriminator(boolean useDiscriminator)     
	FieldModel        build()
}
```

2. `documentFieldName()` sets the name to be used in MongoDB when serializing a field to the database.  This defaults to the Java field name.
2. `type()` sets the type of the field.  Generally, this will be a single class reference such as `String.class`.  If the type can be parameterized, however, mutiple classes can be given to represent the parameterized types.  e.g.. if the field is a `List<String>`, this method would then called like this:  `builder.type(List.class, String.class)`.
3. `storeNulls()` instructs the mapper how to handle null values.  When `true`, if a field is null in a Java object that null will be stored in the document in MongoDB as well.  If `false`, that value will not be stored and the mapped key name will not be present in the document at all.
4. `storeEmpties()` applies similar logic to "container types."  If field is a `Collection` or a `Map`, it's value might be non-null but it might also be empty.  That is, it's `size()` returns 0.  In these cases, it is often desirable to not store that field at all and thus save some space on disk.  In this case, `builder.storeEmpties(false)` would prevent the mapper from storing those empty values.  This setting has no effect on fields that are not a `Collection` or a `Map`.  **The default behavior is to not store null and empty values.**
5. `useDiscriminator()` instructs the mapper whether or not to inject the mapped entity's discriminator value in to the persisted document.  This setting only applies to class mapped through the `PojoCodecProvider`.  By default, this discriminator value *is* stored but under certain conditions it can be advantageous to suppress this value.  For example, if the field is an `Address` and there are no known subclasses of `Address` (e.g., perhaps `Address` is final) it can be assumed that there will only be documents that can map to `Address` in the database.  In this situation, storing the discriminator value is redundant and wasteful and can be suppressed.
6. `included()` can be largely ignored by most developers.  This setting is intended to indicate that a field should or should not be included in serialization.  When building a model manually, if a field shouldn't be included, it simply shouldn't be added.  This setting is used by the conventions system which will help apply general priniciples to mapping configuration rather than requiring explicit decisions at every step.
7. `idField()` is used to mark a field as the `_id` field in a document.  When this is called on the builder, the `documentName` will be set to `_id`.  If this method is called on multiple fields on a `ClassModelBuilder`, the last one wins and the other fields' `documentName` values will revert back to the field names.
8. `build()` is used the `PojoCodec` builders to create the underlying model used by the codec.  It should never be necessary to call this method manually.

Putting it All Together
=====
Once the set of classes to be mapped is determined, mapping them and making them available to Java driver is quite simple:

```java
PojoCodecProvider codecProvider = PojoCodecProvider.builder()
	.register(SomeClass.class, SomeOtherClass.class)
	.build();
	
Builder builder = MongoClientOptions.builder()
 	.withCollectionMapper(codecProvider)
	// other configuration options
   .codecRegistry(fromRegistries(getDefaultCodecRegistry(),
   		fromProviders(codecProvider)));

MongoClient client = new MongoClient("localhost", builder.build());
```

With that, the new `MongoClient` is configured to work directly with POJOs when reading and writing documents.  Traditionally, getting data out of a collection has centered around the `Document` class.  With the `PojoCodec`, however, a POJO can be specified such that queries will return instances of your POJO rather than a `Document`.  Instead of getting a collection like this:

	client.getDatabase("db").getCollection("coll");

now collections can be properly typed to business types like this:

	client.getDatabase("db").getCollection(MyType.class);

This will return a `MongoCollection` instance for the collection which `MyType` has been mapped to.  Sometimes, however, a different collection is needed but for the same type.  Examples of this might be archival collections for old data or perhaps transient collections holding the results of an analytics job that might only live for a short period.  Fetching data from these collections is no different from the current approach:

	client.getDatabase("db").getCollection("coll", MyType.class);

In both of these last two cases, `getCollection()` returns an instance of a `MongoCollection<MyType>`.  From this point, nothing else changes.  The only difference is that documents returned from MongoDB are automatically mapped in to `MyType` instances instead of `Document` instances.

Conventions
======
Conventions provide a more automated approach to configuring class mapping.  Rather than explicitly making every decision about how to map classes, conventions define a set of broad guidelines that are applied across the range of mapped classes.  Conventions are bundled in to a `ConventionPack` and passed to the provider builder via `PojoCodecProviderBuilder.setConventionPack()`.   

While these conventions can be almost anything, there is a default set of conventions provided by the driver.  These conventions are configurable via the `ConventionPackBuilder` class.  This class looks like this:

```java
class ConventionPackBuilder {
    ConventionPackBuilder collectionNaming(CollectionNamingConvention)
    ConventionPackBuilder propertyNaming(PropertyNamingConvention)
    ConventionPackBuilder storeEmptyFields(boolean)
    ConventionPackBuilder storeFinalFields(boolean)
    ConventionPackBuilder storeNullFields(boolean)
    ConventionPackBuilder storeTransientFields(boolean)

    ConventionPackBuilder annotationConvention(AnnotationConvention)
    ConventionPackBuilder addConvention(Convention)
    ConventionPack build()
}
```

This builder applies certain defaults to class mapping:

1.  Classes are mapped collections using the lower case form of the simple class name.  For example, if the class `com.acme.foo.UserProfile` were being mapped, it would be mapped to the collection `userprofile`.  This can be changed using a number of predefined `CollectionNamingConvention ` types found in the `org.bson.codecs.pojo.conventions.naming` package.
2. Fields are mapped, by default, such that the names of the keys in documents in the database match the field names as defined in the Java source code.  Again, there are multiple implementions of `PropertyNamingConvention` if this default naming is not desired.  Users can use the provided implementations or implement more sophisticated variants as needed.
2. By default, `final` and `transient` fields are ignored.  This can be overridden by passing `true` to either method the `storeFinalFields ` or `storeTransientFields` methods.
3. Similarly, null values and empty `Map` or `Collection` instances are not saved to the database by default.  If these values *should* be persisted, `true` can be passed to either method.
4. By default, all models configured by the `ConventionPack` created by this builder are configured via an `AnnotationConvention` which, potentially, will overwrite any settings manually configured on a particular class or field.  Should this global configuration not be desired it can be disabled by passing `null` to `annotationConvention()`.  A reference to a custom implemention can also be passed to provide a more customized processing of annotations.

This is, of course, just one way to configure a `ConventionPack`.  Users are free to create custom implementations of `ConventionPack`s to achieve whatever configurations are desired.  A `ConventionPack` is a simple collection of `Convention` instances that get applied when the `PojoCodecProviderBuilder.build()` method is invoked.  A `ConventionPack` looks like this:

```java
class ConventionPack {
 	addConvention(Convention convention)
 	apply(ClassModelBuilder model)
} 	
```

`Convention` instances added to the pack and then will be applied in order via the `apply()` method.  Similarly, the `Convention` is API is a simple interface:

```java
interface Convention {
 	apply(ClassModelBuilder model)
} 	
```

Annotations
===
Some basic annotations are provided as part of the `PojoCodec` system providing support for mapping configuration on each class to be mapped.  Support for custom annotations can be introduced by providing a custom implementation of `AnnotationConvention`.  The provided annotations are as follows:

1.  `@Entity` -- This annotations allows for the configuration of a custom collection name as well as toggling whether the discriminator is stored in the serialized documents.  If only one type is ever stored in the mapped collection, the discriminator value is an unnecessary use of space and can be suppressed with this annotation.
2. `@Discriminator` -- This annotation allows for explicit discriminator values for a given type.  In some cases, the discriminator value configured by the general conventions system may not be correct.  This annotation allows developers to override that value.
3. `@Id` --  This annotation marks a field as the ID field for the type.  When annotated with this, the document field name will be set to `_id`.  Using this annotation will clear any manually configured ID field set on a `FieldModelBuilder`.
4. `@Property` -- This annotation allows for explicit configuration of the document field name.
5. `@SuppressDiscriminator` -- This annotation instructs the serializer to suppress the inclusion of the discriminator value.  If a mapped class has no subclasses or field will only ever have a reference to the mapped class and not a subclass, the discriminator can be an unnecessary use of space.  Additionally, when applied to say, a `List<Foo>` where the list will only ever hold `Foo` references and not subclasses this annotation can dramatically decrease disk usage.