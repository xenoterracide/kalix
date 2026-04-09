---
name: java
description: |
  Write code in the Java programming language. ALWAYS apply when creating
  or modifying `.java` source files.
---

<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
-->

Use for writing Java code in `.java` files.

## Design Principles

Let your domain language define the responsibilities in your system. Build each unit—object, function, or module—around a single responsibility derived from that language. Encapsulate behavior so it's polymorphic, letting the unit decide how to act rather than orchestrating externally. If you follow these principles, your code will naturally be composable, clear, and aligned with the domain.

- prefer immutability over mutability
  - prefer `final` for fields unless they need to be mutable.
  - prefer `record` classes for simple data carriers.
  - `var strings = List.of("foo");` over `var strings = new ArrayList<String>(); strings.add("foo");`
  - Error Prone [Var](https://errorprone.info/bugpattern/Var) rule is enforced to prevent mutable variables. If you need mutability, you must explicitly annotate with `@Var` and justify why mutability is necessary.
- prefer non nullability. Using [jspecify](https://jspecify.dev/docs/spec/) and [Nullaway](https://github.com/uber/NullAway/wiki) we enforce non nullability by default and explicitly annotate nullable types with `@Nullable`. This helps prevent null pointer exceptions and makes it clear when a value can be null. `Optional` is preferred when mapping or filtering would be clearer than procedural logic.

## Nullability with JSpecify

This codebase uses [JSpecify](https://jspecify.dev/) for nullness annotations and [NullAway](https://github.com/uber/NullAway/wiki) for compile-time null safety.

### The Rule: Non-Null by Default

All types are **non-null by default**. You must explicitly mark nullable types:

```java
// GOOD - parameter is non-null (default), return is nullable
public @Nullable User findById(String id) {
  // ... may return null if not found
}

// GOOD - both parameters nullable
public void merge(@Nullable User first, @Nullable User second) {
  // ...
}
```

### When to Use @Nullable

Mark a type `@Nullable` when:

- A method may return `null` (e.g., finders, lookups)
- A parameter may accept `null` (avoid if possible)
- A field may be uninitialized or set to `null`

### Prefer Optional for Chain Operations

Use `Optional` when you need to map/filter over potentially absent values:

```java
// GOOD - Optional for chaining
return findById(id)
    .map(User::getEmail)
    .filter(Email::isValid)
    .orElse(defaultEmail);

// BAD - null check with intermediate variables
var user = findById(id);
if (user == null) return defaultEmail;
var email = user.getEmail();
if (email == null || !email.isValid()) return defaultEmail;
return email;
```

### NullAway Suppressions

If NullAway cannot prove non-nullness, first exhaust these options in order:

### 1. Fix the Code

Restructure to make non-nullness provable. This is almost always possible:

```java
// BEFORE - NullAway complains
private String id;

public void setId(String id) {
    this.id = Objects.requireNonNull(id);
}

// AFTER - field is final, initialized in constructor
private final String id;

public Foo(String id) {
    this.id = Objects.requireNonNull(id);
}
```

### 2. Use @Initializer

For framework initialization methods (e.g., `@PostConstruct`, `@BeforeEach`), use `@Initializer`:

```java
@Initializer
@PostConstruct
public void init() {
  // NullAway understands this method initializes fields
  this.service = createService();
}
```

### 3. Suppression (Last Resort)

Only suppress if NullAway is fundamentally wrong about the analysis. This is rare:

```java
// Only when NullAway's analysis is incorrect
@SuppressWarnings("NullAway") // Validated by external framework contract
private final String generatedId;
```

**Default to fixing the code, not suppressing.**

## Style

### var keyword

prefer `var` keyword to explicit local variable type declaration. using var reduces quantity of code but more importantly coupling as sometimes it means classes no longer have to be imported and thus class name changes do not impact client code in some cases.

note: Error Prone's `@Var` annotation (unrelated to `var` keyword) marks intentionally mutable local variables. Use it instead of `@SuppressWarnings("Var")` when you need mutability.

**Warning: Using `@Var` is an extreme code smell.** Mutable local variables should be avoided. Most algorithms can be expressed without mutation using streams, recursion, or restructuring:

```java
// BAD - requires @Var because of mutation
@Var
var counter = 0;
for (var item : items) {
    if (item.isValid()) {
        counter++;
    }
}

// GOOD - no mutation needed
var count = items.stream()
    .filter(Item::isValid)
    .count();
```

Examples:

#### GOOD

- `var x = 1;`
- `var foo = "foo"`
- `var list = new ArrayList<Foo>();`

#### BAD

- `int x = 1;`
- `String foo = "foo";`
- `List<Foo> list = new ArrayList<>();`

#### COUNTER EXAMPLE GOOD

- `Supplier<Foo> fooSupplier = () -> new Foo();` // not using var is better than casting

#### COUNTER EXAMPLE BAD

- `var fooSupplier = (Supplier<Foo>) () -> new Foo();` // casting is bad and should be avoided.

#### COUNTER EXAMPLE BETTER

Relying on a static method or variable can often be better than either even though it's more verbose.

```java
static Supplier<Foo> fooSupplier() {
  return () -> new Foo();
}
```

### Imports

- use `import` statements unless it would result in conflicts.
- use `static import` for test helpers that maintain clarity such as `assertThat` for AssertJ and `given` or `mock` from Mockito. Avoid static imports if it makes code comprehension.

### Visibility

Avoid `private` except with fields. prefer the default "package protected" unless must be `public` or is useful for subclasses.

- this allows methods to be exposed for testing but not outside the package. This aligns with the Vertical Slice architecture, Test Driven Principles, conventions where tests live in the same package and can access package protected methods, as well as original Java Language design that made this the default visibility. `private` is only necessary to prevent access from other classes in the same package, which is uncommon and should be avoided.

### Internal Packages

Avoid `internal` packages. Package-private visibility should be preferred to hide implementation details. Only use `internal` packages when something must be `public` (e.g., for framework integration or cross-module access) but should not be part of the stable public API.

- prefer package-private classes and methods over placing them in `internal` packages
- use `internal` only as a last resort when `public` is unavoidable
- if you must use `internal`, clearly document what is internal and why

### Builders

Prefer builder pattern over complex constructors with immutables library `@Builder` and a static factory. Also use `@Data` for generating type-safe field constants for testing.

Required dependencies:

- `org.immutables:value-annotations` (compile-only)
- `org.immutables:datatype` (compile-only, for `@Data`)

```java
import org.immutables.datatype.Data;
import org.immutables.value.Value;

@Value.Builder
@Data
record Bar(String foo) {
  public static BarBuilder builder() {
    return new BarBuilder();
  }
}
```

The `@Data` annotation generates `Bar_.FOO_` constants for type-safe field references in tests (see AssertJ section below).

## Idiomatic AssertJ

AssertJ provides a rich fluent API. Use it instead of manual extraction or property access.

### Common Patterns

```java
// BAD - manual extraction
assertThat(list.getUsers().get(0).getName()).isEqualTo("Alice");

// GOOD - use first() and extracting()
assertThat(list.getUsers())
    .first()
    .extracting(User::getName)
    .isEqualTo("Alice");

// EVEN BETTER - flat extracting
assertThat(list.getUsers())
    .extracting(User::getName)
    .containsExactly("Alice", "Bob", "Carol");
```

### Use the Fluent API

```java
// BAD - chaining assertions
assertThat(user.getName()).isEqualTo("Alice");
assertThat(user.getAge()).isEqualTo(30);
assertThat(user.isActive()).isTrue();

// GOOD - satisfies with multiple checks (soft assertions)
assertThat(user)
    .satisfies(u -> {
        assertThat(u.getName()).isEqualTo("Alice");
        assertThat(u.getAge()).isEqualTo(30);
        assertThat(u.isActive()).isTrue();
    });

// EVEN BETTER - returns for single property checks
assertThat(user)
    .returns("Alice", User::getName)
    .returns(30, User::getAge);

// BEST - hasFieldOrPropertyWithValue with Immutables datatype for fields
// The @Data annotation generates a class named Datatypes_<Type> containing
// type-safe field name constants. For a User record, this generates Datatypes_User
// with constants like User_.NAME_ and User_.AGE_.
//
// Import the constants for clean usage:
//   import static com.example.Datatypes_User.User_;
//
// This method is preferred because when assertions fail, the error message
// includes the field name (e.g., "expected field/property 'name' value"),
// unlike approaches that result in unhelpful messages like "expected:<true> but was:<false>"
assertThat(user)
    .hasFieldOrPropertyWithValue(User_.NAME_, "Alice")
    .hasFieldOrPropertyWithValue(User_.AGE_, 30);
```

### Collection Assertions

```java
// BAD - size check then element check
assertThat(users).hasSize(3);
assertThat(users.get(0)).isEqualTo(alice);

// GOOD - containsExactly with varargs
assertThat(users).containsExactly(alice, bob, carol);

// For partial matching
assertThat(users)
    .extracting(User::getName)
    .contains("Alice", "Bob")
    .doesNotContain("Dave");
```

### Exception Assertions

```java
// BAD - try-catch
try {
    service.doSomething();
    fail("Expected exception");
} catch (IllegalArgumentException e) {
    assertThat(e.getMessage()).contains("invalid");
}

// GOOD - assertThatThrownBy
assertThatThrownBy(() -> service.doSomething())
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("invalid");
```

### Check Available Methods

Use the **javadocs MCP server** to look up AssertJ's fluent API methods for the type you're asserting:

- `AbstractObjectAssert` for object assertions
- `AbstractListAssert` for list assertions
- `AbstractThrowableAssert` for exception assertions

Before writing manual extraction code, check if AssertJ already has a method for it.

## Use What's Already On The Classpath

**Before writing any code, check if the functionality already exists in your dependencies.**

If Guava's `Strings`, Apache Commons, or the JDK already has it → **use it**. Period.

```java
// BAD - even though it's "just" one line
if (str == null) {
    throw new NullPointerException("str must not be null");
}

// GOOD - it's already there, use it
Objects.requireNonNull(str);
```

### The Rule Is Simple

| Is it in your deps? | Action                          |
| ------------------- | ------------------------------- |
| Yes                 | Use it, no matter how "trivial" |
| No                  | Then consider writing it        |

### Check First

Use the **javadocs MCP server** to search available classes before implementing:

- `Strings` (Guava or Commons Lang3)
- `Preconditions` / `Validate` (Guava / Commons Lang3)
- `ObjectUtils` / `Objects` (Commons Lang3 / JDK)
- `CollectionUtils` / `Iterables` / `Streams`

### Benefits

- **Zero additional dependencies** - it's already there!
- **Less code to maintain** - delete your custom version
- **Standard behavior** - what other developers expect
- **Battle-tested** - edge cases already handled

### Only Write Your Own When...

- The functionality truly doesn't exist in any on-classpath library
- You need behavior that's fundamentally different from available options

---

SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-SA-4.0
