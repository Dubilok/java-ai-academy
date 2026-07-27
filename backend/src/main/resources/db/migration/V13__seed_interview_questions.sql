-- Seed: Java interview questions across technologies and difficulties
INSERT INTO interview_questions (id, technology, category, question, short_answer, detailed_explanation, difficulty)
VALUES
    -- Java Core — BEGINNER
    (gen_random_uuid(), 'Java', 'Core', 'What is the difference between == and equals() in Java?',
     '== compares object references; equals() compares object content (unless overridden).',
     'In Java, == on objects checks whether two variables point to the exact same object in memory. The equals() method is defined in Object and by default also checks reference equality, but most classes (String, Integer, List, etc.) override it to check value equality. For primitives, == compares values directly. Always use equals() when comparing String values.',
     'BEGINNER'),

    (gen_random_uuid(), 'Java', 'Core', 'What is the difference between int and Integer in Java?',
     'int is a primitive type; Integer is its wrapper class that allows null and provides utility methods.',
     'int stores a numeric value directly in memory (stack or field). Integer is an object that wraps an int — it can be null, can be stored in collections, and provides methods like Integer.parseInt(), Integer.MAX_VALUE, etc. Java auto-boxes/unboxes between them automatically, but this has a small performance cost and can cause NullPointerException if an Integer field is null and unboxed.',
     'BEGINNER'),

    (gen_random_uuid(), 'Java', 'OOP', 'What is the difference between an abstract class and an interface?',
     'Abstract classes can have state and implemented methods; interfaces define contracts with default/static methods (Java 8+) but no instance state.',
     'An abstract class can have constructors, fields, and both abstract and concrete methods. A class can extend only one abstract class. An interface cannot have instance fields or constructors; it defines method signatures (and since Java 8, default and static method implementations). A class can implement multiple interfaces. Use an abstract class when sharing code between related classes; use an interface to define a contract for unrelated types.',
     'BEGINNER'),

    (gen_random_uuid(), 'Java', 'Core', 'What does the final keyword mean in Java?',
     'On a variable: cannot be reassigned. On a method: cannot be overridden. On a class: cannot be subclassed.',
     'When applied to a local variable or field, final means the reference (or primitive value) is set once and never changed. For object references, the object itself can still be mutated — only the reference is frozen. On a method, final prevents subclasses from overriding it. On a class, final prevents inheritance altogether (e.g., String is final). Using final fields in immutable classes is a key thread-safety technique.',
     'BEGINNER'),

    -- Java Core — INTERMEDIATE
    (gen_random_uuid(), 'Java', 'Collections', 'What is the difference between ArrayList and LinkedList?',
     'ArrayList uses a dynamic array (O(1) random access); LinkedList uses doubly-linked nodes (O(1) head/tail insert, O(n) index access).',
     'ArrayList provides O(1) get(index) because it maps index to an array offset, but O(n) insertions in the middle due to shifting. LinkedList provides O(1) addFirst/addLast and removeFirst/removeLast, making it a natural Deque, but O(n) index-based access and higher memory overhead (each node stores two pointers). For most general-purpose use ArrayList is faster because of CPU cache locality. Prefer LinkedList only when you know you will do many insertions/removals at the head.',
     'INTERMEDIATE'),

    (gen_random_uuid(), 'Java', 'Concurrency', 'What is the volatile keyword and when should you use it?',
     'volatile guarantees visibility of a variable''s latest write to all threads and prevents instruction reordering around that variable.',
     'Without volatile, the JVM may cache a field''s value in a CPU register or L1 cache, so one thread''s write may not be seen by another. volatile forces every read to go to main memory. It does NOT make compound operations atomic (e.g., i++ is still not thread-safe). Use volatile for simple flags (boolean running = true/false) or for a lazily-initialized singleton where the double-checked locking pattern requires the field to be volatile in Java 5+.',
     'INTERMEDIATE'),

    (gen_random_uuid(), 'Java', 'Core', 'Explain the Java memory model: stack vs heap.',
     'Stack stores frames (local variables, method calls) per thread; heap stores all objects shared across threads.',
     'Each thread has its own stack. When a method is called, a frame is pushed containing the method''s local variables and operand stack. Primitives and object references live on the stack; the actual objects they point to live on the heap. The heap is shared across all threads, which is why synchronization is needed for mutable shared state. The JVM''s garbage collector reclaims heap objects that are no longer reachable. Stack frames are reclaimed automatically when the method returns.',
     'INTERMEDIATE'),

    (gen_random_uuid(), 'Java', 'Functional', 'What are functional interfaces and lambda expressions in Java 8?',
     'A functional interface has exactly one abstract method; a lambda is an anonymous implementation of that interface.',
     'Java 8 introduced @FunctionalInterface (a marker annotation) and lambdas as syntactic sugar. Any interface with a single abstract method qualifies — Runnable, Comparator<T>, Predicate<T>, Function<T,R>, etc. Lambdas capture effectively-final local variables from the enclosing scope. They enable a functional style: passing behavior as data, composing pipelines with the Stream API, and writing cleaner event handlers. Method references (Class::method) are a concise alternative to a lambda that just calls one method.',
     'INTERMEDIATE'),

    -- Java Core — ADVANCED
    (gen_random_uuid(), 'Java', 'Concurrency', 'What is the happens-before relationship in the Java Memory Model?',
     'A happens-before edge guarantees that all writes before the edge are visible to reads after it, across threads.',
     'The JMM defines program order (within one thread), monitor lock (unlock happens-before the next lock on the same monitor), volatile write (happens-before subsequent volatile read), thread start/join, and transitivity. Without a happens-before chain between a write and a read, the JMM allows the compiler or CPU to reorder or cache values so the read may see a stale value. synchronized, volatile, java.util.concurrent classes, and Thread.start/join all create happens-before edges.',
     'ADVANCED'),

    (gen_random_uuid(), 'Java', 'JVM', 'What is the difference between G1GC, ZGC, and Shenandoah?',
     'G1: region-based, pause-limited, default since JDK 9. ZGC and Shenandoah: concurrent collectors with sub-millisecond pauses but higher CPU overhead.',
     'G1 divides the heap into equal regions, collects the regions with most garbage first ("Garbage First"), and targets a configurable pause goal (default 200ms). ZGC (Oracle) and Shenandoah (Red Hat) do most marking and relocation concurrently with the application, achieving < 1ms pauses regardless of heap size, at the cost of more CPU and slightly lower throughput. Choose G1 for general-purpose workloads; ZGC/Shenandoah for latency-sensitive services with large heaps (> 4 GB). Java 21 virtual threads also reduce the need for large thread pools that stress GC.',
     'ADVANCED'),

    -- Spring Boot — BEGINNER
    (gen_random_uuid(), 'Spring', 'Core', 'What is dependency injection and how does Spring implement it?',
     'DI is a pattern where dependencies are provided externally rather than created inside the class. Spring does this via its IoC container.',
     'Instead of MyService creating its own Repository, the repository is injected — usually through the constructor. Spring''s IoC container reads @Bean definitions and @Component-scanned classes, resolves their dependencies, and wires them together at startup. Constructor injection (preferred) makes dependencies explicit and allows final fields, making classes easier to test. Spring Boot auto-configures many beans through @EnableAutoConfiguration and starter POMs.',
     'BEGINNER'),

    (gen_random_uuid(), 'Spring', 'Data', 'What is the difference between @Transactional(readOnly=true) and a plain @Transactional?',
     'readOnly=true tells the persistence provider to skip dirty-checking and may apply DB-level read optimizations.',
     'Hibernate skips dirty-checking (comparing entity state to snapshots) when readOnly=true, reducing overhead for query-only methods. Some databases or JDBC drivers can also route read-only transactions to replicas. It does NOT prevent writes at the Java level — you can still call save() inside — but those writes may not be flushed. Best practice: annotate all query-only service methods with @Transactional(readOnly=true) and mutation methods with plain @Transactional.',
     'INTERMEDIATE'),

    -- Spring Boot — INTERMEDIATE
    (gen_random_uuid(), 'Spring', 'Security', 'How does Spring Security''s filter chain work?',
     'Spring Security adds a chain of servlet filters. Each filter can short-circuit the chain or pass control to the next filter, ultimately reaching the dispatcher servlet.',
     'SecurityFilterChain is a list of Filter implementations ordered by precedence. Key filters: UsernamePasswordAuthenticationFilter (form login), BasicAuthenticationFilter (HTTP Basic), BearerTokenAuthenticationFilter (JWT/OAuth2), ExceptionTranslationFilter (converts AuthenticationException/AccessDeniedException to HTTP responses), and FilterSecurityInterceptor (authorization check). Custom filters can be inserted before/after/at any position. Each request flows through all applicable filters; any filter can call SecurityContextHolder.clearContext() and return 401/403 to reject the request.',
     'INTERMEDIATE'),

    -- Spring Boot — ADVANCED
    (gen_random_uuid(), 'Spring', 'Performance', 'What are virtual threads in Java 21 and how do they change Spring MVC?',
     'Virtual threads are lightweight JVM-managed threads that block on I/O without tying up an OS thread, eliminating the need for reactive programming for I/O-heavy workloads.',
     'Traditional OS threads are expensive (~1MB stack). Virtual threads are scheduled by the JVM, not the OS, and are very cheap (thousands can exist). When a virtual thread blocks on I/O, the JVM mounts it on another carrier thread. Spring Boot 3.2+ enables virtual threads for Tomcat with spring.threads.virtual.enabled=true, turning every HTTP request handler into a virtual thread. This removes the need for WebFlux (reactive) in I/O-heavy apps while retaining the simple imperative programming model of Spring MVC.',
     'ADVANCED'),

    -- PostgreSQL — INTERMEDIATE
    (gen_random_uuid(), 'PostgreSQL', 'Performance', 'What is an index and when should you add one?',
     'An index is a data structure (usually B-tree) that allows the DB to find rows without scanning the whole table. Add one on columns used in WHERE, JOIN ON, and ORDER BY clauses that run frequently.',
     'Without an index, PostgreSQL performs a sequential scan (reads every row). A B-tree index lets it jump directly to matching rows in O(log n). The trade-off: indexes speed up reads but slow down writes (INSERT/UPDATE/DELETE must update the index too) and consume disk space. Add indexes on foreign key columns (PostgreSQL does NOT create them automatically), on columns in frequent WHERE filters, and on columns used for sorting. Partial indexes (WHERE is_published = true) are useful when you query only a subset of rows.',
     'INTERMEDIATE'),

    -- Docker / Infrastructure — BEGINNER
    (gen_random_uuid(), 'Docker', 'Containers', 'What is the difference between a Docker image and a container?',
     'An image is a read-only template; a container is a running instance of an image.',
     'A Docker image is a layered filesystem snapshot produced by a Dockerfile. It is immutable. A container is a running process that uses the image as its root filesystem, with a writable layer on top. Many containers can run from the same image simultaneously. Stopping a container does not delete it (and its writable layer) unless you pass --rm. Images are stored in a registry (Docker Hub, GHCR); containers run on a Docker host.',
     'BEGINNER');
