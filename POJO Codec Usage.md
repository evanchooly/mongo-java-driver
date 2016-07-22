POJO Codec Configuration
======

In Morphia, class mapping is done lazily and so can be transparently done at runtime.  Alternately, users can call `morphia.map(SomeClass.class)` to explicitly enumerate the classes to map.  There's also a method to map an entire package.  With the POJO, on the fly mapping isn't allowed since the `CodecProvider` needs to be immutable once it's been integrated in to a `CodecRegistry`.

PojoCodec
-----
The entry point for configuring the driver to work with user defined POJOs is the `PojoCodec`.  When creating a provider, a `Builder` is provided through which classes may be registered.  Classes passed to the `register()` method are mapped according to the default logic as defined by the driver.  The models built by this mapping are immutable and can not be changed once passed on to the driver.  

For these definitions there is `PojoCodecBuilder#buildModel(Class)`.  This method returns a `ClassModelBuilder` that allows for programmatic addition of fields and mapping data.  The API for `PojoCodecProvider.Builder` is as follows:

```java
    class Builder {
       Builder register(Class<?>...)
       Builder register(String...)
       ClassModelBuilder buildModel(Class<?>)
       PojoCodecProvider build()
    }
```
`register()` will accept any number of `Class` references and apply default mapping logic to them.  Alternatively, classes can be registered by package.  In this case, every class found in the packages specified will be mapped.  `buildModel()` allows developers to explicitly configure mapping data.  `build()` is called once class mapping is done and the configurations are ready to be passed in to the `CodecRegistry`.

ClassModelBuilder
----
The `ClassModelBuilder` provides the API defining the mapping logic for the underlying `ClassModel` used by the `PojoCodec`.  The API for `ClassModelBuilder` looks like this:

```java
class ClassModelBuilder {
    ClassModelBuilder(Class<?>)
    ClassModelBuilder collection(String)
    ClassModelBuilder id(String)
    ClassModelBuilder discriminatorName(String)    
    ClassModelBuilder discriminatorValue(String)    
    ClassModelBuilder useDiscriminator(Boolean)
    FieldModelBuilder addField(String)
    ClassModel build()
}
```

1. The `collection()` setting determines which collection this entity will be mapped to.  If unset, the default mapping would to a collection named with the entity's class name.  
2. The `id` value specifies which field on the entity is the ID field.  
3. For polymorphic queries and collections, discriminators are used to distinguish one type from another.  The `discriminatorName()` setting determines what field in the document stored in MongoDB stores this information.  
4. `discriminatorValue()` determines what value is used to denote the type of the entity.  For example, this discriminator might be stored in a field called `_t` and hold the fully qualified class name of the entity, e.g., `com.acme.Widget`.

FieldModelBuilder
-----
`FieldModelBuilder` instances allow for configuring mapping information about the fields on an entity.

```java
class FieldModelBuilder {
    FieldModelBuilder documentName(String)
    FieldModelBuilder type(Class<?>, Class<?>...)
    FieldModelBuilder storeNulls(boolean);
    FieldModelBuilder storeEmpties(boolean);
    FieldModelBuilder useDiscriminator(boolean);
    FieldModelBuilder included(boolean);
    FieldModelBuilder idField();
}
```

1.  `documentName()` sets the name to be used in MongoDB when serializing a field to the database.  This defaults to the Java field name.
2. `type()` sets the type of the field.  Generally, this will be a single class reference such as `String.class`.  If the type can be parameterized, however, mutiple classes can be given to represent the parameterized types.  e.g.. if the field is a `List<String>`, this method would then called like this:  `builder.type(List.class, String.class)`.
3. `storeNulls()` instructs the mapper how to handle null values.  When `true`, if a field is null in a Java object that null will be stored in the document in MongoDB as well.  If `false`, that value will not be stored and the mapped key name will not be present in the document at all.
4. `storeEmpties()` applies similar logic to "container types."  If field is a `Collection` or a `Map`, it's value might be non-null but it might also be empty.  That is, it's `size()` returns 0.  In these cases, it is often desirable to not store that field at all and thus save some space on disk.  In this case, `builder.storeEmpties(false)` would prevent the mapper from storing those empty values.  This setting has no effect on fields that are not a `Collection` or a `Map`.  **The default behavior is to not store null and empty values.**
5. `useDiscriminator()` instructs the mapper whether or not to inject the mapped entity's discriminator value in to the persisted document.  This setting only applies to class mapped through the `PojoCodecProvider`.  By default, this discriminator value *is* stored but under certain conditions it can be advantageous to suppress this value.  For example, if the field is an `Address` and there are no known subclasses of `Address` (e.g., perhaps `Address` is final) it can be assumed that there will only be documents that can map to `Address` in the database.  In this situation, storing the discriminator value is redundant and wasteful and can be suppressed.
6. `included()` can be largely ignored by most developers.  This setting is intended to indicate that a field should or should not be included in serialization.  When building a model manually, if a field shouldn't be included, it simply shouldn't be added.  This setting is largely for use by the conventions system which will help apply general priniciples to mapping configuration rather than requiring explicit decisions at every step.
7. `idField()` is used to mark a field as the `_id` field in a document.  When this is called on the builder, the `documentName` will be set to `_id`.  If this method is called on multiple fields on a `ClassModelBuilder`, the last one wins and the other fields' `documentName` values will revert back to the field names.

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
TBD