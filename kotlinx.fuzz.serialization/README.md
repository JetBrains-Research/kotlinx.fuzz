# kotlinx.fuzz.serialization

This module provides an ability to generate arbitrary data using kotlinx.fuzz
and kotlinx.serialization. The most basic example looks like this:

```kotlin
@Serializable
data class User(val id: Int)

@KFuzzTest
fun `fuzz user`(fuzzer: KFuzzer) {
    val user = fuzzer.serialization.fuzz<User>()
    // do some stuff
}
```

You can use any serializable type in that function and it will be perfectly
valid. However, some types require additional tweaks. Library can't know in
advance how long string or how big collections you might want. So, you have to
setup this by yourself:

```kotlin
@Serializable
data class User(val id: Int, val title: String)

@KFuzzTest
fun `fuzz user`(fuzzer: KFuzzer) {
    val serialization = fuzzer.serialization {
        int { fuzzer -> fuzzer.int(0..Int.MAX_SIZE) }
        string { fuzzer -> fuzzer.string(maxLength = 128) }
        collectionSize { fuzzer -> fuzzer.int(0..100) }
    }
    // List of up to 100 users,
    //  with titles up to 128 chars long,
    //  with only positive IDs
    val users = serialization.fuzz<List<User>>()
}
```

Note that you can also setup any other kinds of values that have their defaults.

It is also possible to override fuzzer config at declaration site using the
following syntax:

```kotlin
@Serializable
data class User(
    val id: Int,
    val title: String,
    @KFuzzerOptions(EmailOptions::class)
    val email: String,
) {
    object EmailOptions : KFuzzerOptions.Provider {
        private val regex = Regex("""(.*)@(.*)\.(.*)""")

        override fun KFuzzerSerialization.apply() {
            string { fuzzer -> fuzzer.string(regex) }
        }
    }
}

@KFuzzTest
fun `fuzz user`(fuzzer: KFuzzer) {
    val serialization = fuzzer.serialization {
        int { fuzzer -> fuzzer.int(0..Int.MAX_SIZE) }
        string { fuzzer -> fuzzer.string(maxLength = 128) }
        collectionSize { fuzzer -> fuzzer.int(0..100) }
    }

    // List of up to 100 users,
    //  with titles up to 128 chars long,
    //  with only positive IDs.
    // Note:
    //  email generation is CUSTOM and is not affected by
    //  `string` defined in `serialization` block.
    val users = serialization.fuzz<List<User>>()
}
```

