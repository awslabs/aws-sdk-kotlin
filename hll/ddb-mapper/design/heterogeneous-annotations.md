# Design: Annotations for heterogeneous item schemas

**Status:** Implemented (uncommitted), see [Implementation notes](#implementation-notes)
**Issue:** [aws/aws-sdk-kotlin#1880](https://github.com/aws/aws-sdk-kotlin/issues/1880)

## Summary

Add annotations which let the schema generator build a `HeterogeneousItemConverter` for a family of types stored under a
string discriminator attribute. The family's base may be a sealed, abstract, or open type in a real hierarchy, or `Any`
for an ad-hoc family of unrelated classes. The generated converter works as a table item (with a schema and a
`getXTable` accessor) or as a nested attribute inside another mappable type.

## Background

Today, users build heterogeneous converters by hand:

```kotlin
val vehicleConverter = HeterogeneousItemConverter<Vehicle>(typeAttribute = "type") {
    exactType("car", CarConverter)
    exactType("bike", BikeConverter)
}

object VehicleSchema : ItemSchema.PartitionKey<Vehicle, Int> {
    override val converter = vehicleConverter
    override val partitionKey = KeySpec.int("id")
}
```

The builder DSL owns the type attribute exclusively: subconverters must not write it and never see it on read. Its
registrations are evaluated in order, first match wins, and registrations shadowed by an earlier *supertype*
registration cannot be detected at runtime. Codegen can do better on all counts: it knows the whole hierarchy, so it can
order registrations correctly, validate keys across subtypes, and emit the schema and table accessor.

For reference, the schema generator currently emits the following for a `@DynamoDbItem class User`:

* `UserBuilder` (when required)
* `object UserConverter : ItemConverter<User> by SimpleItemConverter(...)`
* `val UserValueConverter: ValueConverter<User>` (when `User` is referenced as a nested attribute)
* `object UserSchema : ItemSchema.PartitionKey<User, String>` (or `CompositeKey`)
* `fun DynamoDbMapper.getUserTable(name: String): Table.PartitionKey<User, String>`

For `@DynamoDbMappable` (nested-only) types, only the builder, converter, and value converter are generated.

## Goals

* One annotation marks the **base** of a heterogeneous family; one annotation marks each **subtype**.
* Support sealed, abstract, and open hierarchies, plus ad-hoc families whose base is `Any`.
* Require an explicit type name for every registered subtype.
* Declare primary keys on the base type's properties, on each subtype's properties, or both, as long as the effective
  keys agree across all subtypes.
* Support the family both as a table item and as a nested attribute (including in collections).
* Support multi-level hierarchies. An intermediate type takes part only if it is annotated itself.
* Let an open concrete base register its own instances alongside its subtypes.
* Expose both `exactType` and `instanceOf` matching.

## Non-goals

* Subtypes declared in a different module (or compilation) than the base. This matches today's limits on nested types.
* Cyclic heterogeneous nesting (e.g., a `Vehicle` subtype with a `Vehicle` property). This also matches today's limits.
* A fallback subtype or skip on unknown type names. Reads of unknown types throw `IllegalArgumentException`, as they do
  today.
* Generic base types or subtypes.

## Proposed API

```kotlin
/**
 * Specifies that this class/interface is the base of a heterogeneous family of mappable types, distinguished by a string
 * type name stored in [typeAttribute]. A `HeterogeneousItemConverter` will be generated for the annotated type from
 * every [DynamoDbSubtype] registered in this family.
 *
 * When applied to an `object`, the object serves only as a name for an ad-hoc family whose common type is [Any]. Such
 * a family's subtypes must name the object explicitly via [DynamoDbSubtype.parent].
 *
 * Combine with [DynamoDbItem] to make the family a table item (generating a schema and table accessor). Without
 * [DynamoDbItem], the family is usable only as a nested attribute.
 * @param typeAttribute The name of the attribute which stores the type name of each item
 * @param baseTypeName If non-empty, registers instances of the base type itself under this type name. Valid only on an
 * open concrete class. If empty (the default), the base type itself is not registered.
 * @param baseMatch How objects are matched to the base type's own registration. Ignored when [baseTypeName] is empty.
 */
@Target(AnnotationTarget.CLASS)
@DynamoDbMappable
public annotation class DynamoDbHeterogeneousType(
    val typeAttribute: String,
    val baseTypeName: String = "",
    val baseMatch: DynamoDbSubtypeMatch = DynamoDbSubtypeMatch.INSTANCE_OF,
)

/**
 * Specifies that this class/interface is a member of a [DynamoDbHeterogeneousType] family, stored under [typeName]. This
 * annotation implies [DynamoDbMappable].
 * @param typeName The value stored in the family's type attribute for objects matching this type. Must be non-blank
 * and unique within the family.
 * @param match How objects are matched to this type
 * @param parent The family this type belongs to. If not set, the family is inferred from this type's supertypes. Must
 * be set when the family's base is an ad-hoc `object`.
 * @param converter The item converter to be used for this type. If not set, one will be automatically generated.
 */
@Target(AnnotationTarget.CLASS)
@DynamoDbMappable
public annotation class DynamoDbSubtype(
    val typeName: String,
    val match: DynamoDbSubtypeMatch = DynamoDbSubtypeMatch.INSTANCE_OF,
    val parent: KClass<*> = Any::class, // sentinel: infer
    val converter: KClass<out ItemConverter<*>> = AutoGeneratedConverter::class,
)

/**
 * Determines how objects are matched to a [DynamoDbSubtype]. Mirrors the `HeterogeneousItemConverter.Builder` methods.
 */
public enum class DynamoDbSubtypeMatch {
    /** Matches objects whose class is exactly the annotated type. Corresponds to `Builder.exactType`. */
    EXACT_TYPE,

    /** Matches objects which are instances of the annotated type or any of its subtypes. Corresponds to `Builder.instanceOf`. */
    INSTANCE_OF,
}

/**
 * Specifies that this property's value is a member of the [DynamoDbHeterogeneousType] family [family], converted with
 * that family's generated value converter. Required for ad-hoc families, whose value type is [Any] and so can't be
 * inferred from the property's type.
 *
 * Applies to the property's family-typed value, including inside collections (e.g., `List<Any>`,
 * `Map<String, Any?>`).
 * @param family The family's base, which must be annotated [DynamoDbHeterogeneousType]
 */
@Target(AnnotationTarget.PROPERTY)
public annotation class DynamoDbHeterogeneousAttribute(val family: KClass<*>)
```

A single `@DynamoDbSubtype` with a `match` parameter keeps one annotation per concept. Two separate annotations
(`@DynamoDbExactType` / `@DynamoDbInstanceOf`) would duplicate the other three parameters.

`@DynamoDbHeterogeneousType` is meta-annotated with `@DynamoDbMappable` so the existing `isDynamoDbMappable` check picks up
properties typed as the base (e.g., `val vehicle: Vehicle`, `val fleet: List<Vehicle>`) with no extra work. It has no
`converter` parameter, since overriding the converter would make the annotation meaningless.

An open base registers itself through `baseTypeName` and `baseMatch` on `@DynamoDbHeterogeneousType`, not through a
`@DynamoDbSubtype` on the base. That's because `@DynamoDbSubtype` on a family base already has a meaning: the base is a
member of an *outer* family (see [Nested family](#nested-family)). An empty `baseTypeName` is an unambiguous "not
registered" sentinel, because a blank type name is otherwise an error.

`@DynamoDbSubtype.parent` is a single `KClass`, because a type belongs to exactly one family.

`@DynamoDbHeterogeneousAttribute` names the family's base rather than its generated value converter because the base is
in source when KSP validates the annotation (see [Ad-hoc family](#ad-hoc-family-keys-on-subtypes)). It's also valid
for a typed family (e.g. `Vehicle`), where it's redundant but harmless.

## Examples

### Sealed hierarchy as a table item, keys on the base

```kotlin
@DynamoDbItem
@DynamoDbHeterogeneousType(typeAttribute = "type")
sealed interface Vehicle {
    @DynamoDbPartitionKey val id: Int
    val manufacturer: String
}

@DynamoDbSubtype("car")
data class Car(override val id: Int, override val manufacturer: String, val model: String) : Vehicle

@DynamoDbSubtype("bike")
data class Bike(override val id: Int, override val manufacturer: String, val gears: Int) : Vehicle

@DynamoDbSubtype("unicycle")
data object Unicycle : Vehicle { override val id = 0; override val manufacturer = "n/a" }
```

Generated:

```kotlin
public object CarConverter : ItemConverter<Car> by SimpleItemConverter(...)
public object BikeConverter : ItemConverter<Bike> by SimpleItemConverter(...)
public object UnicycleConverter : ItemConverter<Unicycle> by ...

public object VehicleConverter : ItemConverter<Vehicle> by HeterogeneousItemConverter<Vehicle>("type") {
    instanceOf("car", CarConverter)
    instanceOf("bike", BikeConverter)
    instanceOf("unicycle", UnicycleConverter)
}

public val VehicleValueConverter: ValueConverter<Vehicle> = ConverterChain(VehicleConverter, ItemValueConverter)

public object VehicleSchema : ItemSchema.PartitionKey<Vehicle, Int> {
    override val converter: VehicleConverter = VehicleConverter
    override val partitionKey: KeySpec.Key1<Int> = KeySpec.int("id")
    override val attributes: Attributes = emptyAttributes()
}

public fun DynamoDbMapper.getVehicleTable(name: String): Table.PartitionKey<Vehicle, Int> =
    getTable(name, VehicleSchema)
```

Subtypes get builders, converters, and value converters as `@DynamoDbMappable` types do today, but no schema of their
own. (A subtype which is *also* annotated `@DynamoDbItem` still gets its own schema for standalone tables, independently
of the family.) A family's value converter is a `val`, like every other generated value converter.

### Ad-hoc family, keys on subtypes

```kotlin
@DynamoDbItem
@DynamoDbHeterogeneousType(typeAttribute = "kind")
object Entities

@DynamoDbSubtype("user", parent = Entities::class)
data class User(@DynamoDbPartitionKey val pk: String, @DynamoDbSortKey val sk: String, val name: String)

@DynamoDbSubtype("order", parent = Entities::class)
data class Order(@DynamoDbPartitionKey val pk: String, @DynamoDbSortKey val sk: String, val total: Long)
```

This generates `EntitiesConverter : ItemConverter<Any>`, `EntitiesSchema : ItemSchema.CompositeKey<Any, String,
String>`, and `DynamoDbMapper.getEntitiesTable(name)`. This fits the single-table design pattern.

An ad-hoc family can't be picked up from a property's type, because that type is `Any`. Name the family on the property
instead:

```kotlin
@DynamoDbMappable
data class AuditRecord(
    @DynamoDbHeterogeneousAttribute(Entities::class)
    val entity: Any,

    @DynamoDbHeterogeneousAttribute(Entities::class)
    val related: List<Any>,
)
```

Naming the family's generated `EntitiesValueConverter` in `@DynamoDbAttributeConverter` instead doesn't work: the class
doesn't exist until codegen runs, so KSP's `validate()` rejects the annotation argument, and since `HllKspProcessor`
runs only one round, `AuditRecord` would silently never be generated. `Entities` is in source, so naming it validates.

### Open concrete base

```kotlin
@DynamoDbHeterogeneousType(typeAttribute = "type", baseTypeName = "vehicle")
open class Vehicle(val manufacturer: String)

@DynamoDbSubtype("car")
class Car(manufacturer: String, val model: String) : Vehicle(manufacturer)
```

The base gets a builder (when required) and a `SimpleItemConverter` for its own properties, named `VehicleBaseConverter`
so it doesn't clash with the family's `VehicleConverter`:

```kotlin
public object VehicleBaseConverter : ItemConverter<Vehicle> by SimpleItemConverter(...)

public object VehicleConverter : ItemConverter<Vehicle> by HeterogeneousItemConverter<Vehicle>("type") {
    instanceOf("car", CarConverter)
    instanceOf("vehicle", VehicleBaseConverter)
}
```

The base's registration always comes last, after every registration of its subtypes. With the default
`baseMatch = INSTANCE_OF`, the base acts as a catch-all: unregistered subclasses are stored as `"vehicle"` and read back
as a plain `Vehicle`. With `baseMatch = EXACT_TYPE`, writing an unregistered subclass throws instead.

### Multi-level hierarchy

```kotlin
@DynamoDbHeterogeneousType(typeAttribute = "type")
sealed interface Vehicle

sealed class Truck : Vehicle                       // unannotated: transparent

@DynamoDbSubtype("pickup")
data class Pickup(val bedLength: Int) : Truck()    // registered in Vehicle

@DynamoDbSubtype("van")
open class Van(val seats: Int) : Vehicle           // default INSTANCE_OF: matches Van and any subclass of Van

@DynamoDbSubtype("tow-truck", match = EXACT_TYPE)
class TowTruck(seats: Int, val capacity: Int) : Van(seats)
```

This generates the following registrations. Codegen orders them so that `TowTruck` is registered before `Van`:

```kotlin
instanceOf("pickup", PickupConverter)
exactType("tow-truck", TowTruckConverter)
instanceOf("van", VanConverter)
```

An unannotated subclass of `Van` is stored as `"van"` and read back as a plain `Van`, losing its extra properties.
That's the documented behavior of `instanceOf`.

### Nested family

An intermediate type may be a family of its own, with its own type attribute:

```kotlin
@DynamoDbSubtype("truck")
@DynamoDbHeterogeneousType(typeAttribute = "truckType")
sealed interface Truck : Vehicle

@DynamoDbSubtype("pickup") data class Pickup(val bedLength: Int) : Truck
@DynamoDbSubtype("flatbed") data class Flatbed(val length: Int) : Truck
```

Here `VehicleConverter` registers `instanceOf("truck", TruckConverter)`, and `TruckConverter` is itself a
heterogeneous converter which writes `truckType`. `Pickup` is registered only in its nearest annotated family (`Truck`),
not in `Vehicle` as well.

## Codegen

The schema generator is split into three layers (#2059): `extraction/` turns KSP symbols into a KSP-free IR
(`model/`), reporting every problem to a `DiagnosticCollector`; `rendering/` emits code from the IR alone. Nothing is
rendered if any error was reported. `KspSeamGuardTest` keeps KSP out of `model/` and `rendering/`, so all family
analysis (membership, ordering, completeness, key and attribute merging) happens in extraction, and the renderer only
emits what the IR says.

### Discovery

1. `AnnotationsProcessor` queries `@DynamoDbHeterogeneousType` and `@DynamoDbSubtype` alongside the existing
   `@DynamoDbItem` / `@DynamoDbMappable` queries. (`getSymbolsWithAnnotation` does not expand meta-annotations, so each
   needs an explicit query; the existing `distinctBy` already handles classes found by more than one.)
2. `buildModel` resolves families in a pre-pass over the whole batch, after cycle detection and before per-class
   extraction. For each subtype:
   * If `parent` is set, that type must be annotated `@DynamoDbHeterogeneousType`, and the subtype must be a subtype of it
     (trivially true for `object` families).
   * Otherwise, walk the supertypes breadth-first. On each path, stop at the first `@DynamoDbHeterogeneousType` type found
     and skip unannotated intermediates. It is an error if no family is found, or if more than one is found (a type
     belongs to exactly one family).
3. Family bases are extracted by a new `buildFamily`, not `buildMappedItem`. Otherwise a sealed interface or ad-hoc
   `object` base would get a `SimpleItemConverter` (and builder) for its own properties.
4. Members are extracted by `buildMappedItem` with their family passed in. A member of a *table* family is not run
   through `validateNestedItem`, since its key, TTL and counter annotations are legal and are checked by the family
   rules instead. A member of a nested-only family keeps today's nested validation.

### IR

* `ItemConverterSpec` gains a `Heterogeneous(converter, typeAttribute, registrations)` variant. Each registration holds
  its type name, its match (`EXACT_TYPE` / `INSTANCE_OF`), and the member's converter `TypeRef`. Registrations are
  already sorted (see [Registration ordering](#registration-ordering)), so the renderer emits them in order.
* A family base is a `MappedItem` with that converter spec, no builder, and `SchemaMetadata` merged from its members.
  Its `name` is the base's simple name (`Vehicle`, `Entities`) and its `type` is the family's value type (`Vehicle`,
  or `Any` for an ad-hoc family). `SchemaRenderer`'s existing value converter, schema and `getXTable` rendering then
  work unchanged, because they only read `itemConverter.converter`, `type` and `metadata`.
* A self-registering base produces a second, nested-kind `MappedItem` for the base class itself, carrying the builder
  and a `Generated` converter named `<Base>BaseConverter`. It renders to its own file so it doesn't collide with the
  family's `<Base>Schema` file.
* A nested family (a type with both annotations) is a single `MappedItem`: its family converter is also the converter
  its outer family registers.

### Rendering

`SchemaRenderer` gains one branch, for `ItemConverterSpec.Heterogeneous`, which renders
`object XConverter : ItemConverter<T> by HeterogeneousItemConverter<T>(typeAttribute) { ... }`. No separate family
renderer is needed. `HighLevelRenderer` is unchanged.

### Cycles

`mappedTypeReferences` only follows property types, so `data class Car(val tow: Vehicle) : Vehicle` would not be seen
as a cycle. Add an edge from each family base to each of its members, so the existing cycle check reports it.

### Heterogeneous attributes

`resolvePropertyConverter` reads `@DynamoDbHeterogeneousAttribute` alongside `@DynamoDbAttributeConverter` and passes the
family to `resolveValueConverter`. While recursing through nullability and collections, a leaf whose type equals the
family's value type (`Any` for an ad-hoc family) resolves to `ValueConverterSpec.NestedRef` of the family's generated
value converter, before the scalar lookup that would otherwise reject `Any`.

`mappedTypeReferences` adds an edge from the property's class to the family base. That makes the family's value
converter generated (it's "referenced"), and lets the existing cycle check see cycles through the annotation.

### Sealed completeness

For a sealed family base, codegen enumerates every concrete class that descends from it, looking through unannotated
intermediate types (`getSealedSubclasses()`, recursively). It is a compile error if any of those classes is not matched
by some registration, whether that's its own `EXACT_TYPE` or an `INSTANCE_OF` on one of its ancestors. Without this
check, every write of such a class would fail at runtime. Open and abstract families can't be enumerated, so they get
no such check.

### Registration ordering

Codegen sorts registrations topologically so that a registration whose class is a subtype of another's class comes
first. Ties are broken by qualified name so the output is deterministic. This prevents the undetectable-at-runtime
shadowing described in the `Builder` docs.

### Keys

For table families (`@DynamoDbItem` + `@DynamoDbHeterogeneousType`), compute each registered subtype's *effective keys*.
These are the partition and sort key properties found in `getMappedProperties()`, including keys whose annotation is on
an overridden base property. Resolve those via `KSPropertyDeclaration.findOverridee()`, because the override itself
doesn't carry the base's annotation.

Every subtype must have identical effective keys: the same attribute names, the same key types, and the same order. The
family schema uses those keys. Each member's keys go through the existing `validateTableItemKeys` cardinality check and
`resolveKeyAttribute`, and the resulting `KeyAttribute` lists are compared. For `INSTANCE_OF` registrations of a nested family, the effective keys are those of the
nested family.

For nested-only families, key annotations anywhere in the family are errors, as they are on nested types today.

### TTL and counter attributes

For table families, `@DynamoDbTtlSeconds` and `@DynamoDbCounter` are allowed on any member, including on base
properties. Each member's are extracted with the existing `ttlFields` / `counterFields`, then merged into the family's
`SchemaMetadata`:

* TTL fields are unioned by property. It's an error if the same property is given different lifetimes on different
  members.
* Counter fields are unioned by property. The existing check (`Int` or `Long` only) applies to every member.

For nested-only families, TTL and counter annotations are errors, as they are on nested types today.

### Type attribute

The family converter uses the builder DSL, so the type attribute is exclusive, as in today's builder behavior. For a
nested family, the inner and outer type attributes must differ.

## Validation (compile errors)

Every row is reported through `DiagnosticCollector` against the offending symbol, so all family problems in a build are
reported together, as other schema errors now are.

| Condition | Message gist |
|---|---|
| `typeAttribute` or `typeName` blank | must not be blank |
| Duplicate `typeName` within a family | already registered |
| A mapped property's attribute name equals the family's type attribute (on any member, including an inherited property) | collides with type attribute |
| Nested family reuses its parent family's type attribute | collides with outer type attribute |
| `@DynamoDbHeterogeneousType` on a final class, enum, or annotation class | cannot have subtypes |
| `@DynamoDbHeterogeneousType` family with no members | at least one subtype required |
| Non-empty `baseTypeName` on an interface, abstract or sealed class, or `object` family | base cannot be instantiated |
| `baseTypeName` duplicates a member's `typeName` | already registered |
| Sealed family with a concrete descendant not matched by any registration | uncovered subtype, naming it |
| Type resolves to more than one family | belongs to multiple families |
| Conflicting TTL lifetimes for the same property across members | TTL conflict, naming both members |
| TTL or counter annotations in a nested-only family | only valid on table items |
| `EXACT_TYPE` on an interface or abstract class | can never match |
| `INSTANCE_OF` on an interface or abstract class with neither a custom `converter` nor its own `@DynamoDbHeterogeneousType` | no way to construct instances on read |
| `@DynamoDbSubtype` with no resolvable family, or `parent` not a supertype | no family |
| Ad-hoc (`object`) family member without explicit `parent` | parent required |
| Table family members with mismatched effective keys | key mismatch, naming both subtypes |
| Key annotations in a nested-only family | only valid on table items |
| `@DynamoDbItem(converter = ...)` combined with `@DynamoDbHeterogeneousType` | conflicting converters |
| `@DynamoDbItem(converter = ...)` and `@DynamoDbSubtype(converter = ...)` both set on one class | conflicting converters |
| `@DynamoDbHeterogeneousAttribute.family` not annotated `@DynamoDbHeterogeneousType` | not a family |
| `@DynamoDbHeterogeneousAttribute` on a property with no value of the family's value type | type mismatch |
| `@DynamoDbHeterogeneousAttribute` combined with `@DynamoDbAttributeConverter` | conflicting converters |
| Member declared in another module | unsupported (existing limitation) |
| Cycle through a family | unsupported (existing limitation) |

## Compatibility

This change is purely additive. The three annotations and one enum become new public API in `dynamodb-mapper-annotations`,
and the committed `.api` files will need updating. Existing generated code is unchanged.

The enum is a public exhaustiveness contract. Adding a constant later would break source compatibility for any consumer
with an exhaustive `when`, which is unlikely for an annotation argument, but it's a trade-off we accept.

`SchemaModelBuilder.specifiedItemConverter` currently picks one marker annotation by item kind and reads its converter
through `arguments.singleOrNull()`. Because `@DynamoDbSubtype` has several arguments, it must also consider
`@DynamoDbSubtype` and read the converter by name.

## Testing

* Add schema-generator-plugin functional tests (the `nested/` layout) covering sealed, abstract, open, ad-hoc, `object`
  subtypes, multi-level (transparent and annotated intermediates), nested families, nested use in properties and lists,
  keys on the base vs. on subtypes, and custom subtype converters.
* Add tests for a self-registering open base (both `baseMatch` values), ad-hoc nested use through
  `@DynamoDbHeterogeneousAttribute` (scalar, nullable, list and map values), and TTL and counter attributes merged
  across members.
* Add a negative test per validation row.
* Add extraction unit tests (with the `KspFakes` mocks) for family resolution, registration ordering, sealed
  completeness and key/attribute merging, plus a `ValueConverterRenderingTest`-style test for the heterogeneous
  converter branch.
* Add round-trip tests against DynamoDB Local for a table family and a nested family.

## Decisions

* **Naming:** `@DynamoDbHeterogeneousType`, `@DynamoDbSubtype`, `DynamoDbSubtypeMatch`, `@DynamoDbHeterogeneousAttribute`.
* **Family membership:** a type belongs to exactly one family, so `parent` is a single `KClass`.
* **TTL and counter annotations:** allowed on any member of a table family and merged across the family.
* **Sealed completeness:** an uncovered concrete descendant is a compile error.
* **Open concrete base:** it may register itself through `baseTypeName` / `baseMatch` (default
  `INSTANCE_OF`, so the base is a catch-all), and gets its own `<Base>BaseConverter`.
* **Ad-hoc nested use:** through `@DynamoDbHeterogeneousAttribute(family)` on the property, not by naming the generated
  value converter, which KSP can't validate in a single round.
* **Family value converters:** `val`s, like other generated value converters. The earlier `object` form existed only
  for naming them in `@DynamoDbAttributeConverter`.
* **`parent` sentinel:** `Any::class` means "infer from supertypes", which is unambiguous because `Any` can't carry
  `@DynamoDbHeterogeneousType`.

## Implementation notes

Where the implementation refines or departs from the sections above:

* **Generated converter shape:** the family converter delegates to the builder factory with named arguments
  (`HeterogeneousItemConverter<Vehicle>(typeAttribute = "type", block = { exactType<Car>("car", CarConverter) ... })`),
  passing each registration's type explicitly.
* **Annotation inheritance:** only family members (and bases) read annotations through `findOverridee()`; other classes
  keep today's direct-annotation behavior, so existing generated output is unchanged.
* **`object` members:** get no builder; their converter's factory returns the object itself, getters are lambdas, and
  setters are no-ops.
* **A member that is also `@DynamoDbItem`:** still gets its own schema. Its key/TTL/counter problems are reported once,
  by its own extraction, not again by the family.
* **Code layout:** family discovery and structural validation are in `extraction/Families.kt`; IR building and
  key/TTL/counter merging are in `extraction/FamilyModelBuilder.kt`.
* **Not yet done:** round-trip tests against DynamoDB Local. Plugin tests cover generated output, round-trips through
  the generated converters, and every validation row exercised in `heterogeneous/negative/FamilyProblems.kt`.
