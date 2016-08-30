POJO Codec Configuration
======

In Morphia, class mapping is done lazily and so can be transparently done at runtime.  Alternately, users can call `morphia.map(SomeClass.class)` to explicitly enumerate the classes to map.  There's also a method to map an entire package.  

PojoCodecProvider
-----
The entry point for configuring the driver to work with user defined POJOs is the `PojoCodecProvider`.  When creating a provider, a `Builder` is provided through which classes may be registered.  A `Builder` is created through one of three helper methods on `PojoCocdecProvider`:  

1. `build()`
1. `build(ConventionOptions)`
1. `build(List<Convention>)`

`build()` assumes default values on all mapping options and `Convention` instances applied to the registered classes.  `build(ConventionOptions)` allows a user to start with the default list of `Convention` instances and tweak various settings.  `build(List<Convention>)` allows a user to completely bypass the default settings and provide a customized list of `Convention` instances to apply

Classes passed to the `register()` method are processed with the `Convention` list configured on the builder.  If the user does not provide a list of `Convention` instances, the default list is used.  The models built by this mapping are immutable and can not be changed once passed on to the `PojoCodecProvider`.  

For these definitions there is `PojoCodecProvider.Builder#buildModel(Class)`.  This method returns a `ClassModel.Builder` that allows for programmatic addition of fields and mapping data.  The API for `PojoCodecProvider.Builder` is as follows:

```java
class PojoCodecProvider.Builder {
	PojoCodecProvider.Builder 	register(Class<?>... classes)
	PojoCodecProvider.Builder 	register(ClassModel.Builder... list) 
	PojoCodecProvider.Builder 	registerPackages(Class... classes)
	PojoCodecProvider.Builder 	registerPackages(String... names)
		
	PojoCodecProvider 			build()
}
```

1.  `register(Class<?>...)` will accept any number of `Class` references and apply default mapping logic to them.  Classes can also be registered by package.  
2. `register(ClassModel.Builder...)` allows users to register manually assembled `ClassModel.Builder` instances for inclusion
2. `registerPackages(Class...)` will use the package of the given class to discover the packages to map. 
2. `registerPackages(String...)` will register the package names to map. 

ClassModel.Builder
----
The `ClassModel.Builder` provides the API defining the mapping logic for the underlying `ClassModel` used by the `PojoCodec`.  The API for `ClassModel.Builder` looks like this:

```java
class ClassModel.Builder {
	FieldModel.Builder 			addField(Field field)
	FieldModel.Builder 			addField(String name)
	ClassModel.Builder 			apply(List<Convention> conventions)
	ClassModel.Builder 			collection(String value)
	ClassModel.Builder 			discriminator(String value)
	FieldModel.Builder 			field(String name)
	List<Annotation> 			getAnnotations() 
	FieldModel.Builder 			getField(String name)
	List<FieldModel.Builder>	getFields()
	Class<?>					getType() 
	String 						getTypeName() 
	ClassModel.Builder 			idField(String name)
	ClassModel.Builder 			idGenerator(IdGenerator generator)
	ClassModel.Builder 			map()
	ClassModel.Builder 			useDiscriminator(Boolean value)

	ClassModel 					build()
}
```

1. The `collection()` setting determines which collection this entity will be mapped to.  If unset, the default mapping would to a collection named with the entity's class name.  
2. `idField(String)` specifies which field on the entity is the ID field.  
3. For polymorphic queries and collections, discriminators are used to distinguish one type from another.  The `discriminatorName()` setting determines what field in the document stored in MongoDB stores this information.  
4. `discriminator(String)` determines what value is used to denote the type of the entity.  For example, this discriminator might be stored in a field called `_t` and hold the fully qualified class name of the entity, e.g., `com.acme.Widget`.

FieldModel.Builder
-----
`FieldModel.Builder` instances allow for configuring mapping information about the fields on an entity.

```java
class FieldModel.Builder {
	List<Annotation> 	annotations() 
	FieldModel.Builder 	annotations(Annotation[] annotations) 
	FieldModel.Builder 	bindType(String name, Class<?> type) 
	FieldModel.Builder 	documentFieldName(String name)
	FieldModel.Builder 	include(boolean include) 
	FieldModel.Builder 	storeEmptyFields(boolean storeEmptyFields) 
	FieldModel.Builder 	storeNullFields(boolean storeNullFields) 
	FieldModel.Builder 	type(Class type, List<Class<?>> parameters) 
	FieldModel.Builder 	typeName(String name) 
	FieldModel.Builder 	useDiscriminator(boolean useDiscriminator) 

	FieldModel        build()
}
```

2. `documentFieldName(String)` sets the name to be used in MongoDB when serializing a field to the database.  This defaults to the Java field name.
2. `type(Class<?>)` sets the type of the field.  Generally, this will be a single class reference such as `String.class`.  If the type can be parameterized, however, mutiple classes can be given to represent the parameterized types.  e.g.. if the field is a `List<String>`, this method would then called like this:  `builder.type(List.class, asList(String.class))`.
3. `storeNullFields(boolean)` instructs the mapper how to handle null values.  When `true`, if a field is null in a Java object that null will be stored in the document in MongoDB as well.  If `false`, that value will not be stored and the mapped key name will not be present in the document at all.
4. `storeEmptyFields(boolean)` applies similar logic to "container types."  If field is a `Collection` or a `Map`, it's value might be non-null but it might also be empty.  That is, it's `size()` returns 0.  In these cases, it is often desirable to not store that field at all and thus save some space on disk.  In this case, `builder. storeEmptyFields(false)` would prevent the mapper from storing those empty values.  This setting has no effect on fields that are not a `Collection` or a `Map`.  **The default behavior is to not store null and empty values.**
5. `useDiscriminator(boolean)` instructs the mapper whether or not to inject the mapped entity's discriminator value in to the persisted document.  This setting only applies to class mapped through the `PojoCodecProvider`.  By default, this discriminator value *is* stored but under certain conditions it can be advantageous to suppress this value.  For example, if the field is an `Address` and there are no known subclasses of `Address` (e.g., perhaps `Address` is final) it can be assumed that there will only be documents that can map to `Address` in the database.  In this situation, storing the discriminator value is redundant and wasteful and can be suppressed.
6. `include(boolean)` can be largely ignored by most developers.  This setting is intended to indicate that a field should or should not be included in serialization.  When building a model manually, if a field shouldn't be included, it simply shouldn't be added.  This setting is used by the conventions system which will help apply general priniciples to mapping configuration rather than requiring explicit decisions at every step.
8. `build()` is used the `PojoCodec` builders to create the underlying model used by the codec.  It should never be necessary to call this method manually.

Putting it All Together
=====
Once the set of classes to be mapped is determined, mapping them and making them available to Java driver is quite simple:

```java
PojoCodecProvider codecProvider = PojoCodecProvider
	.builder()
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
Conventions provide a more automated approach to configuring class mapping.  Rather than explicitly making every decision about how to map classes, conventions define a set of broad guidelines that are applied across the range of mapped classes.  Conventions are optionally passed to `PojoCodecProvider.builder()` and applied to every class registered with the builder.  If no list is passed, a default list of Conventions are used.  Users can also provide a `ConventionOptions` instance to configure this set of default Conventions.  If no options are passed, default options are applied.  To sum up, users can pass in:

1. Nothing
2. A `ConventionOptions` instance
3. a `List<Conventions>`

Each option gives increasing customization of mapping behavior.

While these conventions can be almost anything, there is a default set of conventions provided by the driver.  These conventions are configurable via the `ConventionOption.Builder` class.  This class looks like this:

```java
class ConventionOptions.Builder {
	ConventionOptions.Builder 	annotations(AnnotationConvention) 
	ConventionOptions.Builder 	collectionNaming(CollectionNamingConvention) 
	ConventionOptions.Builder 	propertyNaming(PropertyNamingConvention) 
	ConventionOptions.Builder 	storeEmptyFields(boolean) 
	ConventionOptions.Builder 	storeFinalFields(boolean) 
	ConventionOptions.Builder 	storeNullFields(boolean) 
	ConventionOptions.Builder 	storeStaticFields(boolean) 
	ConventionOptions.Builder 	storeTransientFields(boolean) 
	ConventionOptions.Builder 	useNamedIdField(boolean) }

	ConventionOptions 	build() 
```

`Convention` instances will be applied in order via the `apply()` method.  The `Convention` is API is a simple interface:

```java
interface Convention {
 	apply(ClassModel.Builder model)
} 	
```

Annotations
===
Some basic annotations are provided as part of the `PojoCodec` system providing support for mapping configuration on each class to be mapped.  Support for custom annotations can be introduced by providing a custom implementation of `AnnotationConvention`.  The provided annotations are as follows:

1.  `@Entity` -- This annotations allows for the configuration of a custom collection name as well as toggling whether the discriminator is stored in the serialized documents.  If only one type is ever stored in the mapped collection, the discriminator value is an unnecessary use of space and can be suppressed with this annotation.
2. `@Discriminator` -- This annotation allows for explicit discriminator values for a given type.  In some cases, the discriminator value configured by the general conventions system may not be correct.  This annotation allows developers to override that value.
3. `@Id` --  This annotation marks a field as the ID field for the type.  When annotated with this, the document field name will be set to `_id`.  Using this annotation will clear any manually configured ID field set on a `FieldModel.Builder`.
4. `@Property` -- This annotation allows for explicit configuration of the document field name.  This annotation can also be used to suppress discriminator usage on embedded entity types.  If a mapped class has no subclasses or field will only ever have a reference to the declared field type and not a subclass, the discriminator can be an unnecessary use of space.  Additionally, when applied to say, a `List<Foo>` where the list will only ever hold `Foo` references and not subclasses this annotation can dramatically decrease disk usage.
