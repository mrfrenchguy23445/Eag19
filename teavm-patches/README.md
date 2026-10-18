# TeaVM patches

Five upstream TeaVM classes are patched: two produce a wasm-GC module that V8
refuses to compile, one is a runtime data-loss bug in the classlib, and two are
classlib members the wasm-GC link could not resolve at all. The
patched `.class` files are shipped inside
`gradle/local-libs/org/teavm/**` (a first-class `maven {}` repository declared at
the top of `allprojects.repositories` in `build.gradle.kts`, so it wins the
artifact lookup over the remote); `rebuild.sh` reproduces those jars from the
sources here plus the pristine jars already in the Gradle cache.

`docs/wasm-errors-todo.md` has the evidence for all five. Summary:

## `org.teavm.classlib.impl.record.ObjectMethodsSubstitutor` -> teavm-classlib

javac lowers a record's `equals()` to `invokedynamic ... ObjectMethods.bootstrap`,
and TeaVM's substitutor answers the `LONG` case with `a.isEqualTo(b)`.
`ValueEmitter.isEqualTo` builds a `BinaryBranchingInstruction(EQUAL)` carrying
no operand width while `StatementGenerator.visit(BinaryBranchingInstruction)`
hardcodes `OperationType.INT`, so the backend emits `i32.eq`/`i32.ne` on i64
operands -- 8 V8 type errors, and every record with a `long` component (22 of
them) would fail.

The `LONG` case now falls through to the existing boxed `Objects.equals(a, b)`
path. Correct for every long value; the only cost is boxing inside `equals()`.

## `org.teavm.backend.wasm.generate.gc.methods.WasmGCMethodGenerator` -> teavm-core

`generateRegularMethodBody` runs `PreciseTypeInference` twice: once with
`phisSkipped(false)` to feed the register allocator's categories, then with
`phisSkipped(true), backPropagation(true)` to pick each wasm local's declared
type. Where the strict pass answers an object type for a register the allocator
treated as primitive, the local is declared `(ref null java.lang.Object)` and
the statement generator stores `i32` into it.

The repair prefers the allocator's own category whenever it is a
`ValueType.Primitive` and the strict inference is not, and only then falls back
to the strict inference, then to `java.lang.Object`. `wgcTypeRepairs` counts
repairs so a build can report whether the branch fired.

**This patch does not fully fix root cause B** (see the tracker): B2's register
is genuinely mixed int-and-reference, which no declaration can express. It is
kept because it can only tighten a local, never widen one, and it cleared the
part of B that was a pure declaration mismatch.

## `org.teavm.classlib.java.io.TBufferedReader` -> teavm-classlib

`read(char[] cbuf, int off, int len)` calls `fillBuffer(0, len - charsRead)` on
every iteration, including when `charsRead == len` (loop about to exit) if the
wrapped reader reports `ready()`. `fillBuffer` writes at the `offset` it is given
-- here `0` -- and then does `count = offset; index = 0`. When the method has
already copied `len` characters out of this reader's own `buffer` but
`index < count` (i.e. the internal buffer still holds unconsumed characters), the
`fillBuffer(0, 0)` call overwrites `buffer[index, count)` with freshly read data
and resets `index` to 0, so those `count - index` characters are silently
dropped.

It only bites on a **short read**: when `len` is *less than* the reader's own
buffer size (1024 by default) and the copy leaves a remainder. `len == 1024`
copies the whole buffer (`index` reaches `count`) and is safe, which is why a
diagnostic that happened to read exactly 1024 chars at a time saw a byte-exact
stream while Gson -- whose `JsonReader.fillBuffer` reads
`buffer.length - limit` = 1022 after compacting -- lost 2 characters on every
refill. The observed corruption, `NumberFormatException: \u00\u`, is exactly a
`\u00` escape whose last two hex digits fell off the end of a fill.

The `read(char[], int, int)` loop now leaves any unconsumed tail in place instead
of calling `fillBuffer` on it. Correctness of `read` is unchanged; the only
behaviour dropped is the pointless (and destructive) refill of a buffer that was
just shown to still contain data.

## `org.teavm.classlib.java.lang.TString` -> teavm-classlib

The wasm-GC link failed with `Method java.lang.String.<init>([BIII)V was not
found`, reported first from netty's `ByteBufUtil.decodeString` (line 1274).
That is the long-deprecated JDK constructor `String(byte[] bytes, int hibyte,
int offset, int count)` -- TeaVM's classlib never implemented it, because it
only ever existed for pre-Unicode `DataInput`/`DataOutput` protocols, and netty
still uses it to decode a byte slice as ISO-8859-1 (`hibyte == 0`).

The patch adds the constructor with the JDK's exact signature and semantics:
each byte becomes `(char) ((hibyte << 8) | (b & 0xFF))`, i.e. `hibyte == 0`
widens each byte to its unsigned char value. It is `@Deprecated`, like the real
JDK member -- patches mirror the platform API, they do not extend it -- and it
delegates to a `hibyteToChars` helper so the bounds check is
`Objects.checkFromIndexSize`, matching the JDK's
`StringIndexOutOfBoundsException` behaviour.

## `org.teavm.classlib.java.util.TSpliterators` -> teavm-classlib

The link failed with `Class java.util.Spliterators$AbstractSpliterator was not
found`, first reported from `SectionPos$1` (line 239). Four game/Guava sources
extend it -- `Streams` (5 anonymous subclasses), `ImmutableSortedSet`,
`SectionPos`, `ChunkPos` -- and patching those call sites would have meant
rewriting working Guava code, so the missing nested class goes into the classlib
instead.

`AbstractSpliterator<T> implements TSpliterator<T>` is restored with the JDK's
signatures: the `(long sizeEstimate, int characteristics)` protected
constructor (including the JDK's `SIZED -> SUBSIZED` promotion, since batching
callers filter on `SUBSIZED`), abstract `tryAdvance`, `estimateSize`,
`characteristics`. `trySplit()` returns `null` instead of the JDK's batched
splitting: every call site drives the spliterator sequentially on the one
wasm-GC thread, so a split could only hand work to a thread that does not
exist.

## Rebuilding

```sh
export JAVA_HOME="$HOME/.local/jdk17"   # no sudo; gradle.properties must not carry org.gradle.java.home
./teavm-patches/rebuild.sh
```

Jar lookup is by content sha1, so the next Gradle resolve picks the new bytes
up in place -- no `--refresh-dependencies`, no daemon restart needed for the
repository, though `generateWasmGC` forks its own compiler JVM and reads the jar
fresh on each run.

After changing a patch: `./gradlew --stop`, then rebuild the wasm.
