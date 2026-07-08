# Technical Gherkin Test Cases
## Product Sales Application — Technical BDD Specification

> **Document Version:** 1.0-TECH
> **Based on Business BDD:** 0.1-DRAFT
> **Currency:** All amounts in TWD
> **Note:** Test cases marked ⚠️ OPEN correspond to unresolved business questions (Q1–Q13). These scenarios use placeholder assumptions and **must be revisited** once business confirms the relevant questions.

---

## Coverage Report

| Business US | Business Scenario | Technical Scenarios Generated | Happy | Edge | Negative | OPEN |
|---|---|---|---|---|---|---|
| US-001 | 訪客瀏覽商品列表只看到上架商品 | TC-CAT-001 ~ TC-CAT-005 | 2 | 2 | 1 | 0 |
| US-002 | 查看商品詳情 | TC-CAT-006 ~ TC-CAT-010 | 1 | 2 | 2 | 0 |
| US-003 | 管理員成功上架新商品 | TC-CAT-011 ~ TC-CAT-020 | 2 | 3 | 5 | 1 |
| US-004 | 管理員下架商品後顧客無法瀏覽 | TC-CAT-021 ~ TC-CAT-025 | 1 | 2 | 2 | 1 |
| US-005 | 會員成功將商品加入購物車 | TC-CART-001 ~ TC-CART-006 | 2 | 2 | 2 | 0 |
| US-006 | 修改購物車內容 | TC-CART-007 ~ TC-CART-015 | 3 | 3 | 3 | 0 |
| US-007 | 會員成功建立訂單 | TC-ORD-001 ~ TC-ORD-008 | 2 | 3 | 3 | 1 |
| US-008 | 查詢訂單狀態 | TC-ORD-009 ~ TC-ORD-015 | 2 | 2 | 3 | 0 |
| US-009 | 取消訂單 | TC-ORD-016 ~ TC-ORD-022 | 1 | 2 | 4 | 1 |
| US-010 | 進行付款 | TC-PAY-001 ~ TC-PAY-008 | 2 | 2 | 4 | 1 |
| US-011 | 付款失敗補償 | TC-PAY-009 ~ TC-PAY-015 | 1 | 3 | 3 | 1 |
| US-012 | 訂單狀態變更通知 | TC-NOTIF-001 ~ TC-NOTIF-008 | 4 | 2 | 2 | 1 |
| **Total** | **12 business scenarios** | **88 technical scenarios** | **23** | **28** | **34** | **7** |

> **Legend:**
> - ✅ Happy Path — expected successful flow
> - 🔀 Edge Case — boundary values, concurrent operations, state transitions
> - ❌ Negative Case — invalid input, unauthorized access, business rule violations
> - ⚠️ OPEN — depends on unresolved business question

---

## Feature: 商品目錄管理 — 瀏覽商品列表

> **Mapped to:** US-001, BR-007
> **Technical dependencies:** GET /products API, pagination, product status filter

```gherkin
Feature: 商品目錄管理 — 瀏覽商品列表
  As a system
  The product listing endpoint must only expose published products
  And support pagination with correct metadata

  Background:
    Given the database contains the following products:
      | id | name       | price_twd | status      |
      | 1  | 無線滑鼠   | 590       | PUBLISHED   |
      | 2  | 機械鍵盤   | 2490      | PUBLISHED   |
      | 3  | 舊款耳機   | 999       | UNPUBLISHED |
      | 4  | 新品音箱   | 3200      | DRAFT       |
    And the API base URL is configured

  # ✅ Happy Path

  Scenario: TC-CAT-001 — Unauthenticated guest retrieves only published products
    Given I am an unauthenticated client
    When I send GET "/api/v1/products"
    Then the response status code should be 200
    And the response body should contain exactly 2 products
    And the response body should include a product with name "無線滑鼠" and price_twd 590
    And the response body should include a product with name "機械鍵盤" and price_twd 2490
    And the response body should NOT include a product with name "舊款耳機"
    And the response body should NOT include a product with name "新品音箱"
    And each product in the response should have fields: id, name, price_twd, short_description, status
    And each product in the response should have status "PUBLISHED"

  Scenario: TC-CAT-002 — Authenticated member retrieves only published products
    Given I am authenticated as member "王小明"
    When I send GET "/api/v1/products"
    Then the response status code should be 200
    And the response body should contain exactly 2 products
    And each product in the response should have status "PUBLISHED"

  # 🔀 Edge Cases

  Scenario: TC-CAT-003 — Pagination returns correct page size and metadata
    Given the database contains 25 published products
    When I send GET "/api/v1/products?page=1&size=10"
    Then the response status code should be 200
    And the response body should contain exactly 10 products
    And the response pagination metadata should include:
      | field        | value |
      | current_page | 1     |
      | page_size    | 10    |
      | total_items  | 25    |
      | total_pages  | 3     |

  Scenario: TC-CAT-004 — Last page returns remaining products
    Given the database contains 25 published products
    When I send GET "/api/v1/products?page=3&size=10"
    Then the response status code should be 200
    And the response body should contain exactly 5 products

  # ❌ Negative Cases

  Scenario: TC-CAT-005 — Request with out-of-range page number returns empty list
    Given the database contains 5 published products
    When I send GET "/api/v1/products?page=999&size=10"
    Then the response status code should be 200
    And the response body should contain exactly 0 products
    And the response pagination metadata field "total_items" should be 5
```

---

## Feature: 商品目錄管理 — 查看商品詳情

> **Mapped to:** US-002, BR-007
> **Technical dependencies:** GET /products/{id} API

```gherkin
Feature: 商品目錄管理 — 查看商品詳情
  As a system
  The product detail endpoint must return full product information for published products
  And return appropriate errors for non-existent or unpublished products

  Background:
    Given the database contains the following products:
      | id | name     | price_twd | status      | description       |
      | 1  | 無線滑鼠 | 590       | PUBLISHED   | 高效能無線滑鼠    |
      | 3  | 舊款耳機 | 999       | UNPUBLISHED | 舊款耳機描述      |
      | 4  | 新品音箱 | 3200      | DRAFT       | 新品音箱描述      |

  # ✅ Happy Path

  Scenario: TC-CAT-006 — Guest retrieves full details of a published product
    Given I am an unauthenticated client
    When I send GET "/api/v1/products/1"
    Then the response status code should be 200
    And the response body should contain:
      | field            | value          |
      | id               | 1              |
      | name             | 無線滑鼠       |
      | price_twd        | 590            |
      | description      | 高效能無線滑鼠 |
      | status           | PUBLISHED      |

  # 🔀 Edge Cases

  Scenario: TC-CAT-007 — Request for an unpublished product returns 404
    Given I am an unauthenticated client
    When I send GET "/api/v1/products/3"
    Then the response status code should be 404
    And the response body error code should be "PRODUCT_NOT_FOUND"

  Scenario: TC-CAT-008 — Request for a draft product returns 404
    Given I am an unauthenticated client
    When I send GET "/api/v1/products/4"
    Then the response status code should be 404
    And the response body error code should be "PRODUCT_NOT_FOUND"

  # ❌ Negative Cases

  Scenario: TC-CAT-009 — Request for a non-existent product ID returns 404
    Given I am an unauthenticated client
    When I send GET "/api/v1/products/99999"
    Then the response status code should be 404
    And the response body error code should be "PRODUCT_NOT_FOUND"
    And the response body should contain a human-readable error message

  Scenario: TC-CAT-010 — Request with non-numeric product ID returns 400
    Given I am an unauthenticated client
    When I send GET "/api/v1/products/abc"
    Then the response status code should be 400
    And the response body error code should be "INVALID_PATH_PARAMETER"
```

---

## Feature: 商品目錄管理 — 上架商品

> **Mapped to:** US-003, BR-001, BR-014
> **Technical dependencies:** POST /products, PUT /products/{id}/publish, Admin auth token

```gherkin
Feature: 商品目錄管理 — 上架商品
  As a system
  Only authenticated admins may create products
  Products must be created in DRAFT status and explicitly published
  Price must be greater than TWD 0

  Background:
    Given I am authenticated as admin "admin_user"
    And the API base URL is configured

  # ✅ Happy Path

  Scenario: TC-CAT-011 — Admin creates a product and it defaults to DRAFT status
    When I send POST "/api/v1/admin/products" with body:
      """
      {
        "name": "藍牙喇叭",
        "price_twd": 1800,
        "description": "高音質藍牙喇叭"
      }
      """
    Then the response status code should be 201
    And the response body should contain:
      | field       | value    |
      | name        | 藍牙喇叭 |
      | price_twd   | 1800     |
      | status      | DRAFT    |
    And the response body should contain a non-null "id" field
    And the response body should contain a non-null "created_at" field

  Scenario: TC-CAT-012 — Admin publishes a DRAFT product successfully
    Given a product with id "5" exists in DRAFT status
    When I send PUT "/api/v1/admin/products/5/publish"
    Then the response status code should be 200
    And the response body field "status" should be "PUBLISHED"
    And sending GET "/api/v1/products/5" should return status code 200

  # 🔀 Edge Cases

  Scenario: TC-CAT-013 — Admin creates a product with minimum valid price (TWD 1)
    When I send POST "/api/v1/admin/products" with body:
      """
      {
        "name": "最低價商品",
        "price_twd": 1,
        "description": "測試最低售價"
      }
      """
    Then the response status code should be 201
    And the response body field "price_twd" should be 1
    And the response body field "status" should be "DRAFT"

  Scenario: TC-CAT-014 — Publishing an already PUBLISHED product returns appropriate response
    Given a product with id "1" exists in PUBLISHED status
    When I send PUT "/api/v1/admin/products/1/publish"
    Then the response status code should be 409
    And the response body error code should be "INVALID_STATUS_TRANSITION"

  Scenario: TC-CAT-015 — Publishing a non-existent product returns 404
    When I send PUT "/api/v1/admin/products/99999/publish"
    Then the response status code should be 404
    And the response body error code should be "PRODUCT_NOT_FOUND"

  # ❌ Negative Cases

  Scenario: TC-CAT-016 — Admin cannot create a product with price TWD 0
    When I send POST "/api/v1/admin/products" with body:
      """
      {
        "name": "零元商品",
        "price_twd": 0,
        "description": "測試零售價"
      }
      """
    Then the response status code should be 422
    And the response body error code should be "INVALID_PRODUCT_PRICE"
    And the response body error message should be "商品售價必須大於 TWD 0"

  Scenario: TC-CAT-017 — Admin cannot create a product with negative price
    When I send POST "/api/v1/admin/products" with body:
      """
      {
        "name": "負價商品",
        "price_twd": -100,
        "description": "測試負售價"
      }
      """
    Then the response status code should be 422
    And the response body error code should be "INVALID_PRODUCT_PRICE"

  Scenario: TC-CAT-018 — Admin cannot create a product without required field: name
    When I send POST "/api/v1/admin/products" with body:
      """
      {
        "price_twd": 500,
        "description": "缺少名稱"
      }
      """
    Then the response status code should be 422
    And the response body error code should be "MISSING_REQUIRED_FIELD"
    And the response body should indicate field "name" is required

  Scenario: TC-CAT-019 — Admin cannot create a product without required field: description
    When I send POST "/api/v1/admin/products" with body:
      """
      {
        "name": "無描述商品",
        "price_twd": 500
      }
      """
    Then the response status code should be 422
    And the response body error code should be "MISSING_REQUIRED_FIELD"
    And the response body should indicate field "description" is required

  Scenario: TC-CAT-020 — Unauthenticated client cannot create a product
    Given I am an unauthenticated client
    When I send POST "/api/v1/admin/products" with body:
      """
      {
        "name": "未授權商品",
        "price_twd": 500,
        "description": "測試未授權"
      }
      """
    Then the response status code should be 401
    And the response body error code should be "UNAUTHORIZED"

  # ⚠️ OPEN — depends on BR-014 (product name uniqueness — pending business confirmation)

  @open @BR-014
  Scenario: TC-CAT-020B — OPEN: Admin cannot create a product with a duplicate name
    Given a product with name "無線滑鼠" already exists
    When I send POST "/api/v1/admin/products" with body:
      """
      {
        "name": "無線滑鼠",
        "price_twd": 700,
        "description": "重複名稱測試"
      }
      """
    # Expected outcome depends on BR-014 confirmation
    # Option A: 409 CONFLICT with error code DUPLICATE_PRODUCT_NAME
    # Option B: 201 CREATED (if uniqueness is not enforced)
    Then the outcome is PENDING business confirmation of BR-014
```

---

## Feature: 商品目錄管理 — 下架商品

> **Mapped to:** US-004, BR-007
> **Technical dependencies:** PUT /products/{id}/unpublish, Admin auth token

```gherkin
Feature: 商品目錄管理 — 下架商品
  As a system
  Only authenticated admins may unpublish products
  Unpublished products must not appear in the public product listing

  Background:
    Given I am authenticated as admin "admin_user"
    And the database contains the following products:
      | id | name     | price_twd | status    |
      | 1  | 無線滑鼠 | 590       | PUBLISHED |
      | 3  | 舊款耳機 | 999       | UNPUBLISHED |

  # ✅ Happy Path

  Scenario: TC-CAT-021 — Admin unpublishes a published product and it disappears from public listing
    When I send PUT "/api/v1/admin/products/1/unpublish"
    Then the response status code should be 200
    And the response body field "status" should be "UNPUBLISHED"
    And sending GET "/api/v1/products" should not include a product with id 1
    And sending GET "/api/v1/products/1" should return status code 404

  # 🔀 Edge Cases

  Scenario: TC-CAT-022 — Unpublishing an already UNPUBLISHED product returns conflict
    When I send PUT "/api/v1/admin/products/3/unpublish"
    Then the response status code should be 409
    And the response body error code should be "INVALID_STATUS_TRANSITION"

  Scenario: TC-CAT-023 — Unpublishing a DRAFT product returns conflict
    Given a product with id "4" exists in DRAFT status
    When I send PUT "/api/v1/admin/products/4/unpublish"
    Then the response status code should be 409
    And the response body error code should be "INVALID_STATUS_TRANSITION"

  # ❌ Negative Cases

  Scenario: TC-CAT-024 — Unauthenticated client cannot unpublish a product
    Given I am an unauthenticated client
    When I send PUT "/api/v1/admin/products/1/unpublish"
    Then the response status code should be 401
    And the response body error code should be "UNAUTHORIZED"

  Scenario: TC-CAT-025 — Admin cannot unpublish a non-existent product
    When I send PUT "/api/v1/admin/products/99999/unpublish"
    Then the response status code should be 404
    And the response body error code should be "PRODUCT_NOT_FOUND"

  # ⚠️ OPEN — depends on Q8 (cancellation rules for in-progress orders)

  @open @Q8
  Scenario: TC-CAT-025B — OPEN: Admin attempts to unpublish a product with active PENDING_PAYMENT orders
    Given product with id "1" has 1 order in "PENDING_PAYMENT" status
    When I send PUT "/api/v1/admin/products/1/unpublish"
    # Expected outcome depends on Q8 confirmation
    # Option A: 200 OK — unpublish proceeds, existing orders unaffected
    # Option B: 409 CONFLICT with error code PRODUCT_HAS_ACTIVE_ORDERS
    Then the outcome is PENDING business confirmation of Q8
```

---

## Feature: 購物車管理 — 加入購物車

> **Mapped to:** US-005, BR-008
> **Technical dependencies:** POST /cart/items, Member auth token, Cart session/persistence

```gherkin
Feature: 購物車管理 — 加入購物車
  As a system
  Only authenticated members may add items to their cart
  Minimum quantity is 1
  Adding the same product again must accumulate quantity

  Background:
    Given I am authenticated as member "王小明" with member_id "M001"
    And the database contains the following published products:
      | id | name     | price_twd |
      | 1  | 無線滑鼠 | 590       |
      | 2  | 機械鍵盤 | 2490      |
    And member "M001" has an empty cart

  # ✅ Happy Path

  Scenario: TC-CART-001 — Member adds a product to an empty cart
    When I send POST "/api/v1/cart/items" with body:
      """
      {
        "product_id": 1,
        "quantity": 2
      }
      """
    Then the response status code should be 200
    And the response cart should contain:
      | product_id | name     | quantity | unit_price_twd | subtotal_twd |
      | 1          | 無線滑鼠 | 2        | 590            | 1180         |
    And the response cart field "total_twd" should be 1180

  Scenario: TC-CART-002 — Member adds the same product twice and quantities accumulate
    Given member "M001" cart contains product_id 1 with quantity 1
    When I send POST "/api/v1/cart/items" with body:
      """
      {
        "product_id": 1,
        "quantity": 1
      }
      """
    Then the response status code should be 200
    And the response cart should contain product_id 1 with quantity 2
    And the response cart field "total_twd" should be 1180

  # 🔀 Edge Cases

  Scenario: TC-CART-003 — Member adds a product with quantity exactly 1 (minimum boundary)
    When I send POST "/api/v1/cart/items" with body:
      """
      {
        "product_id": 2,
        "quantity": 1
      }
      """
    Then the response status code should be 200
    And the response cart should contain product_id 2 with quantity 1
    And the response cart field "total_twd" should be 2490

  Scenario: TC-CART-004 — Member adds two different products and total is sum of subtotals
    When I send POST "/api/v1/cart/items" with body:
      """
      { "product_id": 1, "quantity": 1 }
      """
    And I send POST "/api/v1/cart/items" with body:
      """
      { "product_id": 2, "quantity": 1 }
      """
    Then the response cart field "total_twd" should be 3080

  # ❌ Negative Cases

  Scenario: TC-CART-005 — Member cannot add a product with quantity 0
    When I send POST "/api/v1/cart/items" with body:
      """
      {
        "product_id": 1,
        "quantity": 0
      }
      """
    Then the response status code should be 422
    And the response body error code should be "INVALID_CART_QUANTITY"
    And the response body error message should be "商品數量最小值為 1"

  Scenario: TC-CART-006 — Unauthenticated client cannot add items to cart
    Given I am an unauthenticated client
    When I send POST "/api/v1/cart/items" with body:
      """
      {
        "product_id": 1,
        "quantity": 1
      }
      """
    Then the response status code should be 401
    And the response body error code should be "UNAUTHORIZED"
```

---

## Feature: 購物車管理 — 修改購物車內容

> **Mapped to:** US-006, BR-008
> **Technical dependencies:** PUT /cart/items/{product_id}, DELETE /cart/items/{product_id}, DELETE /cart

```gherkin
Feature: 購物車管理 — 修改購物車內容
  As a system
  Members may update item quantities, remove individual items, or clear the entire cart
  Quantity must remain >= 1 after update
  Cart total must recalculate after every modification

  Background:
    Given I am authenticated as member "王小明" with member_id "M001"
    And the database contains the following published products:
      | id | name     | price_twd |
      | 1  | 無線滑鼠 | 590       |
      | 2  | 機械鍵盤 | 2490      |
    And member "M001" cart contains:
      | product_id | name     | quantity |
      | 1          | 無線滑鼠 | 1        |
      | 2          | 機械鍵盤 | 2        |

  # ✅ Happy Path

  Scenario: TC-CART-007 — Member updates item quantity and total recalculates
    When I send PUT "/api/v1/cart/items/2" with body:
      """
      { "quantity": 1 }
      """
    Then the response status code should be 200
    And the response cart should contain product_id 2 with quantity 1
    And the response cart field "total_twd" should be 3080

  Scenario: TC-CART-008 — Member removes a single item from cart
    When I send DELETE "/api/v1/cart/items/1"
    Then the response status code should be 200
    And the response cart should NOT contain product_id 1
    And the response cart should contain product_id 2 with quantity 2
    And the response cart field "total_twd" should be 4980

  Scenario: TC-CART-009 — Member clears entire cart
    When I send DELETE "/api/v1/cart"
    Then the response status code should be 200
    And the response cart should contain 0 items
    And the response cart field "total_twd" should be 0

  # 🔀 Edge Cases

  Scenario: TC-CART-010 — Member updates item quantity to exactly 1 (minimum boundary)
    When I send PUT "/api/v1/cart/items/2" with body:
      """
      { "quantity": 1 }
      """
    Then the response status code should be 200
    And the response cart should contain product_id 2 with quantity 1

  Scenario: TC-CART-011 — Member removes the only item in cart results in empty cart
    Given member "M001" cart contains only product_id 1 with quantity 1
    When I send DELETE "/api/v1/cart/items/1"
    Then the response status code should be 200
    And the response cart should contain 0 items
    And the response cart field "total_twd" should be 0

  Scenario: TC-CART-012 — Member clears an already empty cart returns success
    Given member "M001" has an empty cart
    When I send DELETE "/api/v1/cart"
    Then the response status code should be 200
    And the response cart should contain 0 items

  # ❌ Negative Cases

  Scenario: TC-CART-013 — Member cannot set item quantity to 0
    When I send PUT "/api/v1/cart/items/1" with body:
      """
      { "quantity": 0 }
      """
    Then the response status code should be 422
    And the response body error code should be "INVALID_CART_QUANTITY"
    And the response body error message should be "商品數量最小值為 1"

  Scenario: TC-CART-014 — Member cannot set item quantity to negative value
    When I send PUT "/api/v1/cart/items/1" with body:
      """
      { "quantity": -1 }
      """
    Then the response status code should be 422
    And the response body error code should be "INVALID_CART_QUANTITY"

  Scenario: TC-CART-015 — Member cannot remove a product not in their cart
    When I send DELETE "/api/v1/cart/items/99"
    Then the response status code should be 404
    And the response body error code should be "CART_ITEM_NOT_FOUND"
```

---

## Feature: 訂單建立

> **Mapped to:** US-007, BR-002, BR-003
> **Technical dependencies:** POST /orders, Member auth token, Cart service, Price snapshot mechanism

```gherkin
Feature: 訂單建立
  As a system
  Authenticated members may convert their cart into an order
  The order must capture a price snapshot at creation time
  The order must be assigned a unique system-generated order ID
  Initial order status must be PENDING_PAYMENT

  Background:
    Given I am authenticated as member "李大華" with member_id "M002"
    And the database contains the following published products:
      | id | name     | price_twd |
      | 1  | 無線滑鼠 | 590       |
      | 2  | 機械鍵盤 | 2490      |
    And member "M002" cart contains:
      | product_id | name     | quantity | unit_price_twd |
      | 1          | 無線滑鼠 | 2        | 590            |
      | 2          | 機械鍵盤 | 1        | 2490           |

  # ✅ Happy Path

  Scenario: TC-ORD-001 — Member successfully creates an order from cart
    When I send POST "/api/v1/orders"
    Then the response status code should be 201
    And the response body should contain a non-null "order_id" field
    And the response body field "status" should be "PENDING_PAYMENT"
    And the response body field "member_id" should be "M002"
    And the response order items should contain:
      | product_id | name     | quantity | snapshot_price_twd | subtotal_twd |
      | 1          | 無線滑鼠 | 2        | 590                | 1180         |
      | 2          | 機械鍵盤 | 1        | 2490               | 2490         |
    And the response body field "total_twd" should be 3670
    And the response body should contain a non-null "created_at" field

  Scenario: TC-ORD-002 — Price snapshot is preserved after product price changes
    Given an order "ORD-001" was created with product_id 1 snapshot_price_twd 590
    When an admin updates product_id 1 price_twd to 700
    And I send GET "/api/v1/orders/ORD-001"
    Then the response order item for product_id 1 should have snapshot_price_twd 590
    And the response body field "total_twd" should reflect the original snapshot price

  # 🔀 Edge Cases

  Scenario: TC-ORD-003 — Each order receives a unique order_id
    When I send POST "/api/v1/orders"
    And I restore member "M002" cart to original state
    And I send POST "/api/v1/orders" again
    Then the two responses should have different "order_id" values

  Scenario: TC-ORD-004 — Order creation clears the member's cart
    When I send POST "/api/v1/orders"
    Then the response status code should be 201
    And sending GET "/api/v1/cart" should return a cart with 0 items

  Scenario: TC-ORD-005 — Order created with a single item cart
    Given member "M002" cart contains only product_id 1 with quantity 1
    When I send POST "/api/v1/orders"
    Then the response status code should be 201
    And the response order items should contain exactly 1 item
    And the response body field "total_twd" should be 590

  # ❌ Negative Cases

  Scenario: TC-ORD-006 — Member cannot create an order from an empty cart
    Given member "M002" has an empty cart
    When I send POST "/api/v1/orders"
    Then the response status code should be 422
    And the response body error code should be "CART_IS_EMPTY"

  Scenario: TC-ORD-007 — Unauthenticated client cannot create an order
    Given I am an unauthenticated client
    When I send POST "/api/v1/orders"
    Then the response status code should be 401
    And the response body error code should be "UNAUTHORIZED"

  Scenario: TC-ORD-008 — Member cannot create an order containing an unpublished product
    Given member "M002" cart contains product_id 3 which has since been UNPUBLISHED
    When I send POST "/api/v1/orders"
    Then the response status code should be 422
    And the response body error code should be "PRODUCT_NOT_AVAILABLE"
    And the response body should identify product_id 3 as unavailable

  # ⚠️ OPEN — depends on Q5 (inventory management)

  @open @Q5
  Scenario: TC-ORD-008B — OPEN: Member attempts to create an order exceeding available stock
    Given product_id 1 has available stock of 1
    And member "M002" cart contains product_id 1 with quantity 5
    When I send POST "/api/v1/orders"
    # Expected outcome depends on Q5 confirmation
    # Option A: 422 UNPROCESSABLE_ENTITY with error code INSUFFICIENT_STOCK
    # Option B: Allow oversell (no inventory management)
    Then the outcome is PENDING business confirmation of Q5
```

---

## Feature: 訂單查詢

> **Mapped to:** US-008, BR-004
> **Technical dependencies:** GET /orders, GET /orders/{order_id}, Member auth token, Authorization check

```gherkin
Feature: 訂單查詢
  As a system
  Members may only query their own orders
  Order detail must include order_id, items, total_twd, and current status
  Accessing another member's order must be forbidden

  Background:
    Given the database contains the following members:
      | member_id | name   |
      | M002      | 李大華 |
      | M003      | 陳小芳 |
    And the database contains the following orders:
      | order_id | member_id | status          | total_twd |
      | ORD-001  | M002      | PENDING_PAYMENT | 3670      |
      | ORD-002  | M002      | PAID            | 590       |
      | ORD-003  | M003      | PENDING_PAYMENT | 2490      |

  # ✅ Happy Path

  Scenario: TC-ORD-009 — Member retrieves their own order list
    Given I am authenticated as member "李大華" with member_id "M002"
    When I send GET "/api/v1/orders"
    Then the response status code should be 200
    And the response body should contain exactly 2 orders
    And the response body should include order_id "ORD-001"
    And the response body should include order_id "ORD-002"
    And the response body should NOT include order_id "ORD-003"

  Scenario: TC-ORD-010 — Member retrieves detail of their own order
    Given I am authenticated as member "李大華" with member_id "M002"
    When I send GET "/api/v1/orders/ORD-001"
    Then the response status code should be 200
    And the response body should contain:
      | field      | value           |
      | order_id   | ORD-001         |
      | status     | PENDING_PAYMENT |
      | total_twd  | 3670            |
      | member_id  | M002            |
    And the response body should contain a non-empty "items" array
    And each item in "items" should have fields: product_id, name, quantity, snapshot_price_twd, subtotal_twd

  # 🔀 Edge Cases

  Scenario: TC-ORD-011 — Member with no orders receives empty list
    Given I am authenticated as member "新會員" with member_id "M999"
    And member "M999" has no orders
    When I send GET "/api/v1/orders"
    Then the response status code should be 200
    And the response body should contain exactly 0 orders

  Scenario: TC-ORD-012 — Member queries order in PAID status and sees correct status
    Given I am authenticated as member "李大華" with member_id "M002"
    When I send GET "/api/v1/orders/ORD-002"
    Then the response status code should be 200
    And the response body field "status" should be "PAID"

  # ❌ Negative Cases

  Scenario: TC-ORD-013 — Member cannot access another member's order (BR-004)
    Given I am authenticated as member "李大華" with member_id "M002"
    When I send GET "/api/v1/orders/ORD-003"
    Then the response status code should be 403
    And the response body error code should be "FORBIDDEN"

  Scenario: TC-ORD-014 — Member queries a non-existent order returns 404
    Given I am authenticated as member "李大華" with member_id "M002"
    When I send GET "/api/v1/orders/ORD-NONEXISTENT"
    Then the response status code should be 404
    And the response body error code should be "ORDER_NOT_FOUND"

  Scenario: TC-ORD-015 — Unauthenticated client cannot query orders
    Given I am an unauthenticated client
    When I send GET "/api/v1/orders"
    Then the response status code should be 401
    And the response body error code should be "UNAUTHORIZED"
```

---

## Feature: 訂單取消

> **Mapped to:** US-009, BR-004, BR-005
> **Technical dependencies:** PUT /orders/{order_id}/cancel, Member auth token, Order status machine

```gherkin
Feature: 訂單取消
  As a system
  Members may only cancel their own orders in PENDING_PAYMENT status
  Cancellation must transition order status to CANCELLED
  Cancellation of orders in any other status must be rejected

  Background:
    Given the database contains the following orders:
      | order_id | member_id | status          |
      | ORD-001  | M002      | PENDING_PAYMENT |
      | ORD-002  | M002      | PAID            |
      | ORD-003  | M003      | PENDING_PAYMENT |

  # ✅ Happy Path

  Scenario: TC-ORD-016 — Member successfully cancels a PENDING_PAYMENT order
    Given I am authenticated as member "李大華" with member_id "M002"
    When I send PUT "/api/v1/orders/ORD-001/cancel"
    Then the response status code should be 200
    And the response body field "status" should be "CANCELLED"
    And sending GET "/api/v1/orders/ORD-001" should return field "status" as "CANCELLED"

  # 🔀 Edge Cases

  Scenario: TC-ORD-017 — Cancelling an already CANCELLED order returns conflict
    Given order "ORD-001" has been cancelled and status is "CANCELLED"
    And I am authenticated as member "李大華" with member_id "M002"
    When I send PUT "/api/v1/orders/ORD-001/cancel"
    Then the response status code should be 409
    And the response body error code should be "INVALID_ORDER_STATUS_TRANSITION"

  Scenario: TC-ORD-018 — Member cannot cancel a PAID order (BR-005)
    Given I am authenticated as member "李大華" with member_id "M002"
    When I send PUT "/api/v1/orders/ORD-002/cancel"
    Then the response status code should be 409
    And the response body error code should be "INVALID_ORDER_STATUS_TRANSITION"
    And the response body error message should indicate only PENDING_PAYMENT orders can be cancelled

  # ❌ Negative Cases

  Scenario: TC-ORD-019 — Member cannot cancel another member's order (BR-004)
    Given I am authenticated as member "李大華" with member_id "M002"
    When I send PUT "/api/v1/orders/ORD-003/cancel"
    Then the response status code should be 403
    And the response body error code should be "FORBIDDEN"

  Scenario: TC-ORD-020 — Member cannot cancel a non-existent order
    Given I am authenticated as member "李大華" with member_id "M002"
    When I send PUT "/api/v1/orders/ORD-NONEXISTENT/cancel"
    Then the response status code should be 404
    And the response body error code should be "ORDER_NOT_FOUND"

  Scenario: TC-ORD-021 — Unauthenticated client cannot cancel an order
    Given I am an unauthenticated client
    When I send PUT "/api/v1/orders/ORD-001/cancel"
    Then the response status code should be 401
    And the response body error code should be "UNAUTHORIZED"

  Scenario: TC-ORD-022 — Concurrent cancellation requests for the same order — only one succeeds
    Given I am authenticated as member "李大華" with member_id "M002"
    And order "ORD-001" is in PENDING_PAYMENT status
    When two concurrent PUT "/api/v1/orders/ORD-001/cancel" requests are sent simultaneously
    Then exactly one request should return status code 200
    And the other request should return status code 409
    And the final order status should be "CANCELLED"

  # ⚠️ OPEN — depends on Q5 (inventory replenishment on cancellation)

  @open @Q5
  Scenario: TC-ORD-022B — OPEN: Inventory is replenished after order cancellation
    Given I am authenticated as member "李大華" with member_id "M002"
    And order "ORD-001" reserved 2 units of product_id 1
    When I send PUT "/api/v1/orders/ORD-001/cancel"
    Then the response status code should be 200
    # Expected inventory behavior depends on Q5 confirmation
    # Option A: product_id 1 available stock increases by 2
    # Option B: No inventory management — no stock change
    Then the outcome is PENDING business confirmation of Q5
```

---

## Feature: 付款處理

> **Mapped to:** US-010, BR-006
> **Technical dependencies:** POST /payments, Member auth token, SAGA orchestration (Apache Camel), Payment gateway integration

```gherkin
Feature: 付款處理
  As a system
  Members may initiate payment for their own PENDING_PAYMENT orders
  Payment amount must exactly match the order total_twd (BR-006)
  Successful payment transitions order to PAID
  Failed payment keeps order in PENDING_PAYMENT and returns failure reason
  SAGA pattern must ensure distributed consistency

  Background:
    Given I am authenticated as member "李大華" with member_id "M002"
    And the database contains the following orders:
      | order_id | member_id | status          | total_twd |
      | ORD-001  | M002      | PENDING_PAYMENT | 3670      |
      | ORD-002  | M002      | PAID            | 590       |
      | ORD-003  | M003      | PENDING_PAYMENT | 2490      |

  # ✅ Happy Path

  Scenario: TC-PAY-001 — Member successfully pays for a PENDING_PAYMENT order
    Given the payment gateway is available and will accept the transaction
    When I send POST "/api/v1/payments" with body:
      """
      {
        "order_id": "ORD-001",
        "amount_twd": 3670
      }
      """
    Then the response status code should be 200
    And the response body field "status" should be "SUCCESS"
    And the response body should contain a non-null "payment_id" field
    And sending GET "/api/v1/orders/ORD-001" should return field "status" as "PAID"

  Scenario: TC-PAY-002 — Payment failure keeps order in PENDING_PAYMENT with failure reason
    Given the payment gateway will decline the transaction with reason "INSUFFICIENT_FUNDS"
    When I send POST "/api/v1/payments" with body:
      """
      {
        "order_id": "ORD-001",
        "amount_twd": 3670
      }
      """
    Then the response status code should be 200
    And the response body field "status" should be "FAILED"
    And the response body field "failure_reason" should be "INSUFFICIENT_FUNDS"
    And sending GET "/api/v1/orders/ORD-001" should return field "status" as "PENDING_PAYMENT"

  # 🔀 Edge Cases

  Scenario: TC-PAY-003 — Payment gateway timeout — order remains PENDING_PAYMENT
    Given the payment gateway will timeout after 30 seconds
    When I send POST "/api/v1/payments" with body:
      """
      {
        "order_id": "ORD-001",
        "amount_twd": 3670
      }
      """
    Then the response status code should be 504 or 200 with status "FAILED"
    And sending GET "/api/v1/orders/ORD-001" should return field "status" as "PENDING_PAYMENT"
    And the SAGA compensation log should record the timeout event

  Scenario: TC-PAY-004 — Concurrent payment attempts for the same order — only one succeeds
    Given the payment gateway is available
    When two concurrent POST "/api/v1/payments" requests are sent for "ORD-001" with amount_twd 3670
    Then exactly one request should result in status "SUCCESS"
    And the other request should return status code 409 with error code "PAYMENT_ALREADY_PROCESSING"
    And the final order status should be "PAID"

  # ❌ Negative Cases

  Scenario: TC-PAY-005 — Member cannot pay with amount different from order total (BR-006)
    When I send POST "/api/v1/payments" with body:
      """
      {
        "order_id": "ORD-001",
        "amount_twd": 3000
      }
      """
    Then the response status code should be 422
    And the response body error code should be "PAYMENT_AMOUNT_MISMATCH"
    And the response body should indicate expected amount is 3670 TWD

  Scenario: TC-PAY-006 — Member cannot pay for an already PAID order
    When I send POST "/api/v1/payments" with body:
      """
      {
        "order_id": "ORD-002",
        "amount_twd": 590
      }
      """
    Then the response status code should be 409
    And the response body error code should be "INVALID_ORDER_STATUS_FOR_PAYMENT"

  Scenario: TC-PAY-007 — Member cannot pay for another member's order (BR-004)
    When I send POST "/api/v1/payments" with body:
      """
      {
        "order_id": "ORD-003",
        "amount_twd": 2490
      }
      """
    Then the response status code should be 403
    And the response body error code should be "FORBIDDEN"

  Scenario: TC-PAY-008 — Unauthenticated client cannot initiate payment
    Given I am an unauthenticated client
    When I send POST "/api/v1/payments" with body:
      """
      {
        "order_id": "ORD-001",
        "amount_twd": 3670
      }
      """
    Then the response status code should be 401
    And the response body error code should be "UNAUTHORIZED"

  # ⚠️ OPEN — depends on Q4 (supported payment methods)

  @open @Q4
  Scenario: TC-PAY-008B — OPEN: Member pays using a specific payment method
    When I send POST "/api/v1/payments" with body:
      """
      {
        "order_id": "ORD-001",
        "amount_twd": 3670,
        "payment_method": "CREDIT_CARD"
      }
      """
    # Expected outcome depends on Q4 confirmation of supported payment methods
    Then the outcome is PENDING business confirmation of Q4
```

---

## Feature: 付款失敗補償 (SAGA)

> **Mapped to:** US-011, BR-013
> **Technical dependencies:** Apache Camel SAGA orchestration, compensation transaction log, alerting mechanism

```gherkin
Feature: 付款失敗補償 (SAGA)
  As a system
  When any step in the payment SAGA fails, compensating transactions must execute automatically
  All compensation results must be recorded in the system log
  Compensation failures must trigger a manual intervention alert

  Background:
    Given the SAGA orchestrator is running via Apache Camel
    And the compensation log store is available
    And the alerting system is configured

  # ✅ Happy Path

  Scenario: TC-PAY-009 — Payment service failure triggers automatic compensation and logs result
    Given order "ORD-001" is in PENDING_PAYMENT status
    And the payment gateway returns a failure response
    When the SAGA payment step executes for order "ORD-001"
    Then the SAGA should execute the compensation transaction
    And the compensation log should contain an entry with:
      | field        | value                  |
      | order_id     | ORD-001                |
      | saga_step    | PAYMENT                |
      | result       | COMPENSATED            |
      | timestamp    | non-null               |
    And the order "ORD-001" status should remain "PENDING_PAYMENT"

  # 🔀 Edge Cases

  Scenario: TC-PAY-010 — SAGA compensation executes idempotently on duplicate trigger
    Given the compensation for order "ORD-001" has already been executed successfully
    When the SAGA triggers compensation for order "ORD-001" again
    Then the system should NOT apply the compensation a second time
    And the compensation log should record a "DUPLICATE_COMPENSATION_SKIPPED" entry
    And the order status should remain unchanged

  Scenario: TC-PAY-011 — SAGA compensation failure triggers manual intervention alert
    Given order "ORD-001" is in PENDING_PAYMENT status
    And the compensation transaction will fail due to a downstream service error
    When the SAGA payment step fails and attempts compensation
    Then the compensation should fail
    And the compensation log should contain an entry with result "COMPENSATION_FAILED"
    And an alert should be triggered for manual intervention
    And the alert payload should contain order_id "ORD-001" and saga_step "PAYMENT"

  Scenario: TC-PAY-012 — SAGA records full audit trail for a successful payment flow
    Given order "ORD-001" is in PENDING_PAYMENT status
    And the payment gateway will accept the transaction
    When the SAGA payment step executes successfully for order "ORD-001"
    Then the SAGA log should contain entries for:
      | saga_step | result  |
      | PAYMENT   | SUCCESS |
    And no compensation entries should exist for order "ORD-001"

  # ❌ Negative Cases

  Scenario: TC-PAY-013 — SAGA does not execute compensation for a successful payment
    Given the payment gateway accepts the transaction for order "ORD-001"
    When the SAGA completes successfully
    Then the compensation log should NOT contain any COMPENSATED entry for order "ORD-001"

  Scenario: TC-PAY-014 — SAGA compensation log is immutable — entries cannot be deleted
    Given a compensation log entry exists for order "ORD-001"
    When a DELETE request is sent to the compensation log for order "ORD-001"
    Then the response status code should be 405 or 403
    And the compensation log entry should still exist

  Scenario: TC-PAY-015 — SAGA handles unavailable compensation log gracefully
    Given the compensation log store is unavailable
    And the payment gateway returns a failure for order "ORD-001"
    When the SAGA attempts to log the compensation result
    Then the system should retry the log write according to the retry policy
    And if all retries are exhausted, an alert should be triggered for manual intervention

  # ⚠️ OPEN — depends on BR-013 (retry count and timeout configuration)

  @open @BR-013
  Scenario: TC-PAY-015B — OPEN: SAGA compensation respects configured retry limit
    Given the retry limit is configured to N attempts
    And the compensation transaction fails on every attempt
    When the SAGA executes compensation for order "ORD-001"
    Then the SAGA should attempt compensation exactly N times
    And after N failures, trigger a manual intervention alert
    # N value depends on BR-013 confirmation
    Then the outcome is PENDING business and technical confirmation of BR-013
```

---

## Feature: 訂單狀態變更通知

> **Mapped to:** US-012
> **Technical dependencies:** Event bus / message queue, Notification service, Member contact info

```gherkin
Feature: 訂單狀態變更通知
  As a system
  Members must receive notifications when their order status changes
  Notification must be triggered for: order created, payment success, payment failure, order cancelled
  Notification content must include: order_id, new status, relevant amount in TWD

  Background:
    Given the notification service is running
    And member "M002" has a registered notification contact
    And the database contains order "ORD-001" belonging to member "M002" with total_twd 3670

  # ✅ Happy Path

  Scenario: TC-NOTIF-001 — Notification is sent when order is created
    Given member "M002" creates a new order "ORD-NEW" with total_twd 3670
    When the order status transitions to "PENDING_PAYMENT"
    Then a notification should be dispatched to member "M002"
    And the notification payload should contain:
      | field      | value           |
      | order_id   | ORD-NEW         |
      | status     | PENDING_PAYMENT |
      | amount_twd | 3670            |

  Scenario: TC-NOTIF-002 — Notification is sent when payment succeeds
    Given order "ORD-001" transitions from "PENDING_PAYMENT" to "PAID"
    Then a notification should be dispatched to member "M002"
    And the notification payload should contain:
      | field      | value |
      | order_id   | ORD-001 |
      | status     | PAID    |
      | amount_twd | 3670    |

  Scenario: TC-NOTIF-003 — Notification is sent when payment fails
    Given order "ORD-001" payment attempt fails and status remains "PENDING_PAYMENT"
    Then a notification should be dispatched to member "M002"
    And the notification payload should contain:
      | field      | value           |
      | order_id   | ORD-001         |
      | status     | PAYMENT_FAILED  |
      | amount_twd | 3670            |

  Scenario: TC-NOTIF-004 — Notification is sent when order is cancelled
    Given order "ORD-001" transitions from "PENDING_PAYMENT" to "CANCELLED"
    Then a notification should be dispatched to member "M002"
    And the notification payload should contain:
      | field      | value     |
      | order_id   | ORD-001   |
      | status     | CANCELLED |
      | amount_twd | 3670      |

  # 🔀 Edge Cases

  Scenario: TC-NOTIF-005 — Notification service unavailable — event is queued for retry
    Given the notification service is temporarily unavailable
    When order "ORD-001" transitions to "PAID"
    Then the notification event should be placed in the retry queue
    And when the notification service recovers, the notification should be delivered
    And the notification should be delivered exactly once (no duplicate)

  Scenario: TC-NOTIF-006 — Notification is not sent for non-triggering status transitions
    Given order "ORD-001" is in "PENDING_PAYMENT" status
    When an internal system update occurs that does NOT change the order status
    Then no notification should be dispatched to member "M002"

  # ❌ Negative Cases

  Scenario: TC-NOTIF-007 — Notification is not sent to a different member for another member's order
    Given order "ORD-003" belongs to member "M003" and transitions to "PAID"
    Then a notification should be dispatched to member "M003"
    And no notification should be dispatched to member "M002"

  Scenario: TC-NOTIF-008 — Notification with missing required field is rejected by notification service
    Given a notification event is generated with missing "order_id" field
    When the notification service processes the event
    Then the notification service should reject the event
    And the rejection should be logged with error code "INVALID_NOTIFICATION_PAYLOAD"
    And no notification should be delivered

  # ⚠️ OPEN — depends on Q10 (notification channel: Email / SMS / Push)

  @open @Q10
  Scenario: TC-NOTIF-008B — OPEN: Notification is delivered via the configured channel
    Given member "M002" has a registered [EMAIL | SMS | PUSH] contact
    When order "ORD-001" transitions to "PAID"
    Then the notification should be delivered via [EMAIL | SMS | PUSH]
    # Channel type depends on Q10 confirmation
    Then the outcome is PENDING business confirmation of Q10
```

---

## Open Items Summary

The following test cases are tagged `@open` and **cannot be finalized** until the corresponding business questions are resolved:

| Tag | Test Case ID | Blocked By | Description |
|---|---|---|---|
| @BR-014 | TC-CAT-020B | BR-014 | Product name uniqueness enforcement |
| @Q8 | TC-CAT-025B | Q8 | Unpublish product with active orders |
| @Q5 | TC-ORD-008B | Q5 | Order creation with insufficient stock |
| @Q5 | TC-ORD-022B | Q5 | Inventory replenishment on order cancellation |
| @Q4 | TC-PAY-008B | Q4 | Payment method selection |
| @BR-013 | TC-PAY-015B | BR-013 | SAGA retry count and timeout |
| @Q10 | TC-NOTIF-008B | Q10 | Notification delivery channel |

> **Action Required:** Business team must resolve Q4, Q5, Q8, Q10, BR-013, and BR-014 before the `@open` scenarios can be implemented and executed.

## 補充情境（人工修正）
Scenario: 重複退貨同一筆訂單應被拒絕
