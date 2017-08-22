# sash
Library for software hashing.

Generally this library has the purpose of seeing what dependencies are being used at runtime.
This hash will only include classes that have been "imported" or are being used starting from a root class, and then traversing all of that classes dependencies. In JVM languages this is done by using ASM to read the byte code of all the classes loaded and extract the class types of the bytecode.

The loaded class names are then read and hashed giving a hash that represents the code at a certain point in time. If any of the code changes, or any dependency changes, or a dependency's dependency changes it will update the hash if that class is reachable from the root class.


I plan on creating this library for the follwing languages:
* Scala (Requires Java 8)
* Java (7, 8)
* Python (3+)
* Ruby (TBD)

### TODO
* Java 7
* Python
* Ruby
