"""Python client for the Karnak REST API (karnak-endpoints fork).

Transport client (``client``) plus idempotent desired-state apply
(``apply``). See API.md at the repository root for endpoint details.
"""

from .apply import (
    KarnakConfigError,
    apply_config,
    load_apply_config,
    profile_meta_from_yaml,
    write_json_atomic,
)
from .client import (
    ApiError,
    AuthenticationError,
    KarnakApiError,
    KarnakClient,
    KarnakClientConfig,
    KarnakError,
)

__version__ = "0.1.0"

__all__ = [
    "ApiError",
    "AuthenticationError",
    "KarnakApiError",
    "KarnakClient",
    "KarnakClientConfig",
    "KarnakConfigError",
    "KarnakError",
    "apply_config",
    "load_apply_config",
    "profile_meta_from_yaml",
    "write_json_atomic",
    "__version__",
]
