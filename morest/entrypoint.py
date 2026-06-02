"""restberus-compatible entrypoint for the original Morest fuzzer.

Wires the original Morest (``build_graph.parse`` + ``fuzzer.fuzzer.APIFuzzer``) to
restberus's tool-run contract and seeds all RNGs deterministically. The algorithm
modules under this directory are the unmodified original Morest source; only this
thin entry shim is restberus-specific.

Invocation (from ``tools/morest/wrapper.sh``):
    python3 entrypoint.py <spec_path> <base_url> <time_budget_seconds>
with fallbacks to the env vars restberus injects (SPEC_PATH/TARGET_URL/
TIME_BUDGET_SECONDS) and RANDOM_SEED.
"""

import os
import sys

# Reproducibility (1/2): Python randomizes str/bytes hashing per process (PYTHONHASHSEED),
# which changes set/dict iteration order and therefore the order in which Morest consumes its
# (seeded) random draws -- so seeding alone is NOT reproducible. PYTHONHASHSEED can only be set
# before interpreter startup, so derive it from RANDOM_SEED and re-exec the interpreter once.
_run_seed = os.environ.get("RANDOM_SEED", "42")
try:
    _hash_seed = str(int(_run_seed) % (2**32))
except ValueError:
    _hash_seed = "0"
if os.environ.get("PYTHONHASHSEED") != _hash_seed:
    os.environ["PYTHONHASHSEED"] = _hash_seed
    os.execv(sys.executable, [sys.executable, *sys.argv])

import random  # noqa: E402

import numpy as np  # noqa: E402
from prance import ResolvingParser  # noqa: E402

from build_graph import parse  # noqa: E402
from fuzzer.fuzzer import APIFuzzer  # noqa: E402


def default_reclimit_handler(limit, parsed_url, recursions=()):
    """Return a stub object on a recursive ``$ref`` so prance keeps resolving.

    Mirrors the original Morest ``fuzzer.py`` handler.
    """
    return {
        "type": "object",
        "name": "Recursive Dependency",
        "properties": {},
    }


def main():
    # Reproducibility (2/2): seed every RNG BEFORE any randomness. Morest's randomness flows
    # solely through the numpy legacy global (``np.random.*``) and the stdlib ``random`` global
    # -- single-threaded, no per-instance RNG -- so these two calls cover every stochastic draw.
    # (The original tool never seeded at all.)
    seed = int(os.environ.get("RANDOM_SEED", "42"))
    random.seed(seed)
    np.random.seed(seed)

    spec_path = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("SPEC_PATH", "/app/spec.json")
    base_url = sys.argv[2] if len(sys.argv) > 2 else os.environ["TARGET_URL"]
    time_budget = (
        int(sys.argv[3]) if len(sys.argv) > 3 else int(os.environ.get("TIME_BUDGET_SECONDS", "3700"))
    )

    parser = ResolvingParser(
        spec_path,
        recursion_limit_handler=default_reclimit_handler,
        backend="openapi-spec-validator",
        strict=False,
    )
    apis, odg = parse(parser.specification)
    # NOTE: the original main() also called odg.draw() here; it only writes a graph file
    # for visualization and is intentionally dropped (avoids a CWD write / graphviz binary).
    api_fuzzer = APIFuzzer(apis, parser.specification, odg, base_url, time_budget=time_budget)
    api_fuzzer.run()


if __name__ == "__main__":
    main()
