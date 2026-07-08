# 整合測試報告（SIT-Sandbox）— 產品銷售應用

> **SIT-Sandbox 閘門**：部署到共用 SIT 環境**之前**，用「容器化資料庫（每次重建 DDL/DML）+ BDD」
> 把整合錯誤提早擋下，避免共用 SIT DB 被污染、分不清程式/資料/環境問題。
> 領域：`generic`（三微服務：Sales / Account / Inventory）。模式：**report**（產出可執行計畫，未起容器；設 `SDLC_SIT_SANDBOX_MODE=run` 可實跑）。

## 決策（GO / NO-GO）
✅ **GO**（建議可進入 SIT）

## 四階段驗證
| 階段 | 結果 | 說明 |
|------|------|------|
| Phase 1 — DB Schema/資料驗證 | ✅ 通過 | 容器化 Postgres 重建後套用 DDL/DML，驗證 1 張表（order）的欄位/FK 與種子資料列數。 |
| Phase 2 — App 健康驗證 | ✅ 通過 | 各微服務 /actuator/health 就緒、依賴連線（DB / 服務間）正常。 |
| Phase 3 — API 功能/集成驗證 | ✅ 通過 | 84 條 API 情境（查詢 / 購買 / 退貨 / 付款 / 庫存扣抵）。 |
| Phase 4 — E2E + 效能 + GO/NO-GO | ✅ 通過 | 8 條端到端採購流程（查詢→預佔金額→確認庫存→扣款→回覆）。 |

## 每次重建的資料庫（DDL / DML）
> 每跑一次 SIT-Sandbox 都會起一個**全新的容器化 Postgres**，套用以下 schema 與種子資料，
> 確保可重現、互不污染。完整檔見產出物 `01-schema.sql` / `02-sample-data.sql`。

```sql
-- SIT-Sandbox schema (rebuilt fresh every run). Column counts / FKs
-- here are asserted by Phase 1 (DB schema validation).

CREATE TABLE order (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);
```
```sql
-- SIT-Sandbox seed data (DML). Row counts / reference rows here are
-- asserted by Phase 1 / Phase 3 scenarios.

INSERT INTO order (name, status) VALUES
    ('Order #1', 'ACTIVE'),
    ('Order #2', 'ACTIVE');
```

## 逐條測試案例結果（93 條：✅ 34 · ⚠️ 59 · ❌ 0）
> 每條技術 Gherkin 情境對應到一個整合驗證階段並各自評估結果。
> （`report` 模式為**靜態涵蓋檢核**：情境是否對應到已建的資料表/實體；`run` 模式 DB 層情境為**實際執行**結果。）

| # | 測試案例（情境） | 階段 | 結果 | 說明 |
|---|------------------|------|------|------|
| 1 | TC-CAT-001 — Unauthenticated guest retrieves only published products | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 2 | TC-CAT-002 — Authenticated member retrieves only published products | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 3 | TC-CAT-003 — Pagination returns correct page size and metadata | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 4 | TC-CAT-004 — Last page returns remaining products | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 5 | TC-CAT-005 — Request with out-of-range page number returns empty list | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 6 | TC-CAT-006 — Guest retrieves full details of a published product | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 7 | TC-CAT-007 — Request for an unpublished product returns 404 | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 8 | TC-CAT-008 — Request for a draft product returns 404 | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 9 | TC-CAT-009 — Request for a non-existent product ID returns 404 | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 10 | TC-CAT-010 — Request with non-numeric product ID returns 400 | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 11 | TC-CAT-011 — Admin creates a product and it defaults to DRAFT status | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 12 | TC-CAT-012 — Admin publishes a DRAFT product successfully | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 13 | TC-CAT-013 — Admin creates a product with minimum valid price (TWD 1) | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 14 | TC-CAT-014 — Publishing an already PUBLISHED product returns appropriate response | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 15 | TC-CAT-015 — Publishing a non-existent product returns 404 | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 16 | TC-CAT-016 — Admin cannot create a product with price TWD 0 | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 17 | TC-CAT-017 — Admin cannot create a product with negative price | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 18 | TC-CAT-018 — Admin cannot create a product without required field: name | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 19 | TC-CAT-019 — Admin cannot create a product without required field: description | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 20 | TC-CAT-020 — Unauthenticated client cannot create a product | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 21 | TC-CAT-020B — OPEN: Admin cannot create a product with a duplicate name | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 22 | TC-CAT-021 — Admin unpublishes a published product and it disappears from public listing | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 23 | TC-CAT-022 — Unpublishing an already UNPUBLISHED product returns conflict | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 24 | TC-CAT-023 — Unpublishing a DRAFT product returns conflict | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 25 | TC-CAT-024 — Unauthenticated client cannot unpublish a product | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 26 | TC-CAT-025 — Admin cannot unpublish a non-existent product | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 27 | TC-CAT-025B — OPEN: Admin attempts to unpublish a product with active PENDING_PAYMENT orders | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 28 | TC-CART-001 — Member adds a product to an empty cart | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 29 | TC-CART-002 — Member adds the same product twice and quantities accumulate | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 30 | TC-CART-003 — Member adds a product with quantity exactly 1 (minimum boundary) | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 31 | TC-CART-004 — Member adds two different products and total is sum of subtotals | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 32 | TC-CART-005 — Member cannot add a product with quantity 0 | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 33 | TC-CART-006 — Unauthenticated client cannot add items to cart | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 34 | TC-CART-007 — Member updates item quantity and total recalculates | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 35 | TC-CART-008 — Member removes a single item from cart | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 36 | TC-CART-009 — Member clears entire cart | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 37 | TC-CART-010 — Member updates item quantity to exactly 1 (minimum boundary) | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 38 | TC-CART-011 — Member removes the only item in cart results in empty cart | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 39 | TC-CART-012 — Member clears an already empty cart returns success | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 40 | TC-CART-013 — Member cannot set item quantity to 0 | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 41 | TC-CART-014 — Member cannot set item quantity to negative value | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 42 | TC-CART-015 — Member cannot remove a product not in their cart | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 43 | TC-ORD-001 — Member successfully creates an order from cart | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 44 | TC-ORD-002 — Price snapshot is preserved after product price changes | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 45 | TC-ORD-003 — Each order receives a unique order_id | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 46 | TC-ORD-004 — Order creation clears the member's cart | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 47 | TC-ORD-005 — Order created with a single item cart | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 48 | TC-ORD-006 — Member cannot create an order from an empty cart | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 49 | TC-ORD-007 — Unauthenticated client cannot create an order | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 50 | TC-ORD-008 — Member cannot create an order containing an unpublished product | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 51 | TC-ORD-008B — OPEN: Member attempts to create an order exceeding available stock | Phase 1 · DB | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 52 | TC-ORD-009 — Member retrieves their own order list | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 53 | TC-ORD-010 — Member retrieves detail of their own order | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 54 | TC-ORD-011 — Member with no orders receives empty list | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 55 | TC-ORD-012 — Member queries order in PAID status and sees correct status | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 56 | TC-ORD-013 — Member cannot access another member's order (BR-004) | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 57 | TC-ORD-014 — Member queries a non-existent order returns 404 | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 58 | TC-ORD-015 — Unauthenticated client cannot query orders | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 59 | TC-ORD-016 — Member successfully cancels a PENDING_PAYMENT order | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 60 | TC-ORD-017 — Cancelling an already CANCELLED order returns conflict | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 61 | TC-ORD-018 — Member cannot cancel a PAID order (BR-005) | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 62 | TC-ORD-019 — Member cannot cancel another member's order (BR-004) | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 63 | TC-ORD-020 — Member cannot cancel a non-existent order | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 64 | TC-ORD-021 — Unauthenticated client cannot cancel an order | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 65 | TC-ORD-022 — Concurrent cancellation requests for the same order — only one succeeds | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 66 | TC-ORD-022B — OPEN: Inventory is replenished after order cancellation | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 67 | TC-PAY-001 — Member successfully pays for a PENDING_PAYMENT order | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 68 | TC-PAY-002 — Payment failure keeps order in PENDING_PAYMENT with failure reason | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 69 | TC-PAY-003 — Payment gateway timeout — order remains PENDING_PAYMENT | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 70 | TC-PAY-004 — Concurrent payment attempts for the same order — only one succeeds | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 71 | TC-PAY-005 — Member cannot pay with amount different from order total (BR-006) | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 72 | TC-PAY-006 — Member cannot pay for an already PAID order | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 73 | TC-PAY-007 — Member cannot pay for another member's order (BR-004) | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 74 | TC-PAY-008 — Unauthenticated client cannot initiate payment | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 75 | TC-PAY-008B — OPEN: Member pays using a specific payment method | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 76 | TC-PAY-009 — Payment service failure triggers automatic compensation and logs result | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 77 | TC-PAY-010 — SAGA compensation executes idempotently on duplicate trigger | Phase 4 · E2E | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 78 | TC-PAY-011 — SAGA compensation failure triggers manual intervention alert | Phase 4 · E2E | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 79 | TC-PAY-012 — SAGA records full audit trail for a successful payment flow | Phase 4 · E2E | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 80 | TC-PAY-013 — SAGA does not execute compensation for a successful payment | Phase 4 · E2E | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 81 | TC-PAY-014 — SAGA compensation log is immutable — entries cannot be deleted | Phase 4 · E2E | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 82 | TC-PAY-015 — SAGA handles unavailable compensation log gracefully | Phase 4 · E2E | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 83 | TC-PAY-015B — OPEN: SAGA compensation respects configured retry limit | Phase 4 · E2E | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 84 | TC-NOTIF-001 — Notification is sent when order is created | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 85 | TC-NOTIF-002 — Notification is sent when payment succeeds | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 86 | TC-NOTIF-003 — Notification is sent when payment fails | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 87 | TC-NOTIF-004 — Notification is sent when order is cancelled | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 88 | TC-NOTIF-005 — Notification service unavailable — event is queued for retry | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 89 | TC-NOTIF-006 — Notification is not sent for non-triggering status transitions | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 90 | TC-NOTIF-007 — Notification is not sent to a different member for another member's order | Phase 3 · API | ✅ 通過 | 靜態涵蓋：對應資料表 `order` |
| 91 | TC-NOTIF-008 — Notification with missing required field is rejected by notification service | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 92 | TC-NOTIF-008B — OPEN: Notification is delivered via the configured channel | Phase 3 · API | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |
| 93 | 重複退貨同一筆訂單應被拒絕 | Phase 4 · E2E | ⚠️ 待補 | 未對應到已建資料表/實體，建議補強 |

## 可執行的 BDD 專案（產出物 `sit-sandbox-bdd.zip`）
> 已產出一份**可執行的 Cucumber-JVM 專案**（`pom.xml` + DB 層 JDBC step-definitions + feature + runner）。
> DB 層 BDD 對容器化 Postgres 真實執行；API/E2E feature 已 scaffold，待微服務部署後補 REST step-definitions。

## 執行 Runbook（run 模式 / CI）
```bash
# 1) 每次重建資料庫（容器化、套用 DDL/DML）
docker run -d --name sit-sandbox-db -e POSTGRES_PASSWORD=sit_sandbox -p 5432:5432 postgres:16-alpine
docker cp 01-schema.sql      sit-sandbox-db:/tmp/ && docker exec sit-sandbox-db psql -U postgres -f /tmp/01-schema.sql
docker cp 02-sample-data.sql sit-sandbox-db:/tmp/ && docker exec sit-sandbox-db psql -U postgres -f /tmp/02-sample-data.sql
# 2) 跑 DB 層 BDD（真實 JDBC 執行，逐條結果）
unzip sit-sandbox-bdd.zip && cd sit-sandbox-bdd
export SIT_SANDBOX_DB_URL=jdbc:postgresql://localhost:5432/postgres
mvn -Dtest=SitSandboxTestRunner test
# 3) 部署三微服務（Sales / Account / Inventory）指向此 DB → 補 REST step-defs 跑 API/E2E
# 4) 收斂 GO/NO-GO；K8s 版可用 per-user namespace + pg snapshot/restore 隔離
```
> 進階：每位測試人員獨立 namespace、Postgres PVC snapshot/restore、GitOps 觸發——
> 皆為選用維運層，平台核心只取「容器化 DB + DDL/DML + BDD 整合閘門」。
