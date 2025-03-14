# Crash reproduction in `kotlinx.fuzz`

Crash reproduction is the process of generating tests that correspond to a user's test run on a specific data that lead to a crash.

## Supported approaches

* List based without inlining - it creates an object of a `List<Any>` type and passes it as a parameter for an object that implements `KFuzzer` interface by returning elements from list one by one. Then this object is passed as an argument to a user's test.
* List based with inlining - it creates an object of a `List<Any>` type and passes it as a parameter for an object that implements `KFuzzer` interface by returning elements from list one by one. Then this object is used as a variable with the name of an argument and original user's method code is inlined. What is more, all private classes and methods from top-level are copied in order to have correctly resolvable reproducer. Note that for some cases reproducer can still be incorrect, e.g. usage of private or protected methods of test class. It can be fixed by copying unresolved methods manually to the file of the reproducer (or by making original methods/fields/classes public or internal)