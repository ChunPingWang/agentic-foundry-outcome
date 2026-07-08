# Work Breakdown Structure — 產品銷售應用

> **WBS Version:** 1.0
> **Based on:** BRD v0.1-DRAFT
> **Currency:** TWD throughout
> **Note:** Tasks marked `[BLOCKED]` cannot start until the listed open question is resolved. All BLOCKED items are tracked in the Dependency & Constraint Register at the end of this document.

---

## Structure Overview

```
EPIC-1  Platform Foundation & Infrastructure
EPIC-2  Identity & Access Management
EPIC-3  Product Catalog
EPIC-4  Shopping Cart
EPIC-5  Order Management
EPIC-6  Payment Processing & SAGA Orchestration
EPIC-7  Notifications
EPIC-8  Observability, Alerting & Operations
EPIC-9  Non-Functional Requirements & Compliance
EPIC-10 QA, BDD Automation & Release
```

---

## EPIC-1 — Platform Foundation & Infrastructure

**Goal:** Establish the shared technical substrate on which all other epics depend.

---

### Story 1.1 — Repository & Project Scaffolding

*As a development team, we need a version-controlled monorepo with agreed conventions so that all engineers work from a consistent baseline.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T1.1.1 | Create monorepo structure (apps/, libs/, infra/) | — | Git; agree on Nx / Turborepo or plain Maven multi-module |
| T1.1.2 | Define branch strategy & PR template | T1.1.1 | GitHub / GitLab; require linear history |
| T1.1.3 | Configure linting, formatting, commit-message hooks | T1.1.1 | ESLint / Checkstyle; Husky or equivalent |
| T1.1.4 | Publish ADR-001: overall architecture decision | T1.1.1 | Markdown ADR format; stored in `/docs/adr/` |

---

### Story 1.2 — CI/CD Pipeline

*As a delivery lead, I need automated build, test, and deploy pipelines so that every merge is validated and deployable.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T1.2.1 | Configure build pipeline (compile + unit test gate) | T1.1.1 | GitHub Actions / GitLab CI; fail-fast on test failure |
| T1.2.2 | Configure integration-test stage (Docker Compose / Testcontainers) | T1.2.1 | Testcontainers; ephemeral DB per run |
| T1.2.3 | Configure BDD/acceptance-test stage | T1.2.2, T10.1.1 | Cucumber / Serenity; reports published as artifacts |
| T1.2.4 | Configure container image build & push to registry | T1.2.1 | Docker multi-stage build; image tagged with Git SHA |
| T1.2.5 | Configure deployment pipeline (dev → staging → prod) | T1.2.4 | Kubernetes / Docker Swarm; manual gate before prod |
| T1.2.6 | Configure secrets management | T1.2.5 | Vault / AWS Secrets Manager; no secrets in source |

---

### Story 1.3 — Local Development Environment

*As a developer, I need a one-command local stack so that I can develop and test without external dependencies.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T1.3.1 | Author `docker-compose.yml` for all backing services | T1.1.1 | PostgreSQL, Redis, Kafka (or ActiveMQ), mock payment stub |
| T1.3.2 | Seed script for development data | T1.3.1, T3.1.1 | Idempotent; covers all Gherkin Background fixtures |
| T1.3.3 | Document local setup in README | T1.3.1 | Must include prerequisite versions (Docker, JDK, Node) |

---

### Story 1.4 — Shared Domain Libraries

*As a developer, I need shared value objects and domain primitives so that Money(TWD), OrderId, and ProductId are defined once.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T1.4.1 | Implement `Money` value object (amount + currency=TWD) | T1.1.1 | Immutable; BigDecimal precision; no float arithmetic |
| T1.4.2 | Implement typed ID value objects (ProductId, OrderId, CartId) | T1.1.1 | UUID v4; serialisable to/from String |
| T1.4.3 | Implement `DomainEvent` base class / interface | T1.1.1 | Includes eventId, occurredAt, aggregateId |
| T1.4.4 | Publish shared library to internal artifact registry | T1.4.1–T1.4.3 | Semantic versioning; breaking changes require major bump |

---

## EPIC-2 — Identity & Access Management

**Goal:** Authenticate users, enforce role-based access (Guest / Member / Admin), and protect all secured endpoints.

> **Dependency:** Member registration flow is not specified in the BRD. Tasks T2.2.x cover the minimum required for other epics to function. Full registration UX is out of scope until confirmed.

---

### Story 2.1 — Authentication

*As any user, I need to log in so that the system knows my identity and role.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T2.1.1 | Design auth strategy (JWT / session) and document in ADR-002 | T1.1.4 | Stateless JWT preferred for microservice compatibility |
| T2.1.2 | Implement login endpoint (`POST /auth/login`) | T2.1.1 | Returns signed JWT; RS256 or HS256 with rotation policy |
| T2.1.3 | Implement token refresh endpoint | T2.1.2 | Refresh token stored server-side (Redis); short-lived access token |
| T2.1.4 | Implement logout / token revocation | T2.1.3 | Revocation list in Redis with TTL |

---

### Story 2.2 — Role-Based Access Control

*As a system, I need to enforce that Guests can only browse, Members can manage their own cart and orders, and Admins can manage the catalog.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T2.2.1 | Define roles enum: GUEST, MEMBER, ADMIN | T1.4.2 | Stored in JWT claims |
| T2.2.2 | Implement authorization middleware / filter | T2.1.2, T2.2.1 | Reject 401 if unauthenticated; 403 if insufficient role |
| T2.2.3 | Enforce member-owns-resource rule (BR-004) | T2.2.2 | Compare JWT subject with resource ownerId at service layer |
| T2.2.4 | Write security unit tests for each role boundary | T2.2.2, T2.2.3 | Cover: guest→cart (403), member→other's order (403), admin→catalog (200) |

---

### Story 2.3 — Minimal Member Provisioning

*As a developer, I need seed/test member accounts so that other epics can be developed and tested.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T2.3.1 | Create member table / schema | T1.3.1 | Fields: id, email, passwordHash, role, createdAt |
| T2.3.2 | Implement member creation (internal/admin API only) | T2.3.1 | bcrypt password hashing; not a public registration endpoint |
| T2.3.3 | Add test members to dev seed script | T2.3.2, T1.3.2 | Covers: guest (no account), member「王小明」,「李大華」, admin |

---

## EPIC-3 — Product Catalog

**Goal:** Enable Admins to manage the product lifecycle (Draft → Published → Unpublished) and enable Guests/Members to browse published products.

---

### Story 3.1 — Product Data Model & Repository

*As a developer, I need a persistent product schema so that product data survives restarts and supports all catalog operations.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T3.1.1 | Design product schema (id, name, description, priceAmount, currency, status, createdAt, updatedAt) | T1.4.1, T1.4.2 | currency column default 'TWD'; status enum: DRAFT, PUBLISHED, UNPUBLISHED |
| T3.1.2 | Write and apply DB migration (Flyway / Liquibase) | T3.1.1 | Migration scripts versioned in source; never edited after merge |
| T3.1.3 | Implement ProductRepository (CRUD + findAllByStatus) | T3.1.2 | JPA / JOOQ; no raw SQL in service layer |
| T3.1.4 | `[BLOCKED: Q14]` Add name-uniqueness constraint if confirmed | T3.1.2 | Unique index on `name`; return 409 on duplicate |

> **Q14** = BR-014 (product name uniqueness) — awaiting business confirmation.

---

### Story 3.2 — Create & Publish Product (US-003)

*As an Admin, I need to create a product in Draft state and explicitly publish it.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T3.2.1 | Implement `POST /admin/products` — create product in DRAFT | T3.1.3, T2.2.2 | Validate: name non-empty, price > TWD 0 (BR-001); return 201 + productId |
| T3.2.2 | Implement `POST /admin/products/{id}/publish` — transition DRAFT→PUBLISHED | T3.2.1 | Guard: only DRAFT may be published; emit `ProductPublished` domain event |
| T3.2.3 | Validate price > TWD 0 at domain layer | T1.4.1 | Throw `InvalidPriceException`; map to HTTP 422 |
| T3.2.4 | Write unit tests: create product, publish, invalid price | T3.2.1–T3.2.3 | Cover Gherkin scenarios: "管理員成功上架新商品", "管理員無法建立售價為零的商品" |

---

### Story 3.3 — Unpublish Product (US-004)

*As an Admin, I need to unpublish a product so that it is hidden from the public catalog.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T3.3.1 | Implement `POST /admin/products/{id}/unpublish` — transition PUBLISHED→UNPUBLISHED | T3.2.2 | Emit `ProductUnpublished` domain event |
| T3.3.2 | `[BLOCKED: Q8]` Define behaviour when active orders exist for the product | T3.3.1 | Options: allow unpublish (orders unaffected) / block / warn — awaiting Q8 |
| T3.3.3 | Write unit tests: unpublish, verify hidden from public list | T3.3.1 | Cover Gherkin: "管理員下架商品後顧客無法瀏覽" |

---

### Story 3.4 — Browse Product List (US-001)

*As a Guest or Member, I need to see only PUBLISHED products with pagination.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T3.4.1 | Implement `GET /products` — paginated list of PUBLISHED products | T3.1.3 | Query param: `page`, `size`; default size=20; max size=100 |
| T3.4.2 | Response DTO: id, name, priceAmount (TWD), shortDescription, `[BLOCKED: Q5]` stockStatus | T3.4.1 | stockStatus field omitted until Q5 resolved |
| T3.4.3 | Write integration tests: guest sees only PUBLISHED; DRAFT and UNPUBLISHED excluded | T3.4.1 | Cover Gherkin: "訪客瀏覽商品列表只看到上架商品" |

---

### Story 3.5 — View Product Detail (US-002)

*As a Guest or Member, I need to view full product details.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T3.5.1 | Implement `GET /products/{id}` — return full product detail | T3.1.3 | Return 404 if not found or UNPUBLISHED/DRAFT |
| T3.5.2 | `[BLOCKED: Q5]` Add inventory quantity display rule | T3.5.1 | Pending Q5: show exact count / low-stock label / hide |
| T3.5.3 | Write unit tests: found, not found, unpublished returns 404 | T3.5.1 | — |

---

## EPIC-4 — Shopping Cart

**Goal:** Allow Members to build a cart before checkout.

> **Dependency:** `[BLOCKED: Q13]` — Business must confirm whether a cart is required or whether single-item direct checkout is preferred. All stories in this epic are contingent on Q13 confirmation. Development can proceed in parallel as a likely-needed feature, but scope must be confirmed before sprint commitment.

---

### Story 4.1 — Cart Data Model

*As a developer, I need a cart persistence model so that cart contents survive page refreshes.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T4.1.1 | Decide cart storage strategy: DB vs Redis (ADR-003) | T1.1.4 | Redis preferred for TTL-based expiry; DB if audit trail needed |
| T4.1.2 | Design cart schema: cartId, memberId, items[{productId, quantity, priceSnapshot(TWD)}], updatedAt | T4.1.1, T1.4.1 | priceSnapshot captured at add-to-cart time for display; order creation re-validates |
| T4.1.3 | Implement CartRepository | T4.1.2 | One cart per member; upsert semantics |

---

### Story 4.2 — Add Item to Cart (US-005)

*As a Member, I need to add a product to my cart with a specified quantity.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T4.2.1 | Implement `POST /cart/items` — add or merge item | T4.1.3, T2.2.2 | Validate: product exists and is PUBLISHED; quantity ≥ 1 (BR-008) |
| T4.2.2 | Merge logic: if productId already in cart, accumulate quantity | T4.2.1 | Atomic update to prevent race condition (Redis WATCH or DB row lock) |
| T4.2.3 | Return updated cart with line subtotals and total (TWD) | T4.2.1 | Subtotal = priceSnapshot × quantity |
| T4.2.4 | Write unit tests: add new item, merge duplicate, quantity < 1 rejected | T4.2.1–T4.2.3 | Cover Gherkin: "會員成功將商品加入購物車", "重複加入相同商品時累加數量" |

---

### Story 4.3 — Modify Cart (US-006)

*As a Member, I need to update quantities, remove items, or clear my cart.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T4.3.1 | Implement `PATCH /cart/items/{productId}` — update quantity | T4.1.3, T2.2.2 | Reject quantity < 1 with 422 and message "商品數量最小值為 1" (BR-008) |
| T4.3.2 | Implement `DELETE /cart/items/{productId}` — remove single item | T4.1.3, T2.2.2 | Return 404 if item not in cart |
| T4.3.3 | Implement `DELETE /cart` — clear entire cart | T4.1.3, T2.2.2 | Idempotent; return 204 |
| T4.3.4 | Write unit tests: update qty, remove item, clear cart, qty=0 rejected | T4.3.1–T4.3.3 | Cover Gherkin: "會員修改購物車商品數量", "會員移除購物車中的商品", "購物車商品數量不可設為零" |

---

## EPIC-5 — Order Management

**Goal:** Convert a cart into an order, manage the order lifecycle, and enforce all confirmed business rules.

---

### Story 5.1 — Order Data Model

*As a developer, I need an order schema that captures a price snapshot and supports the confirmed state machine.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T5.1.1 | Design order schema: orderId (UUID), memberId, status, items[{productId, productNameSnapshot, priceSnapshot(TWD), quantity}], totalAmount(TWD), createdAt, updatedAt | T1.4.1, T1.4.2 | Price snapshot columns are immutable after insert (BR-002) |
| T5.1.2 | Write and apply DB migration | T5.1.1 | Flyway/Liquibase; include index on (memberId, status) |
| T5.1.3 | Implement OrderRepository (save, findById, findByMemberId, updateStatus) | T5.1.2 | Optimistic locking on `version` column to prevent concurrent state transitions |
| T5.1.4 | Implement Order aggregate with state-machine guard methods | T5.1.3 | Valid transitions: PENDING_PAYMENT→PAYING, PAYING→PAID, PAYING→PAYMENT_FAILED, PENDING_PAYMENT→CANCELLED |

---

### Story 5.2 — Create Order (US-007)

*As a Member, I need to convert my cart into an order with a locked price snapshot.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T5.2.1 | Implement `POST /orders` — create order from cart | T5.1.4, T4.1.3, T2.2.2 | Re-fetch current product prices at order creation (do not trust cart snapshot for billing); lock as price snapshot (BR-002) |
| T5.2.2 | Generate unique OrderId (UUID v4) and return in response (BR-003) | T5.2.1 | — |
| T5.2.3 | Set initial status to PENDING_PAYMENT | T5.2.1 | — |
| T5.2.4 | `[BLOCKED: Q5]` Implement inventory reservation at order creation | T5.2.1 | Pending Q5: optimistic vs pessimistic lock; oversell protection strategy |
| T5.2.5 | Clear cart after successful order creation | T5.2.1, T4.1.3 | Only clear on success; leave cart intact on failure |
| T5.2.6 | Emit `OrderCreated` domain event | T5.2.1, T1.4.3 | Consumed by Notification service (EPIC-7) |
| T5.2.7 | Write integration tests: successful order, price snapshot locked, cart cleared | T5.2.1–T5.2.6 | Cover Gherkin: "會員成功建立訂單" |

---

### Story 5.3 — Query Orders (US-008)

*As a Member, I need to list my orders and view a single order's details.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T5.3.1 | Implement `GET /orders` — paginated list of caller's orders | T5.1.3, T2.2.2 | Filter by JWT subject (BR-004); include orderId, status, totalAmount(TWD), createdAt |
| T5.3.2 | Implement `GET /orders/{orderId}` — full order detail | T5.1.3, T2.2.2 | Return 403 if orderId belongs to different member (BR-004); return 404 if not found |
| T5.3.3 | Write security tests: member cannot access another member's order | T5.3.2 | Cover BR-004 |

---

### Story 5.4 — Cancel Order (US-009)

*As a Member, I need to cancel a PENDING_PAYMENT order.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T5.4.1 | Implement `POST /orders/{orderId}/cancel` | T5.1.4, T2.2.2 | Guard: only PENDING_PAYMENT allowed (BR-005); return 409 if wrong state |
| T5.4.2 | Transition order status to CANCELLED | T5.4.1 | Emit `OrderCancelled` domain event |
| T5.4.3 | `[BLOCKED: Q5]` Release inventory reservation on cancel | T5.4.2 | Pending Q5: BR-010 |
| T5.4.4 | `[BLOCKED: Q8]` Implement extended cancellation rules (e.g., post-payment cancel) | T5.4.1 | Pending Q8 |
| T5.4.5 | Write unit tests: cancel PENDING_PAYMENT succeeds; cancel PAID returns 409 | T5.4.1–T5.4.2 | — |

---

## EPIC-6 — Payment Processing & SAGA Orchestration

**Goal:** Process payments reliably using the SAGA pattern (Apache Camel) to guarantee distributed consistency across Order, Inventory, and Payment services.

> **Dependency:** `[BLOCKED: Q4]` — Payment methods not confirmed. Tasks T6.1.x implement the SAGA orchestration shell and a stub payment adapter. Real payment gateway adapters are `[BLOCKED: Q4]`.

---

### Story 6.1 — SAGA Orchestrator Design

*As a system, I need a SAGA orchestrator so that the multi-step payment flow can be coordinated and compensated.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T6.1.1 | Document SAGA flow in ADR-004: steps, compensations, timeout, retry limits | T1.1.4 | Steps: ReserveInventory → ChargePayment → ConfirmOrder; compensations in reverse |
| T6.1.2 | `[BLOCKED: Q5, Q13]` Finalise SAGA step list once inventory and cart scope confirmed | T6.1.1 | — |
| T6.1.3 | Implement Apache Camel route skeleton for payment SAGA | T6.1.1, T1.3.1 | Apache Camel; use Saga EIP (`saga()` DSL); idempotent consumer on all steps |
| T6.1.4 | `[BLOCKED: BR-013]` Configure retry count and timeout per SAGA step | T6.1.3 | Pending business + tech confirmation of BR-013 |
| T6.1.5 | Implement SAGA state persistence (saga log table or Camel saga repository) | T6.1.3 | Must survive service restart; use DB-backed saga repository |

---

### Story 6.2 — Initiate Payment (US-010)

*As a Member, I need to pay for a PENDING_PAYMENT order.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T6.2.1 | Implement `POST /orders/{orderId}/pay` — trigger payment SAGA | T6.1.3, T5.1.4, T2.2.2 | Validate: order is PENDING_PAYMENT; amount matches order total (BR-006) |
| T6.2.2 | Transition order to PAYING state before calling payment gateway | T6.2.1 | Prevents duplicate payment attempts |
| T6.2.3 | `[BLOCKED: Q4]` Implement real payment gateway adapter(s) | T6.2.1 | Pending Q4; stub adapter returns configurable success/failure for testing |
| T6.2.4 | On payment success: transition order to PAID; emit `PaymentSucceeded` event | T6.2.2 | — |
| T6.2.5 | On payment failure: transition order back to PENDING_PAYMENT; emit `PaymentFailed` event | T6.2.2 | Return structured error with failure reason to caller |
| T6.2.6 | Write integration tests with stub payment adapter: success path, failure path | T6.2.1–T6.2.5 | — |

---

### Story 6.3 — Compensation Transactions (US-011)

*As a system, I need automatic compensation when any SAGA step fails so that data remains consistent.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T6.3.1 | Implement compensation route: payment failure → release inventory reservation | T6.1.3, T5.4.3 | `[BLOCKED: Q5]` — inventory release logic pending |
| T6.3.2 | Implement compensation route: inventory release failure → alert (no further auto-compensation) | T6.3.1 | Emit `CompensationFailed` event; trigger ops alert (EPIC-8) |
| T6.3.3 | Log all compensation executions to audit table (BR-013 partial) | T6.3.1, T6.3.2 | Fields: sagaId, step, action, result, timestamp |
| T6.3.4 | `[BLOCKED: BR-013]` Implement configurable retry with exponential back-off | T6.3.1 | Max retries and timeout pending BR-013 confirmation |
| T6.3.5 | Write chaos/failure tests: inject payment failure, verify inventory released and logged | T6.3.1–T6.3.3 | Use WireMock or Camel mock to simulate gateway failure |

---

### Story 6.4 — Idempotency & Duplicate Payment Protection

*As a system, I need to ensure that retried payment requests do not result in double charges.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T6.4.1 | Implement idempotency key on `POST /orders/{orderId}/pay` | T6.2.1 | Client sends `Idempotency-Key` header; server deduplicates within 24h window (Redis) |
| T6.4.2 | Implement PAYING state guard to reject concurrent payment attempts | T5.1.4 | Return 409 if order already in PAYING state |
| T6.4.3 | Write concurrency tests: simultaneous pay requests for same order | T6.4.1, T6.4.2 | — |

---

## EPIC-7 — Notifications

**Goal:** Notify Members of order state changes across confirmed channels.

> **Dependency:** `[BLOCKED: Q10]` — Notification channels (Email / SMS / Push) not confirmed. The event-publishing infrastructure can be built now; channel adapters are blocked.

---

### Story 7.1 — Notification Event Infrastructure

*As a developer, I need an event-driven notification pipeline so that any service can publish an event and the notification service delivers it.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T7.1.1 | Define notification event schema: eventType, orderId, memberId, status, amount(TWD), occurredAt | T1.4.3 | JSON schema; versioned |
| T7.1.2 | Implement event publisher in Order service (publishes on: OrderCreated, PaymentSucceeded, PaymentFailed, OrderCancelled) | T7.1.1, T5.2.6, T6.2.4, T6.2.5, T5.4.2 | Kafka topic or in-process event bus; at-least-once delivery |
| T7.1.3 | Implement Notification service consumer (subscribes to order events) | T7.1.2 | Idempotent consumer; deduplicate by eventId |
| T7.1.4 | Implement notification template engine (orderId, status, amount TWD in body) | T7.1.3 | Mustache / Thymeleaf; templates externalised for easy update |

---

### Story 7.2 — Channel Adapters `[BLOCKED: Q10]`

*As a Member, I need to receive notifications via the confirmed channel(s).*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T7.2.1 | `[BLOCKED: Q10]` Implement Email adapter | T7.1.4 | SMTP / SendGrid / SES; HTML + plain-text |
| T7.2.2 | `[BLOCKED: Q10]` Implement SMS adapter | T7.1.4 | Twilio / local provider; character limit handling |
| T7.2.3 | `[BLOCKED: Q10]` Implement Push notification adapter | T7.1.4 | FCM / APNs; device token management |
| T7.2.4 | Write integration tests for each confirmed adapter with mock provider | T7.2.x | — |

---

### Story 7.3 — Notification Preferences `[BLOCKED: Q10]`

*As a Member, I need to manage which channels I receive notifications on.*

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T7.3.1 | `[BLOCKED: Q10]` Design member notification preference schema | T2.3.1 | Only needed if multiple channels confirmed |
| T7.3.2 | `[BLOCKED: Q10]` Implement preference API | T7.3.1 | — |

---

## EPIC-8 — Observability, Alerting & Operations

**Goal:** Provide structured logging, distributed tracing, metrics, and alerting so that the team can operate the system confidently.

---

### Story 8.1 — Structured Logging

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T8.1.1 | Configure structured JSON logging across all services | T1.1.1 | Logback / Log4j2; include traceId, spanId, service name in every log line |
| T8.1.2 | Ensure SAGA compensation events are logged to audit table (BR-013 partial) | T6.3.3 | — |
| T8.1.3 | `[BLOCKED: Q12]` Apply PII masking to log fields (email, phone, member name) | T8.1.1 | Pending Q12: GDPR / 個資法 requirements |

---

### Story 8.2 — Distributed Tracing

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T8.2.1 | Instrument all services with OpenTelemetry SDK | T1.1.1 | Propagate W3C Trace Context headers |
| T8.2.2 | Deploy Jaeger / Tempo trace collector in dev and staging | T1.3.1 | Docker Compose service |
| T8.2.3 | Verify SAGA flow is traceable end-to-end as a single trace | T8.2.1, T6.1.3 | — |

---

### Story 8.3 — Metrics & Dashboards

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T8.3.1 | Expose Prometheus metrics endpoint on each service | T1.1.1 | Micrometer; include JVM, HTTP latency, error rate |
| T8.3.2 | Define business metrics: orders_created_total, payments_succeeded_total, payments_failed_total, compensations_triggered_total | T8.3.1 | Custom counters; labelled by status |
| T8.3.3 | Build Grafana dashboard for business and technical metrics | T8.3.1, T8.3.2 | Dashboard JSON committed to source |

---

### Story 8.4 — Alerting

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T8.4.1 | Configure alert: compensation failure triggers ops notification (US-011) | T8.3.1, T6.3.2 | PagerDuty / Slack webhook; P1 severity |
| T8.4.2 | `[BLOCKED: Q11]` Configure SLA-based alerts (response time, availability) | T8.3.1 | Pending Q11: SLA thresholds not defined |
| T8.4.3 | Configure alert: payment failure rate exceeds threshold | T8.3.2 | Threshold TBD with business; default 5% error rate |

---

## EPIC-9 — Non-Functional Requirements & Compliance

**Goal:** Ensure the system meets performance, security, and regulatory requirements.

---

### Story 9.1 — Performance & Scalability `[BLOCKED: Q11]`

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T9.1.1 | `[BLOCKED: Q11]` Define and document SLA targets (response time p95, availability %) | — | Pending Q11 |
| T9.1.2 | `[BLOCKED: Q11]` Implement load tests against SLA targets | T9.1.1 | k6 / Gatling; run in CI on staging |
| T9.1.3 | Add DB indexes for high-frequency queries (product list, order by member) | T3.1.2, T5.1.2 | EXPLAIN ANALYZE before and after |

---

### Story 9.2 — Security Hardening

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T9.2.1 | Enable HTTPS / TLS on all external endpoints | T1.2.5 | TLS 1.2 minimum; cert managed by cert-manager or load balancer |
| T9.2.2 | Implement rate limiting on auth and payment endpoints | T2.1.2, T6.2.1 | 429 Too Many Requests; configurable per endpoint |
| T9.2.3 | Run OWASP dependency check in CI pipeline | T1.2.1 | Fail build on CVSS ≥ 7.0 |
| T9.2.4 | Conduct threat model review for payment flow | T6.1.1 | STRIDE model; document mitigations |

---

### Story 9.3 — Data Governance & Compliance `[BLOCKED: Q12]`

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T9.3.1 | `[BLOCKED: Q12]` Define data retention policy for orders, members, payment logs | — | Pending Q12: GDPR / 個資法 |
| T9.3.2 | `[BLOCKED: Q12]` Implement data deletion / anonymisation endpoint for member data | T2.3.1 | Pending Q12 |
| T9.3.3 | `[BLOCKED: Q12]` Implement audit log for all PII access | T8.1.1 | Pending Q12 |

---

### Story 9.4 — Deferred Scope (Pending Open Questions)

| Task ID | Task | Blocked By | Notes |
|---------|------|------------|-------|
| T9.4.1 | Multi-currency support | Q3 | If confirmed, requires Money VO update, FX rate service, report changes |
| T9.4.2 | B2B pricing tiers / contract pricing | Q2 | Significant pricing engine scope |
| T9.4.3 | Promotions / discount / coupon engine | Q7 | New epic if confirmed |
| T9.4.4 | Refund / return workflow | Q6 | New epic if confirmed; affects order state machine |
| T9.4.5 | Inventory management module | Q5 | Unblocks T5.2.4, T5.4.3, T6.3.1 |
| T9.4.6 | i18n / multi-language support | Q9 | Affects product descriptions, notification templates, error messages |
| T9.4.7 | Digital goods / subscription fulfilment | Q1 | New fulfilment epic if confirmed |

---

## EPIC-10 — QA, BDD Automation & Release

**Goal:** Automate all Gherkin acceptance scenarios, establish regression gates, and manage releases.

---

### Story 10.1 — BDD Test Framework Setup

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T10.1.1 | Configure Cucumber + step-definition project structure | T1.1.1 | Java/Kotlin + Cucumber 7; feature files in `/src/test/resources/features/` |
| T10.1.2 | Implement shared step definitions for Background fixtures | T10.1.1, T1.3.2 | Reuse dev seed data; reset DB state between scenarios |
| T10.1.3 | Integrate BDD report publishing in CI (T1.2.3) | T10.1.1, T1.2.3 | HTML + JSON reports; fail pipeline on any scenario failure |

---

### Story 10.2 — Product Catalog BDD Scenarios

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T10.2.1 | Automate: "訪客瀏覽商品列表只看到上架商品" | T10.1.2, T3.4.3 | — |
| T10.2.2 | Automate: "管理員成功上架新商品" | T10.1.2, T3.2.4 | — |
| T10.2.3 | Automate: "管理員無法建立售價為零的商品" | T10.1.2, T3.2.4 | — |
| T10.2.4 | Automate: "管理員下架商品後顧客無法瀏覽" | T10.1.2, T3.3.3 | — |

---

### Story 10.3 — Shopping Cart BDD Scenarios

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T10.3.1 | Automate: "會員成功將商品加入購物車" | T10.1.2, T4.2.4 | `[BLOCKED: Q13]` |
| T10.3.2 | Automate: "重複加入相同商品時累加數量" | T10.1.2, T4.2.4 | `[BLOCKED: Q13]` |
| T10.3.3 | Automate: "會員修改購物車商品數量" | T10.1.2, T4.3.4 | `[BLOCKED: Q13]` |
| T10.3.4 | Automate: "會員移除購物車中的商品" | T10.1.2, T4.3.4 | `[BLOCKED: Q13]` |
| T10.3.5 | Automate: "購物車商品數量不可設為零" | T10.1.2, T4.3.4 | `[BLOCKED: Q13]` |

---

### Story 10.4 — Order & Payment BDD Scenarios

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T10.4.1 | Automate: "會員成功建立訂單" (price snapshot locked) | T10.1.2, T5.2.7 | — |
| T10.4.2 | Automate: payment success path end-to-end | T10.1.2, T6.2.6 | Use stub payment adapter |
| T10.4.3 | Automate: payment failure + compensation executed | T10.1.2, T6.3.5 | Inject failure via WireMock |
| T10.4.4 | Automate: member cancels PENDING_PAYMENT order | T10.1.2, T5.4.5 | — |
| T10.4.5 | Automate: member cannot view another member's order | T10.1.2, T5.3.3 | — |

---

### Story 10.5 — Release Management

| Task ID | Task | Depends On | Technical Constraints |
|---------|------|------------|----------------------|
| T10.5.1 | Define release versioning scheme (SemVer) and CHANGELOG format | T1.1.2 | Conventional Commits → auto-generated CHANGELOG |
| T10.5.2 | Define staging smoke-test checklist | T10.2.x–T10.4.x | Must pass before prod promotion |
| T10.5.3 | Define rollback procedure and document in runbook | T1.2.5 | DB migration rollback scripts required for each Flyway version |

---

## Dependency & Constraint Register

### Critical Path (Minimum Viable Flow)

```
T1.1.1 → T1.4.x → T3.1.x → T3.2.x → T3.4.x
                                          ↓
T2.1.x → T2.2.x → T4.1.x → T4.2.x → T5.2.x → T6.2.x → T7.1.x
```

### Open Question → Blocked Task Mapping

| Open Question | Blocked Tasks | Unblocking Action |
|---------------|--------------|-------------------|
| Q1 — Product type (physical/digital/subscription) | T9.4.7, fulfilment states in T5.1.4 | Business decision meeting |
| Q2 — B2B vs B2C | T9.4.2 | Business decision meeting |
| Q3 — Multi-currency | T9.4.1, T1.4.1 (Money VO extension) | Business decision meeting |
| Q4 — Payment methods | T6.2.3 | Business decision + gateway contract |
| Q5 — Inventory management | T3.5.2, T5.2.4, T5.4.3, T6.3.1, T9.4.5 | Business decision meeting |
| Q6 — Refund/return rules | T9.4.4 | Business decision meeting |
| Q7 — Promotions/discounts | T9.4.3 | Business decision meeting |
| Q8 — Cancellation conditions | T3.3.2, T5.4.4 | Business decision meeting |
| Q9 — i18n | T9.4.6 | Business decision meeting |
| Q10 — Notification channels | T7.2.1–T7.2.4, T7.3.1–T7.3.2 | Business decision meeting |
| Q11 — SLA requirements | T9.1.1, T9.1.2, T8.4.2 | Business decision meeting |
| Q12 — Data retention / privacy law | T8.1.3, T9.3.1–T9.3.3 | Legal / compliance review |
| Q13 — Cart required? | All EPIC-4 tasks, T10.3.x | Business decision meeting |
| BR-013 — SAGA retry/timeout | T6.1.4, T6.3.4 | Joint tech + business session |
| BR-014 — Product name uniqueness | T3.1.4 | Business confirmation |

### Technical Constraint Summary

| Constraint | Applies To | Rationale |
|------------|-----------|-----------|
| Apache Camel SAGA EIP | EPIC-6 | Specified in BRD glossary |
| BigDecimal for all TWD amounts | T1.4.1, all Money usage | Prevent floating-point rounding errors in financial calculations |
| Immutable price snapshot after order creation | T5.1.1, T5.2.1 | BR-002 |
| Optimistic locking on Order aggregate | T5.1.3 | Prevent concurrent state-machine violations |
| Idempotent consumers on all event handlers | T6.1.3, T7.1.3 | At-least-once delivery guarantee requires deduplication |
| Flyway/Liquibase for all schema changes | T3.1.2, T5.1.2 | Reproducible migrations; no manual DDL in production |
| JWT stateless auth | T2.1.1 | Microservice compatibility; documented in ADR-002 |
| No secrets in source control | T1.2.6 | Security baseline |
| PII fields masked in logs | T8.1.3 | Pre-emptive compliance; mandatory once Q12 resolved |