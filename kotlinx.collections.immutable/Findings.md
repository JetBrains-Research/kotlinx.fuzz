## equal `PersistentOrderedMaps` are not equal

### Code:

```kotlin
val set1 = persistentSetOf(-486539264, 16777216, 0, 67108864)
val builder = set1.builder()

assertEquals(set1, builder)
assertEquals(set1, builder.build())
assertEquals(set1, builder.build().toSet())


val set2 = set1.remove(0)
builder.remove(0)

assertEquals(set2, builder.build().toSet())
assertEquals(set2, builder.build()) // fails, expected to pass
```
