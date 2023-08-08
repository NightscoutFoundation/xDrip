### Kotlin Policy

xDrip supports both Java and Kotlin source files.

New class files in Java or Kotlin are accepted but there is no intention to convert existing Java files to Kotlin.

Kotlin classes should be mindful to maintain as much java compatibility as possible where they may need to interact with existing classes.

Overhead in terms of additional libraries and any performance related issues (such as threading and component lifecycle) should be carefully monitored.