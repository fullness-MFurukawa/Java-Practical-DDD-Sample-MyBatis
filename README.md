# 商品管理API — ドメイン駆動設計 実践サンプル（MyBatis版）

商品管理REST APIを題材に、ドメイン駆動設計（DDD）を「分析 → 設計 → 実装」まで一貫して学ぶための実装例です。
研修テキスト『ドメイン駆動設計実践 Java編』に対応します。

本リポジトリは **永続化技術に MyBatis を採用した版** です。
同一のドメインモデル・アプリケーション層・プレゼンテーション層を保ちながら、インフラストラクチャ層だけを差し替えた 3 リポジトリ構成のうちのひとつです。

| ORM | リポジトリ |
|---|---|
| jOOQ | practical_sample_jooq |
| **MyBatis** | **practical_sample_mybatis**（本リポジトリ） |
| Spring Data JPA | practical_sample_springdatajpa |

> 3 版はドメイン／アプリ／プレゼン層のコードが共通です。受講企業は自社で採用している ORM の版だけを追えば学習が完結します。

---

## 特徴

- レイヤードアーキテクチャ（クリーンアーキテクチャ準拠）による 4 層構成
- ドメイン層を永続化技術から独立させ、ORM をインフラ層に閉じ込める設計
- 値オブジェクト（VO）による「正しい状態しか作れない」ドメインモデル
- 不変条件・バリデーションをドメイン層に集約
- **MyBatis の XML マッパー**による SQL の外部化と、ネストした `<association>` による集約のマッピング
- **専用の Row DTO + Assembler（腐敗防止層）** で Row ⇔ ドメインの変換を分離
- **MapStruct** による DTO ⇔ ドメインの変換
- JUnit 5 による単体テスト・結合テストを完備
- OpenAPI ドキュメント UI（Swagger UI）を同梱

---

## アーキテクチャ

```
プレゼンテーション層 (presentation)   … Controller / Schema / ExceptionHandler
        ↓ 依存
アプリケーション層 (application)       … UseCase / Service / DTO
        ↓ 依存
   ドメイン層 (domain)  ← 中核。技術に依存しない
        ↑ 依存（依存性逆転）
インフラストラクチャ層 (infrastructure) … MyBatis による Repository 実装
```

- ドメイン層はどのレイヤ・どの技術にも依存しません。
- インフラ層はドメイン層のインターフェイス（Repository / Mapper）を実装します（依存性逆転の原則）。
- MyBatis に依存するコードはインフラ層に閉じ込め、ドメイン／アプリ／プレゼンの 3 層は他の ORM 版と共通です。

### パッケージ構成（ベース: `jp.co.fullness.ddd`）

```
jp.co.fullness.ddd
├── domain                       … ドメイン層（技術非依存）
│   ├── model
│   │   ├── category             … Category / CategoryId / CategoryName / CategoryRepository
│   │   ├── product              … Product（集約ルート）/ ProductId / ProductName / ProductPrice / ProductRepository
│   │   └── stock                … Stock / StockId / StockQuantity / StockRepository
│   ├── mapper                   … ToDomainMapper / DomainBiMapper（変換の抽象）
│   └── exception                … DomainException（ドメインルール違反）
├── infrastructure               … 永続化の実装（MyBatis）
│   ├── category                 … ProductCategoryRow / CategoryRowMapper / CategorySqlMapper / CategoryRepositoryImpl
│   ├── stock                    … ProductStockRow / StockRowMapper
│   └── product                  … ProductRow / ProductRowMapper / ProductAssembler / ProductSqlMapper / ProductRepositoryImpl
├── application                  … アプリケーション層
│   ├── dto                      … CategoryDTO / ProductDTO / StockDTO
│   ├── mapper                   … CategoryMapper / ProductMapper / StockMapper（MapStruct）/ ProductDTOAssembler
│   ├── category                 … CategoryService / CategoryServiceImpl
│   └── product                  … ProductService / usecase（RegisterProduct / SearchProductByName）
└── presentation                … プレゼンテーション層
    ├── product.controller       … RegisterProductController / SearchProductByNameController
    ├── product.schema           … ProductCreateSchema / ProductCreateSchemaMapper
    ├── config                   … OpenApiConfig
    └── exception                … ApiExceptionHandler
```

- インフラ層は概念（category / stock / product）ごとに分割し、Row DTO・`@Mapper` インターフェイス（SqlMapper）・Assembler・Repository 実装を同じパッケージに配置します。
- XML マッパーは `src/main/resources/mapper/*.xml` に配置します。単一 ORM 構成のため `@Profile` は使用しません。

---

## ドメインモデルとビジネスルール

| 概念 | 型 | 種別 | 主なルール |
|---|---|---|---|
| 商品 | `Product` | エンティティ（集約ルート） | Category と Stock を集約として保持 |
| 商品ID | `ProductId` | 値オブジェクト | UUID（canonical・小文字） |
| 商品名 | `ProductName` | 値オブジェクト | 必須・最大30文字・前後トリム |
| 商品単価 | `ProductPrice` | 値オブジェクト | 50〜10000 |
| カテゴリ | `Category` | エンティティ | 名称を変更可能（rename） |
| カテゴリID | `CategoryId` | 値オブジェクト | UUID（canonical・小文字） |
| カテゴリ名 | `CategoryName` | 値オブジェクト | 必須・最大20文字・前後トリム |
| 商品在庫 | `Stock` | エンティティ | 在庫の加算・減算を自身で管理 |
| 在庫ID | `StockId` | 値オブジェクト | UUID（canonical・小文字） |
| 在庫数 | `StockQuantity` | 値オブジェクト | 0〜100 |

ファクトリの命名規約:

- ID系VO … `createNew()`（新規採番）/ `fromString(String)`（既存値から復元）
- 値系VO … `of(...)`（生の値から生成）
- エンティティ … `createNew(...)`（新規）/ `restore(...)`（復元）

集約 `Product` は、まず骨格（skeleton）を復元し、`attachCategory` / `attachStock` で構成要素を組み立てることで完全な集約を構築します。

---

## 技術スタック

- Java 21
- Spring Boot 4.0.7
- Gradle（Wrapper 同梱）
- PostgreSQL
- **MyBatis**（mybatis-spring-boot-starter 4.1.0 / XML マッパー）
- MapStruct 1.6.3（DTO ⇔ ドメイン変換）
- HikariCP（コネクションプール）
- springdoc-openapi 3.0.3（Swagger UI 同梱・Spring Boot 4 対応）
- テスト: JUnit 5 / Mockito

> 各ライブラリの正確なバージョンは `build.gradle` を参照してください。

---

## セットアップ

### 前提

- JDK 21 が有効になっていること

```bash
java -version   # 21.x であることを確認
```

### データベース（PostgreSQL）

データベース名は `restapi_exercise` です。付属の SQL でデータベース・テーブル・サンプルデータを作成できます。

```bash
psql -U postgres -f restapi_exercise.sql
```

作成されるテーブル: `product_category` / `product` / `product_stock`
（`*_uuid` 列は `VARCHAR(36)` で、ドメインの識別子 UUID を文字列として保持します）

接続情報は `src/main/resources/application.yml` で設定します（既定値）。

| 項目 | 値 |
|---|---|
| URL | `jdbc:postgresql://localhost:5432/restapi_exercise` |
| ユーザー | `postgres` |
| パスワード | `training` |

---

## MyBatis の設定

`application.yml` の `mybatis:` はトップレベル（`spring:` の下ではない）に置きます。

```yaml
mybatis:
  type-aliases-package: jp.co.fullness.ddd.infrastructure   # Row を型エイリアスとして解決
  mapper-locations: classpath:mapper/*.xml                  # XML マッパーの配置場所
  configuration:
    map-underscore-to-camel-case: true                     # snake_case 列 → camelCase プロパティ
```

- `@Mapper` インターフェイス（SqlMapper）と対応する XML を組み合わせて SQL を定義します。
- 集約のマッピングは XML の `<resultMap>` 内でネストした `<association>` を用い、商品にカテゴリ・在庫を紐付けます。
- 登録系は `useGeneratedKeys` で自動採番の主キーを取得します。

> `type-aliases-package` が古いパッケージを指していたり `spring:` 配下に誤って置かれていると「Could not resolve type alias」エラーになります。上記のトップレベル指定を守ってください。

---

## 実行

```bash
./gradlew bootRun
```

| URL | 内容 |
|---|---|
| `http://localhost:8080/` | Swagger UI へリダイレクト |
| `http://localhost:8080/swagger-ui/index.html` | OpenAPI ドキュメント UI（Swagger UI） |
| `http://localhost:8080/v3/api-docs` | OpenAPI 仕様（JSON） |

### 主なエンドポイント

| メソッド | パス | 内容 |
|---|---|---|
| GET | `/api/products/categories` | カテゴリ一覧 |
| GET | `/api/products/categories/{id}` | カテゴリ取得 |
| GET | `/api/products/exists?name=...` | 商品名の重複確認（存在しなければ 204） |
| GET | `/api/products/search?name=...` | 商品名で検索 |
| POST | `/api/products` | 商品登録（成功時 201・Location ヘッダ） |

### 例外とHTTPステータスの対応

| 例外 | ステータス |
|---|---|
| `DomainException` / `InvalidInputException` / 入力検証違反 | 400 Bad Request |
| `NotFoundException` | 404 Not Found |
| `ExistsException` | 409 Conflict |
| その他（`InternalException` 等） | 500 Internal Server Error |

---

## テスト

```bash
# すべてのテストを実行
./gradlew test

# ドメイン層のみ
./gradlew test --tests "jp.co.fullness.ddd.domain.*"

# インフラ層（MyBatis）のみ
./gradlew test --tests "jp.co.fullness.ddd.infrastructure.*"
```

- ドメイン層: VO・エンティティの境界値・null・異常値・等価性・集約の完全性を検証（JUnit 5）。
- インフラ層: RowMapper は `@SpringBootTest` + `@Autowired`、Assembler は Mockito、SqlMapper / Repository は `@SpringBootTest` + `@Transactional` でローカル PostgreSQL に対して検証。
- アプリ層: Service / UseCase は Mockito、MapStruct マッパーは生成実装を直接検証。
- プレゼン層: `@WebMvcTest` + `@MockitoBean` でルーティング・入力検証・レスポンス整形を検証。

> 結合テストはローカルの PostgreSQL（`restapi_exercise`）に接続します。サンプルデータの UUID は `gen_random_uuid()` で採番されるため、テストは固定 UUID を前提とせず `findAll()` 等で実データを取得してから検証します。

---

## 実装状況

| レイヤ / 要素 | 状態 |
|---|---|
| ドメイン層（VO・エンティティ・例外） | ✅ 実装・レビュー済み |
| インフラ層（MyBatis） | ✅ 実装・テスト済み |
| アプリケーション層 | ✅ 実装・テスト済み |
| プレゼンテーション層 | ✅ 実装・テスト済み |
| Swagger UI / OpenAPI | ✅ 稼働確認済み |
| 単体・結合テスト（JUnit 5） | ✅ 全件グリーン |

---

## ライセンス / 著作

© 2026 株式会社フルネス
本プロジェクトは研修教材『ドメイン駆動設計実践 Java編』の付属サンプルです。