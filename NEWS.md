## 1.0.19 2025-08-19
* Reject mediated request placed for inactive patron (MODREQMED-148)

## 1.0.18 2025-07-25
* Declare lost API for mediated requests (MODREQMED-134)

## 1.0.17 2025-07-01
* Support for instance titles longer than 255 characters (MODREQMED-136)

## 1.0.16 2025-05-28
* Support for `Open - Awaiting delivery` status (MODREQMED-127)

## 1.0.15 2025-05-21
* Use Interim SP in local requests management (MODREQMED-120)

## 1.0.14 2025-05-21
* Check-out API (MODREQMED-122)

## 1.0.13 2025-05-07
* Add support for case-insensitive mediated request search (MODREQMED-17)
* Extend `send-item-in-transit` response with additional user properties (MODREQMED-116)

## 1.0.12 2025-04-28
* Fix synchronization of mediated request and circulation request for oLOAN collection items (MODREQMED-111)

## 1.0.11 2025-04-18
* Support mediated requests for items without barcodes (MODREQMED-109)
* New mediated TLRs - populate Item information accordion (MODREQMED-98)

## 1.0.10 2025-04-08
* Add item/title information to In transit slips (MODREQMED-105)

## 1.0.9 2025-03-17
* Update item info in title-level mediated request (MODREQMED-93)

## 1.0.8 2025-03-03
* Add Kafka TLS support (MODREQMED-89)

## 1.0.7 2025-02-13
* Fix secure request synchronization issues (MODREQMED-86)

## 1.0.6 2025-02-06
* Mediated request workflow - `Closed - Filled` status change (MODREQMED-76)
* Update local request upon confirmation (MODREQMED-82)

## 1.0.5 2025-01-23
* Update primary request on arrival confirmation stage (MODREQMED-70)
* Update primary request on mediated request confirmation stage (MODREQMED-67)

## 1.0.4 2025-01-16
* Upgrade to Spring Boot 3.3.7, folio-spring 8.2.2 (MODREQMED-73)
* Mediated request workflow - Open - Awaiting pickup status change (MODREQMED-74)

## 1.0.3 2025-01-09
* Cancel mediated request when confirmed circulation request is cancelled (MODREQMED-27)
* Mediated request status transition from `Not yet filled` to `In transit for approval` (MODREQMED-65)
* Add missing required interfaces to module descriptor (MODREQMED-68)

## 1.0.2 2024-12-12
* Mediated request confirmation - create ECS TLR on behalf of fake user (MODREQMED-48)
* Make mod-tlr dependency optional (MODREQMED-51)

## 1.0.1 2024-11-30
* Fetch items from all tenants for `Send item in transit` (MODREQMED-44)
* Upgrade to folio-spring-support 8.2.1 and Spring Boot 3.3.4 (MODREQMED-8)
* Add permission `user-tenants.collection.get` (MODREQMED-47)
* User, Proxy, and Item data are stored and returned if reference records are deleted (MODREQMED-40)
* Add system user section to the module descriptor (MODREQMED-52)

## 1.0.0 2024-10-31
* Extend schema, add missed permissions (MODREQMED-41)
* Fetching inventory from different tenants (MODREQMED-36)
* Add support for interface `instance-storage` 11.0 (MODREQMED-29)
* Send item in transit - build context safely (MODREQMED-34)
* Decline mediated request - new endpoint (MODREQMED-26)
* Mediated request activities - add staff slips context (MODREQMED-23)
* Make type and fulfillment preference not required (MODREQMED-28)
* Create a table for storing mediated request step history (MODREQMED-25)
* Mediated request confirmation (MODREQMED-18)
* Send item in transit by barcode - implementation (MODREQMED-22)
* Confirm item arrival (MODREQMED-21)
* Extend mediated request with remote data (MODREQMED-11)
* Create skeleton for a new endpoint - send item in transit (MODREQMED-16)
* Update version of interface `tenant` to 2.0 (MODREQMED-15)
* Create skeleton for a new endpoint - confirm item arrival (MODREQMED-14)
* Mediated request CRUD - additional fields (MODREQMED-10)
* Create mediated requests CRUD API (MODREQMED-9)
* Update packages in Dockerfile (MODREQMED-7)
* Add PD disclosure form (MODREQMED-5)
* Use GitHub workflows `api-lint` and `api-schema-lint` and `api-doc` (MODREQMED-4)
* Create module skeleton - SpringBoot (MODREQMED-2)
* Set up initial project structure (MODREQMED-1)
