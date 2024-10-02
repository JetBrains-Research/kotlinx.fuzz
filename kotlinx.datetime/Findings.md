# `kotlinx.datetime.LocalDate.parse(s)` vs `LocalDate.Formats.ISO.parse(s)`
## Code:
```kotlin
val s = "+" + "0".repeat(7)  + "2222-07-22"
assertThrows<Exception> { kotlinx.datetime.LocalDate.parse(s) }
assertDoesNotThrow { kotlinx.datetime.LocalDate.Formats.ISO.parse(s) }
```
## Expected behavior

Both `LocalDate.parse(s)` and `ISO.parse(s)` have the same behavior (i.e. one of the asserts fails)
## Actual behavior

Behavior is different. Both assertions passes
## Also

`kotlinx.datetime.LocalDate.parse` default second argument is `LocalDate.Formats.ISO`. When it passed, this method actualy uses `java.time.LocalDate.parse` for parsing (instead of using `Format.parse` otherwise)

With `s = "0".repeat(7)  + "2222-07-22"` (basically, removed leading "+") works as expected.

In native it works as expected, because `LocalDate.parse` just calls `Format.parse`
# `kotlinx.datetime.LocalDateTime.parse(s)` vs `LocalDateTime.Formats.ISO.parse(s)`

Same as previous, but with `LocalDateTime`
## Code
```kotlin
val s = "+" + "0".repeat(7) + "2020-08-30T18:43"
assertThrows<Exception> { kotlinx.datetime.LocalDateTime.parse(s) }
assertDoesNotThrow { kotlinx.datetime.LocalDateTime.Formats.ISO.parse(s) }
```

# `kotlinx.datetime.DatePeriod` is always normalized unlike `java.time.Duration`

## Code:
```kotlin
val kotlinDatePeriod = kotlinx.datetime.DatePeriod(months = 88)
val javaDatePeriod = java.time.Period.ofMonths(88)
println(kotlinDatePeriod) // P7Y4M
println(javaDatePeriod) // P88M
println(kotlinDatePeriod.toJavaPeriod()) // P7Y4M
```

Most likely not a bug, just an implementation detail.

# `DatePeriod.parse` and `DateTimePeriod.parse`

## Code
```kotlin
val s = "P"
assertDoesNotThrow { kotlinx.datetime.DateTimePeriod.parse(s) }
assertDoesNotThrow { kotlinx.datetime.DatePeriod.parse(s) }

assertThrows<Throwable> { java.time.Period.parse(s) }
assertThrows<Throwable> { java.time.Duration.parse(s) }
assertThrows<Throwable> { kotlin.time.Duration.parseIsoString(s) }
```

## Expected behavior
First 2 asserts fails

## Actual behavior
All asserts passes

# `kotlinx.datetime.Instant.parse`

## Code:
```kotlin
val s = "-211242-10-21t20:20:44+10"
assertDoesNotThrow { kotlinx.datetime.Instant.parse(s) }
assertThrows<Throwable> { java.time.Instant.parse(s) }
```
## Expected behavior
First assert fails
## Actual behavior
All asserts passes

# `kotlinx.datetime.LocalDate.periodUntil`

## Code
```kotlin
val firstDate = kotlinx.datetime.LocalDate(year = 0, monthNumber = 5, dayOfMonth = 31)
val secondDate = kotlinx.datetime.LocalDate(year = 0, monthNumber = 4, dayOfMonth = 1)

val ktPeriod = firstDate.periodUntil(secondDate)
val jvPeriod = firstDate.toJavaLocalDate().until(secondDate.toJavaLocalDate())

println(ktPeriod) // -P1M29D
println(jvPeriod) // P-1M-30D
```
## Expected behavior

`ktPeriod` and `jvPeriod` represents the same period

## Actual behavior

`ktPeriod` is negative 1 month 29 days

`jvPeriod` is negative 1 month 30 days


# `kotlin.datetime.Duration` max value

## Difference

- `java.time.Duration` allows seconds from `Long.MIN_VALUE` to `Long.MAX_VALUE`
- `kotlin.time.Duraiton` allows only from `Long.MIN_VALUE / 2` to `Long.MAX_VALUE / 2`

----
----

Below are some findings is `kotlin.time.Duration` which is not part of `kotlinx.datetime` but still related.

# `kotlin.time.Duration.parseIsoString` with too many digits

## Code:
```kotlin
val s = "PT" + "0".repeat(17) + "M"
val javaDuration = java.time.Duration.parse(s)
val kotlinDuration = kotlin.time.Duration.parseIsoString(s)

assertTrue(javaDuration.isZero)
assertTrue(kotlinDuration.isInfinite())
```

## Expected behavior

`kotlin.time.Duration.parseIsoString` returns zero duration or throws an exception. 

## Actual behavior

`kotlin.time.Duration.parseIsoString` returns an infinite duration. All asserts passes.

# `kotlin.time.Duration.parseIsoString`

```kotlin
kotlin.time.Duration.parse("PT+-2H")
```

## Expected behavior

Failure with exception

## Actual behavior

Duration parsed as negative 2 hours




