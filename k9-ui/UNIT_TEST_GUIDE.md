# KubeKui — Unit Testing Guide

## Test Command

```bash
./gradlew :shared:jvmTest                    # all tests
./gradlew :shared:jvmTest --tests "*.PortForwardStoreTest.*"  # single class
./gradlew :shared:jvmTest --tests "*.PortForwardStoreTest.test addConfig*"  # single test
```

## Test Structure

| Source Set | Directory | For |
|-----------|-----------|-----|
| `commonTest` | `shared/src/commonTest/kotlin/com/fgsoft/klusterui/` | Pure Kotlin logic (models, algorithms) |
| `jvmTest` | `shared/src/jvmTest/kotlin/com/fgsoft/klusterui/` | JVM-dependent tests (database, coroutines, process manager) |

## Test Framework

- **Assertions**: `kotlin.test` (`assertEquals`, `assertTrue`, `assertFalse`, `assertNull`, `assertNotNull`, `assertFailsWith`)
- **Lifecycle**: `@Test`, `@BeforeTest`, `@AfterTest`
- **Coroutines**: `kotlinx-coroutines-test` (`runTest`, `UnconfinedTestDispatcher`)
- **No mocking library** — use manual fake implementations of project interfaces

## Fakes

Fake implementations live in `shared/src/jvmTest/kotlin/com/fgsoft/klusterui/fakes/`:

| Fake | Implements | Purpose |
|------|-----------|---------|
| `FakeDatabase` | `Database` | In-memory lists, no SQLite |
| `FakeKubectlClient` | `KubectlClient` | Canned responses, no real kubectl |
| `FakeProcessManager` | `ProcessManager` | Tracks started/killed PIDs in memory |
| `FakeContextRepository` | `ContextRepository` | Delegates to FakeDatabase |
| `FakeDependencies` | — | Wires all fakes into `AppDependencies` |

Real `JvmDatabase` with `jdbc:sqlite::memory:` is used for database integration tests.

## Convention

- Test file matches source file: `FooStore.kt` → `FooStoreTest.kt`
- One test class per source class
- Each `@Test` method name describes the scenario: `test addConfig creates record and refreshes list`
- `@BeforeTest` sets up fresh state; `@AfterTest` cleans up
- Tests never depend on execution order

## When Adding Code

1. Create the source file
2. Create the matching test file in the correct source set
3. Add at least one test per public method
4. Run `./gradlew :shared:jvmTest` to verify

## When Removing Code

1. Delete the source code
2. Search test directories for references to the deleted class/method
3. Remove matching test files and test methods
4. Run `./gradlew :shared:jvmTest` to verify no broken references
