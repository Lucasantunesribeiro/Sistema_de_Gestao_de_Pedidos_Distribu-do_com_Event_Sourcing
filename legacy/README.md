# Legacy Runtime

This directory keeps the historical microservices and the old `shared-events`
module outside the active delivery pipeline.

Active product:
- `frontend/`
- `unified-order-system/`
- `libs/common-*`

Historical reference only:
- `legacy/services/`
- `legacy/shared-events/`

If a legacy service needs to be revisited, treat it as a migration/reference
source instead of part of the default build, test, or deploy flow.
