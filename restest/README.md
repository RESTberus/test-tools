# RESTest (restberus build)

Vendored from [isa-group/RESTest](https://github.com/isa-group/RESTest) at commit
`cab2edd` (2025-12-06, `1.6.0-SNAPSHOT`), which is the revision the
`isagroup/restgym-restest` image was built from. Only `pom.xml`, `src/main` and the
upstream `LICENSE` are vendored — tests, docs and the Allure distribution are not needed
to build the CLI.

The image builds `restest-cli.jar` from this source and drops it into the vendor image,
which supplies everything else under `/tool` (the FT/RT/RT-LLM instances, the RT-LLM
model and its Python dependencies).

## Why the fork: reproducible runs

Upstream RESTest cannot replay a run. `AbstractTestCaseGenerator` draws its seed from a
fresh `new Random()`, and the rest of the generation pulls from sources that cannot be
seeded at all: `SecureRandom` in the mutation rules and stateful generators,
`ThreadLocalRandom` in the dictionaries and mutation operators, and the static
`RandomStringUtils` helpers.

`es.us.isa.restest.util.RESTestRandom` (restberus addition) is now the single source of
randomness. With `RANDOM_SEED` set — restberus injects it per run — every component draws
from a deterministic stream derived from that seed; without it, behaviour is unchanged
from upstream. Component streams are derived from the seed and the component name, so one
component's stream does not shift when an unrelated part of the run changes.

Patched call sites:

| File | Was |
| --- | --- |
| `generators/AbstractTestCaseGenerator.java` | `rand.nextLong()` for the run seed |
| `util/IDGenerator.java` | `new Random()` |
| `inputs/random/RandomGenerator.java` | `rand.getRandomGenerator().nextLong()` |
| `inputs/random/RandomInputValueIterator.java` | `new Random()` |
| `inputs/random/RandomStringGenerator.java` | static `RandomStringUtils` |
| `inputs/fuzzing/FuzzingDictionary.java` | `ThreadLocalRandom` |
| `inputs/stateful/DataMatching.java` | `ThreadLocalRandom` |
| `inputs/stateful/ParameterGenerator.java`, `BodyGenerator.java` | `SecureRandom` |
| `inputs/perturbation/ObjectPerturbator.java` | `SecureRandom` |
| `mutation/TestCaseMutation.java` | unseeded `Collections.shuffle` |
| `mutation/SchemaMutation.java`, `rules/SingleRule.java`, `rules/PathRule.java`, `pipelines/DropSelectTypePipeline.java` | `SecureRandom` |
| `mutation/operators/RemoveRequiredParameter.java`, `operators/invalidvalue/*.java` | `ThreadLocalRandom`, `RandomStringUtils`, unseeded shuffle |
| `writers/postman/PostmanWriter.java` | static `RandomStringUtils` |

## Re-syncing with upstream

Re-vendor `pom.xml` and `src/main`, then re-apply the patches above; they are all
one-liners of the form "replace the unseedable source with `RESTestRandom`". A quick check
that nothing was missed:

```sh
grep -rn "ThreadLocalRandom\|new SecureRandom()\|Math.random()\|RandomStringUtils" \
    src/main/java | grep -v RESTestRandom.java
```

`generators/AbstractTestCaseGenerator.java` keeps its own `new Random()` instance — it is
seeded from `RESTestRandom` on the next line, and `setSeed()` still works as upstream.
