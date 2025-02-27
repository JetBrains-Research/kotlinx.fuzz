# Crash reproduction in `kotlinx.fuzz`

Crash reproduction is the process of generating tests that correspond to a user's test run on a specific data that lead to a crash.

## Supported approaches

* List based without inlining - it creates an object of a `List<Any>` type and passes it as a parameter for an object that implements `KFuzzer` interface by returning elements from list one by one. Than this object is passed as an argument to a user's test.