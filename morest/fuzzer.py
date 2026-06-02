import os
import sys

from prance import ResolvingParser

from build_graph import parse
from fuzzer.fuzzer import APIFuzzer
from utils.auth_util import get_token, SUT


def default_reclimit_handler(limit, parsed_url, recursions=()):
    """Raise prance.util.url.ResolutionError."""
    return {
        "type": "object",
        "name": "Recursive Dependency",
        "properties": {}
    }


def main():
    parser = ResolvingParser(f"/specifications/{os.environ['API']}-openapi.json", recursion_limit_handler=default_reclimit_handler, backend='openapi-spec-validator', strict=False)
    apis, odg = parse(parser.specification)
    odg.draw()
    api_fuzzer = APIFuzzer(apis, parser.specification, odg, f"http://localhost:{os.environ['PORT']}", time_budget=3700)
    api_fuzzer.run()
    

if __name__ == '__main__':
    main()
