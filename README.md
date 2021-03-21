# communicator

Modern applications, in particular scientific ones, besides transferring information with common application protocols, 
often need the organization of remote procedure calling (RPC).

Typical techniques of implementing RPC involve interprocess communication technologies including Component Object Model 
(COM), direct Unix socket usage, shared memory, cumbersome foreign function interfaces&mdash;e.g., a manual combination 
of JNI, intermediate C API, and `ctypes` bindings to invoke a Java function from Python&mdash;as well as designing 
custom protocols based on HTTP, WebSockets or gRPC. In some cases, even raw TCP networking is used.

To conveniently implement RPC, it is useful to create a cross-language API to register and call functions with tight 
transport-level abstraction.

As part of JetBrains Research summer internships, and architecture of the application was designed. It involves a 
so-called client that receives the result of the operation, the contractor for general data-related procedures, and 
functional servers for providing mathematical and statistical functions implementations to the contractors. This model 
allows one to lift out mathematical transformations from the main logic. Functional servers can be set up either 
remotely or locally, encapsulated with proxy middleware, and get launched independently of the contractor. In this
project, a prototype of API to build contractors and functional servers in Kotlin implemented.
