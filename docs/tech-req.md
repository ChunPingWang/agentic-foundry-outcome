# 產品銷售應用 — 技術需求文件

**文件版本：** 1.0
**狀態：** 草稿
**最後更新：** 2025-07-11

---

## ⚠️ 待確認事項（需人工確認後方可定案）

> 以下項目在原始輸入中**未明確指定**，架構師不得自行假設，請相關負責人確認後填入。

| # | 項目 | 說明 | 狀態 |
|---|------|------|------|
| C-01 | **Spring Boot 版本** | Spring Boot 4 截至本文件撰寫日期（2025-07）尚未正式 GA，請確認是否採用最新 Spring Boot 3.x（如 3.5.x），或確認 Spring Boot 4 Milestone/RC 版本號 | 🔴 待確認 |
| C-02 | **Spring Cloud Contract 版本** | 需與最終確認之 Spring Boot 版本對齊，請確認 BOM 版本（如 Spring Cloud 2025.x） | 🔴 待確認 |
| C-03 | **Apache Camel 版本** | 未指定版本，請確認（建議 4.x 系列，需與 Spring Boot 版本相容） | 🔴 待確認 |
| C-04 | **ORM / 資料存取框架** | 未指定，候選方案：Spring Data JPA（Hibernate 6）、jOOQ、Spring Data JDBC，請確認 | 🔴 待確認 |
| C-05 | **認證與授權（Auth）** | 未指定，候選方案：Spring Security + OAuth2/OIDC、JWT、API Key，請確認 | 🔴 待確認 |
| C-06 | **日誌框架與格式** | 未指定，候選方案：Logback / Log4j2，結構化日誌（JSON）或純文字，請確認 | 🔴 待確認 |
| C-07 | **Mono Repo 管理工具** | 未指定，候選方案：純 Gradle Multi-Project、Nx、Turborepo（若含前端），請確認 | 🔴 待確認 |
| C-08 | **訊息佇列 / 事件匯流排** | SAGA pattern 需要非同步通訊媒介，未指定，候選方案：Kafka、RabbitMQ、ActiveMQ Artemis，請確認 | 🔴 待確認 |
| C-09 | **部署目標環境** | 未指定，候選方案：Kubernetes、Docker Compose、Cloud PaaS，影響 Testcontainers 整合策略，請確認 | 🔴 待確認 |
| C-10 | **前端技術** | 未指定是否包含前端模組，若 Mono Repo 含前端，需補充技術棧，請確認 | 🔴 待確認 |

---

## 1. 文件目的

本文件定義「產品銷售應用」的**具體架構約束與技術選型**，作為開發團隊、DevOps 及 QA 的唯一技術基準。所有技術決策須符合本文件規範，變更須經架構審查委員會（Architecture Review Board）核准並更新版本。

---

## 2. 技術堆疊總覽

```
┌─────────────────────────────────────────────────────────┐
│                    Mono Repo (Gradle)                   │
├──────────────┬──────────────────┬───────────────────────┤
│  domain      │  application     │  infrastructure       │
│  (純 POJO)   │  (Use Cases)     │  (Adapters / Camel)   │
├──────────────┴──────────────────┴───────────────────────┤
│              Spring Boot [待確認版本]                    │
│              Java 23 (LTS 候選)                         │
├─────────────────────────────────────────────────────────┤
│  PostgreSQL 16  │  [訊息佇列 待確認]  │  [Auth 待確認]  │
└─────────────────────────────────────────────────────────┘
```

---

## 3. 核心技術約束（已確定）

### 3.1 程式語言

| 項目 | 規格 |
|------|------|
| **語言** | Java |
| **版本** | **Java 23** |
| **語言特性** | 啟用 Preview Features 需明確標註於 `build.gradle`；Record、Sealed Classes、Pattern Matching 鼓勵使用於領域模型 |
| **最低編譯目標** | `--release 23` |

```groovy
// build.gradle（根層）
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += ['--enable-preview']
    options.release = 23
}
```

---

### 3.2 框架

| 項目 | 規格 | 備註 |
|------|------|------|
| **應用框架** | Spring Boot | 版本待 C-01 確認 |
| **SAGA 協調** | **Apache Camel** | 版本待 C-03 確認；採用 Choreography-based SAGA |
| **契約測試** | **Spring Cloud Contract** | 版本待 C-02 確認 |

#### Apache Camel SAGA 約束

- SAGA 步驟必須實作 `compensation route`（補償路由），與正向路由一對一對應
- 每個 Camel Route 必須定義 `errorHandler` 與 `onException` 策略
- SAGA 狀態持久化至 PostgreSQL 16（`camel_saga_*` schema），不得使用記憶體儲存
- Route ID 命名規範：`{bounded-context}.{aggregate}.{action}`，例如 `order.payment.charge`

```java
// SAGA Route 範例骨架
from("direct:order.payment.charge")
    .saga()
        .compensation("direct:order.payment.charge.compensate")
        .option("orderId", header("orderId"))
    .to("direct:payment-service")
    .end();
```

---

### 3.3 建置工具

| 項目 | 規格 |
|------|------|
| **建置工具** | **Gradle**（Kotlin DSL，`*.gradle.kts`） |
| **Wrapper 版本** | 鎖定於 `gradle/wrapper/gradle-wrapper.properties`，CI 禁止使用系統安裝版本 |
| **專案結構** | Gradle Multi-Project（Mono Repo） |
| **版本目錄** | 使用 `gradle/libs.versions.toml`（Version Catalog）統一管理所有依賴版本 |

#### Mono Repo 模組結構（建議）

```
product-sales-app/
├── gradle/
│   └── libs.versions.toml          # 版本目錄
├── settings.gradle.kts
├── build.gradle.kts                # 根層共用設定
├── domain/                         # 純領域模型，零框架依賴
│   └── build.gradle.kts
├── application/                    # Use Cases / Ports
│   └── build.gradle.kts
├── infrastructure/                 # Adapters（DB、MQ、外部 API）
│   └── build.gradle.kts
├── api-gateway/                    # RESTful API 入口
│   └── build.gradle.kts
├── saga-orchestration/             # Apache Camel SAGA 路由
│   └── build.gradle.kts
└── contract-tests/                 # Spring Cloud Contract
    └── build.gradle.kts
```

> **約束：** `domain` 模組**禁止**依賴 `infrastructure`、`application`、任何 Spring 模組。違反者 CI 自動失敗。

---

### 3.4 資料庫

| 項目 | 規格 |
|------|------|
| **資料庫** | **PostgreSQL 16** |
| **連線池** | HikariCP（Spring Boot 預設，需明確設定 `maximum-pool-size`） |
| **Schema 遷移** | **Flyway**（版本需與 PostgreSQL 16 JDBC Driver 相容） |
| **JDBC Driver** | `org.postgresql:postgresql`，版本鎖定於 `libs.versions.toml` |
| **Schema 命名** | 每個 Bounded Context 使用獨立 PostgreSQL Schema（如 `order`、`inventory`、`payment`） |
| **測試資料庫** | Testcontainers 啟動 `postgres:16` Docker Image，禁止使用 H2 替代 |

```toml
# gradle/libs.versions.toml（節錄）
[versions]
postgresql = "42.7.3"
flyway = "10.15.0"
testcontainers = "1.20.x"   # 請鎖定具體版本

[libraries]
postgresql-driver = { module = "org.postgresql:postgresql", version.ref = "postgresql" }
flyway-core = { module = "org.flywaydb:flyway-core", version.ref = "flyway" }
flyway-postgresql = { module = "org.flywaydb:flyway-database-postgresql", version.ref = "flyway" }
```

#### PostgreSQL 16 遷移注意事項

> **[人工補充指示落實]** 以下為從既有系統遷移至 PostgreSQL 16 的強制檢查清單。

##### 3.4.1 版本升級路徑

- PostgreSQL 16 **不支援直接從 PostgreSQL 9.x 以下升級**，需先升至 10.x 再逐步升級
- 建議升級路徑：`舊版本 → 14 → 16`（使用 `pg_upgrade` 工具）
- 升級前必須執行 `pg_upgrade --check` 乾跑（dry-run），確認無相容性問題

##### 3.4.2 SQL 語法與行為變更

| 變更項目 | 影響版本 | 說明 | 行動項目 |
|----------|----------|------|----------|
| `pg_stat_activity.query` 欄位長度限制調整 | PG 14+ | 監控 SQL 可能截斷 | 更新監控查詢 |
| `EXPLAIN` 輸出格式變更 | PG 14+ | 影響效能分析工具 | 更新 APM 解析器 |
| `jsonb` 下標語法（`jsonb[0]`）正式支援 | PG 14+ | 既有 `jsonb_array_element` 仍可用 | 評估是否重構 |
| `MERGE` 語句正式支援 | PG 15+ | 可取代部分 `INSERT ... ON CONFLICT` | 評估重構機會 |
| `ICU` 成為預設 Collation Provider | PG 15+ | 影響字串排序行為，**中文排序需特別驗證** | 執行排序迴歸測試 |
| `pg_wal` 目錄重命名（原 `pg_xlog`） | PG 10+ | 影響備份腳本 | 更新所有備份腳本 |
| `recovery.conf` 廢除 | PG 12+ | 設定移至 `postgresql.conf` | 更新 HA 設定 |
| 邏輯複製（Logical Replication）增強 | PG 16 | 支援 `standby` 發布，影響 CDC 架構 | 評估 CDC 工具相容性 |
| `pg_hba.conf` 新增 `trust` 限制 | PG 16 | 本地連線預設行為變更 | 更新連線認證設定 |

##### 3.4.3 Flyway 遷移腳本規範

```
infrastructure/src/main/resources/db/migration/
├── V1__init_schema.sql
├── V2__create_order_tables.sql
└── R__refresh_views.sql          # 可重複執行的 View 定義
```

- 所有 DDL 變更**禁止**直接修改既有 Migration 檔案，必須新增版本
- 遷移腳本需在 Testcontainers 環境（`postgres:16`）通過後方可合併
- 既有資料庫若已有 Schema，需使用 `flyway baseline` 建立基準線

##### 3.4.4 資料遷移策略

```
┌─────────────────────────────────────────────────────────┐
│                   遷移執行順序                           │
│                                                         │
│  1. 備份既有資料庫（pg_dump --format=custom）           │
│  2. 建立 PostgreSQL 16 新實例                           │
│  3. pg_upgrade --check（乾跑驗證）                      │
│  4. 執行 pg_upgrade（停機遷移）或 pg_logical（零停機）  │
│  5. 執行 Flyway baseline（若為首次納管）                │
│  6. 執行應用層 Flyway migrate                           │
│  7. 執行資料驗證腳本（行數、checksum 比對）             │
│  8. 執行迴歸測試套件                                    │
│  9. 切換連線字串，監控 24 小時                          │
│ 10. 舊實例保留 7 天後下線                               │
└─────────────────────────────────────────────────────────┘
```

##### 3.4.5 效能調校（PostgreSQL 16 新特性）

- 啟用 `pg_stat_io`（PG 16 新增）監控 I/O 效能
- 評估使用 `VACUUM` 並行化改善（PG 16 增強）
- `logical_replication_mode = 'immediate'`（PG 16 新增）可降低複製延遲

---

### 3.5 API 風格

| 項目 | 規格 |
|------|------|
| **API 風格** | **RESTful API** |
| **規範標準** | OpenAPI 3.1（使用 `springdoc-openapi` 自動產生） |
| **版本策略** | URL Path Versioning：`/api/v{n}/...` |
| **資料格式** | JSON（`application/json`），日期時間採 ISO 8601（`yyyy-MM-dd'T'HH:mm:ssZ`） |
| **錯誤格式** | RFC 9457（Problem Details for HTTP APIs） |
| **HTTP 方法語意** | 嚴格遵守 GET/POST/PUT/PATCH/DELETE 語意，禁止 GET 產生副作用 |

```java
// RFC 9457 錯誤回應範例
{
  "type": "https://api.product-sales.com/errors/order-not-found",
  "title": "Order Not Found",
  "status": 404,
  "detail": "Order with ID 'ORD-12345' does not exist.",
  "instance": "/api/v1/orders/ORD-12345",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
}
```

---

### 3.6 架構模式

#### 3.6.1 六角形架構（Hexagonal Architecture）

```
┌─────────────────────────────────────────────────────────┐
│                    Driving Side                         │
│         (REST Controller / Camel Consumer)              │
│                         │                               │
│              ┌──────────▼──────────┐                   │
│              │   Inbound Port      │                   │
│              │   (Interface)       │                   │
│              └──────────┬──────────┘                   │
│                         │                               │
│         ┌───────────────▼───────────────┐              │
│         │        Domain Core            │              │
│         │  (Aggregates / Entities /     │              │
│         │   Value Objects / Services)   │              │
│         └───────────────┬───────────────┘              │
│                         │                               │
│              ┌──────────▼──────────┐                   │
│              │   Outbound Port     │                   │
│              │   (Interface)       │                   │
│              └──────────┬──────────┘                   │
│                         │                               │
│         (Repository Impl / MQ Adapter / HTTP Client)   │
│                    Driven Side                          │
└─────────────────────────────────────────────────────────┘
```

**強制約束：**

- `domain` 模組只包含 Port 介面定義與領域物件，**零框架依賴**
- Adapter 實作類別必須位於 `infrastructure` 模組
- Spring `@Component`、`@Service` 等注解**禁止**出現在 `domain` 模組
- 依賴方向：`infrastructure` → `application` → `domain`（單向）

#### 3.6.2 DDD 戰術設計規範

| 構件 | 規範 |
|------|------|
| **Aggregate** | 每個 Aggregate 有唯一 Aggregate Root，外部只能透過 Root 存取內部 Entity |
| **Value Object** | 使用 Java Record 實作，必須 immutable |
| **Domain Event** | 命名採過去式：`OrderPlaced`、`PaymentCharged`；由 Aggregate Root 發布 |
| **Repository** | Port 介面定義於 `domain`，實作於 `infrastructure`；每個 Aggregate Root 對應一個 Repository |
| **Domain Service** | 僅用於跨 Aggregate 業務邏輯，不持有狀態 |
| **Bounded Context** | 每個 Bounded Context 對應獨立 Gradle 子模組與 PostgreSQL Schema |

#### 3.6.3 SOLID Principles 執行機制

- **SRP**：每個類別只有一個變更原因，由 Code Review Checklist 強制檢查
- **OCP**：擴充點使用 Strategy Pattern / 介面注入，禁止修改既有穩定類別
- **LSP**：繼承關係需通過 Liskov 替換測試，優先使用組合取代繼承
- **ISP**：Port 介面依使用者需求切分，禁止 Fat Interface
- **DIP**：所有跨層依賴透過介面，由 Spring DI 容器注入，禁止 `new` 具體 Adapter

---

### 3.7 測試框架與策略

| 項目 | 規格 |
|------|------|
| **單元測試框架** | **JUnit 5**（`junit-jupiter`） |
| **Mock 框架** | Mockito 5（與 JUnit 5 整合） |
| **整合測試** | **Testcontainers**（`postgres:16` 映像） |
| **契約測試** | **Spring Cloud Contract**（Consumer-Driven Contract） |
| **BDD 框架** | Cucumber（與 JUnit 5 整合）或 JUnit 5 + `@DisplayName` 敘述式命名 |
| **覆蓋率工具** | JaCoCo，最低門檻：Line 80%、Branch 70% |
| **架構測試** | ArchUnit（強制執行六角形架構依賴規則） |

#### 測試分層策略（TDD / BDD）

```
┌─────────────────────────────────────────────────────────┐
│  Layer 4: Contract Tests (Spring Cloud Contract)        │
│           Consumer ↔ Provider API 契約驗證              │
├─────────────────────────────────────────────────────────┤
│  Layer 3: Integration Tests (Testcontainers)            │
│           真實 PostgreSQL 16 + [訊息佇列]               │
├─────────────────────────────────────────────────────────┤
│  Layer 2: Application Tests (Use Case Tests)            │
│           Mock Ports，驗證業務流程                      │
├─────────────────────────────────────────────────────────┤
│  Layer 1: Unit Tests (Domain Tests)                     │
│           純 POJO，零 Mock，最快速                      │
└─────────────────────────────────────────────────────────┘
```

#### TDD 工作流程約束

```
Red → Green → Refactor

1. 先寫失敗測試（Red）
2. 寫最小實作使測試通過（Green）
3. 重構，保持測試綠燈（Refactor）

PR 合併條件：
- 所有測試通過
- 覆蓋率達標
- ArchUnit 架構測試通過
- Contract Tests 通過
```

#### Testcontainers 設定規範

```java
// 共用 PostgreSQL Container（避免每個測試類別重啟）
@Testcontainers
public abstract class PostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("product_sales_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);  // 啟用 Container 重用

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

#### Spring Cloud Contract 規範

- Contract 定義檔案位於 `contract-tests/src/test/resources/contracts/`
- 命名規範：`{provider}/{consumer}/{scenario}.groovy` 或 `.yml`
- Provider 測試自動產生，Consumer 使用 WireMock Stub
- Contract 變更需同時更新 Provider 與 Consumer 測試

---

## 4. 待確認項目的暫定預設值

> 以下為在 C-01 至 C-10 確認前的**暫定技術選型**，僅供開發環境初始化使用，**不得用於生產環境決策**。

| 待確認項 | 暫定預設值 | 風險說明 |
|----------|-----------|----------|
| C-01 Spring Boot 版本 | Spring Boot **3.5.x**（最新 GA） | Spring Boot 4 未 GA，採用 Milestone 版本有 API 破壞性變更風險 |
| C-03 Apache Camel 版本 | **4.8.x**（與 Spring Boot 3.5 相容） | 需驗證 SAGA Component 相容性 |
| C-04 ORM | **Spring Data JPA + Hibernate 6.x** | jOOQ 提供更強型別安全，但學習曲線較高 |
| C-05 Auth | **Spring Security + JWT（Bearer Token）** | 若需 SSO 則需改為 OAuth2/OIDC |
| C-06 日誌 | **Logback + JSON 結構化日誌（Logstash Encoder）** | 需確認 ELK/Loki 等日誌平台 |
| C-07 Mono Repo 工具 | **Gradle Multi-Project（純 Gradle）** | 若含前端需評估 Nx |
| C-08 訊息佇列 | **Apache Kafka**（Camel Kafka Component） | 若輕量需求可改用 RabbitMQ |

---

## 5. CI/CD 約束

| 項目 | 規格 |
|------|------|
| **CI 工具** | 待確認（建議 GitHub Actions / GitLab CI） |
| **必要 CI 閘門** | 單元測試 ✓、整合測試 ✓、Contract Tests ✓、ArchUnit ✓、JaCoCo 覆蓋率 ✓、Flyway 遷移驗證 ✓ |
| **Docker Image** | 使用 `eclipse-temurin:23-jre-alpine` 作為基礎映像 |
| **Gradle Build Cache** | 啟用 Remote Build Cache（CI 環境） |

---

## 6. 非功能性需求（架構層面）

| 項目 | 要求 | 備註 |
|------|------|------|
| **SAGA 冪等性** | 所有 SAGA 步驟必須冪等，使用 `idempotency_key` 欄位去重 | 儲存於 PostgreSQL |
| **資料庫連線** | HikariCP `maximum-pool-size` 依服務實例數計算，避免超過 PostgreSQL `max_connections` | PG 16 預設 100 |
| **API 回應時間** | P99 < 500ms（待業務確認） | 需壓測驗證 |
| **資料庫遷移** | Flyway 遷移腳本執行時間 < 30 秒（避免長時間鎖表） | 大量資料遷移需使用線上遷移策略 |

---

## 7. 文件變更記錄

| 版本 | 日期 | 變更說明 | 作者 |
|------|------|----------|------|
| 1.0 | 2025-07-11 | 初版建立，標記 10 個待確認項目 | 架構師 |

---

> **下一步行動：** 請相關負責人針對第 2 節「⚠️ 待確認事項」中標記為 🔴 的項目，於 **5 個工作天內** 提供確認，以便更新本文件至 1.1 版並解除暫定預設值。