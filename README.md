#Tangerine Clinic

This is an implementation of the Line Server described at https://salsify.github.io/line-server.html

This is a Scala/Play Framework application. If you have Java installed, you should be able to run `activator` to get to the SBT prompt.

To build, run `./build.sh`

To run, run `./run.s <path-to-file-to-serve>`

##Design

Rather then try to keep all of the source file in memory, Tangerine Clinic uses Java NIO to open a [FileChannel](https://docs.oracle.com/javase/8/docs/api/java/nio/channels/FileChannel.html) that is looped through to find the indexes of all newlines. If only small files needed to be supported, this index could be kept in a Scala Seq, but since Seq is a Java Array under the hood, it's limited to 2^31 entries. The spec asks about 100 gig files, and it seems reasonable that they may have more lines then that. Instead a Map[Long, Long] is used, which trades memory for expanded functionality. For loading the actual lines, again Java NIO is used to memory map the files. Memory mapping the file is slightly more expensive then using JVM IO (mostly because of the additional page faults), but has the advantage of not needing to load the entire file into memory.

The rest of the design is just a basic Play application. IoC (via Guice) is used to make unit testing simpler.

##Performance
File size will only affect startup performance while the source file is scanned for line endings. Startup time will scale linearly with file size. The current implementation has a bug where objects are not getting correctly GCed during indexing. This causes heap usage to go up catastrophically, using around 4.9 gigs to index 3.9 gig of file. Once indexed, file size and number of requests should not affect performance. With enough users, Netty (Play's webserver) will eventually run out of heap to keep open connections on.

##Outside Help
I consulted the [Java 8 Javadocs](), the [Scala 2.12 documentation](http://www.scala-lang.org/api/2.12.0/) and the [Play Framework 2.5 documentation](https://www.playframework.com/documentation/2.5.x/Home).

##Libraries
This project uses the Play Framework as its webframework. I chose the combination of Play and Scala because I have used them before, I wanted to refamilarize myself with Scala, and because I know that Play would be simple to get up a running and mostly stay out of my way. Additional, Apache Commons is used for some string manipulation. Apache Commons is a very common utility package, and I used it because of its well known high-performance simple APIs.

##Time
About 5 hours. A large portion of the was fighting Scala's lack of try-with-resources and trying to build my own version of it in a previous Seq based index version. 
###Improvements:
* Figure out what's causing the obscene memory usage during indexing. My guess is that Scala's Map is pretty inefficient at memory usage. A possible solution would be a structure containing as many 2^31 length Seqs as we need to hold all the line indexes. This would eliminate a lot of object overhead, and still give us O(1) lookups.
* I would like to add would be to cache the index between server restarts. For a single server version, writing this data into a file on the local filesystem is probably the best way. If there were going to be multiple servers serving the same file, I would put this data into DB of some type, probably still loading all the index data into memory at startup.

##Criticism
* Using a mutable map for the index is not very functional. However, constantly copying and appending immutable maps has a serious performance penalty.
* The code in LineReader.createIndex() is quite dense, and what it does is probably not obvious to folks not famillar with functional programming.
* Sticking Java Exceptions into the left of a Scala Either is ugly. But it's better then throwing Exceptions and blowing up the stack. At least this way, Exception nastiness is constrained to the Java-interop code, and elsewhere we can stick with functional paradigms.
* Using HTTP response codes in the Error model is a leaky abstraction. Ideally there would be an intermediate representation of these error conditions that was translated to HTTP response codes in the controller or view. I didn't do this because it seems very unlikely that there will be a non-HTTP version of this, and adding unneccesary layers only serves to make the code more complex. This is an easy enough refactor if/when and additional protocol is added.
* No unit tests. There needs to be tests around the index function, which is crazy complicated, and around the little logic getLine().