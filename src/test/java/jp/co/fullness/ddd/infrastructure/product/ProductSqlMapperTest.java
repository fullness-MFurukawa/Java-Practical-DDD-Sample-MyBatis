package jp.co.fullness.ddd.infrastructure.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jp.co.fullness.ddd.infrastructure.stock.StockRow;

/**
 * {@link ProductSqlMapper}（MyBatis の SQL マッパー）の結合テスト（実 PostgreSQL に接続）。
 *
 * <p>Repository 結合テストが「ドメインまで通した」検証なのに対し、本テストは
 * <b>SQL / XML の層だけ</b>を Row DTO の入出力で直接検証する。特に次を確認する。</p>
 * <ul>
 *   <li>{@code findByName} / {@code findByUuid} のネストした ResultMap
 *       （{@code <association>} による category / stock の合成、別名によるカラム振り分け）</li>
 *   <li>{@code insertProduct} / {@code insertStock} の {@code useGeneratedKeys} による
 *       採番PKの書き戻し</li>
 *   <li>{@code updateProduct} / {@code updateStock} による名称・単価・在庫数の更新</li>
 *   <li>{@code existsByName} / {@code findCategoryPkByUuid} の単純クエリ</li>
 * </ul>
 *
 * <p><b>前提：</b>ローカル PostgreSQL が起動し、{@code restapi_exercise} に
 * サンプルデータが投入済みであること。{@code @Transactional} で各テストは自動ロールバックされる。</p>
 */
@SpringBootTest
@Transactional
@DisplayName("ProductSqlMapper（MyBatis SQL）結合テスト（ローカル PostgreSQL / サンプルデータ前提）")
class ProductSqlMapperTest {

    @Autowired
    private ProductSqlMapper sqlMapper;

    /** サンプルデータに存在する商品（文房具 / 単価 120 / 在庫 80） */
    private static final String EXISTING_NAME = "油性ボールペン";

    /** サンプルデータに存在しない商品名 */
    private static final String MISSING_NAME = "存在しない商品ZZZ";

    @Nested
    @DisplayName("existsByName")
    class ExistsByName {
        @Test
        @DisplayName("存在する商品名なら true")
        void exists_true() {
            assertTrue(sqlMapper.existsByName(EXISTING_NAME));
        }
        @Test
        @DisplayName("存在しない商品名なら false")
        void exists_false() {
            assertFalse(sqlMapper.existsByName(MISSING_NAME));
        }
    }

    @Nested
    @DisplayName("findByName / findByUuid（ネストした ResultMap の検証）")
    class Find {
        @Test
        @DisplayName("findByName でカテゴリ・在庫までネスト取得できる")
        void findByName_withNestedRelations() {
            var row = sqlMapper.findByName(EXISTING_NAME);
            assertNotNull(row, "サンプル商品が取得できること");
            assertNotNull(row.getProductUuid());
            assertEquals(EXISTING_NAME, row.getName());
            assertEquals(120, row.getPrice().intValue());
            assertNotNull(row.getCategoryId());
            // association: category
            assertNotNull(row.getCategory(), "ネストした category が合成されること");
            assertNotNull(row.getCategory().getCategoryUuid());
            assertEquals("文房具", row.getCategory().getName());
            // association: stock
            assertNotNull(row.getStock(), "ネストした stock が合成されること");
            assertNotNull(row.getStock().getStockUuid());
            assertEquals(80, row.getStock().getStock().intValue());
        }
        @Test
        @DisplayName("findByUuid は findByName と同じ商品を返す")
        void findByUuid_matchesFindByName() {
            var byName = sqlMapper.findByName(EXISTING_NAME);
            assertNotNull(byName);
            var byUuid = sqlMapper.findByUuid(byName.getProductUuid());
            assertNotNull(byUuid);
            assertEquals(byName.getProductUuid(), byUuid.getProductUuid());
            assertEquals(byName.getName(), byUuid.getName());
            assertEquals(byName.getStock().getStock(), byUuid.getStock().getStock());
        }
        @Test
        @DisplayName("存在しない商品名なら null")
        void findByName_missing_returnsNull() {
            assertNull(sqlMapper.findByName(MISSING_NAME));
        }
    }

    @Nested
    @DisplayName("findCategoryPkByUuid")
    class FindCategoryPk {
        @Test
        @DisplayName("存在する category_uuid なら商品の category_id と一致する PK を返す")
        void existing() {
            var row = sqlMapper.findByName(EXISTING_NAME);
            String categoryUuid = row.getCategory().getCategoryUuid();
            Integer pk = sqlMapper.findCategoryPkByUuid(categoryUuid);
            assertNotNull(pk);
            assertEquals(row.getCategoryId(), pk);
        }
        @Test
        @DisplayName("存在しない category_uuid なら null")
        void missing() {
            assertNull(sqlMapper.findCategoryPkByUuid(UUID.randomUUID().toString()));
        }
    }

    @Nested
    @DisplayName("insertProduct → insertStock（採番の書き戻しと往復）")
    class Insert {
        @Test
        @DisplayName("商品と在庫を登録し、採番PKが書き戻り、findByUuid で取得できる")
        void insert_then_find() {
            // 既存商品から実在するカテゴリの内部PKを借りる
            var existing = sqlMapper.findByName(EXISTING_NAME);
            Integer categoryPk = existing.getCategoryId();
            // 商品を登録（useGeneratedKeys で id が書き戻る）
            String productUuid = UUID.randomUUID().toString();
            ProductRow pr = new ProductRow();
            pr.setProductUuid(productUuid);
            pr.setName("SQLテスト商品");
            pr.setPrice(500);
            pr.setCategoryId(categoryPk);
            sqlMapper.insertProduct(pr);
            assertNotNull(pr.getId(), "採番された商品PKが id に書き戻ること");
            // 在庫を登録（product_id を補完）
            StockRow sr = new StockRow();
            sr.setStockUuid(UUID.randomUUID().toString());
            sr.setStock(15);
            sr.setProductId(pr.getId());
            sqlMapper.insertStock(sr);
            assertNotNull(sr.getId(), "採番された在庫PKが id に書き戻ること");
            // 往復：登録した商品を UUID で取得
            var found = sqlMapper.findByUuid(productUuid);
            assertNotNull(found);
            assertEquals("SQLテスト商品", found.getName());
            assertEquals(500, found.getPrice().intValue());
            assertEquals(15, found.getStock().getStock().intValue());
            assertEquals(existing.getCategory().getCategoryUuid(),
                    found.getCategory().getCategoryUuid());
            assertTrue(sqlMapper.existsByName("SQLテスト商品"));
        }
    }

    @Nested
    @DisplayName("updateProduct / updateStock（名称・単価・在庫数の更新）")
    class Update {
        @Test
        @DisplayName("商品の名称・単価と在庫数を更新でき、findByUuid で反映を確認できる")
        void update_then_find() {
            // 更新対象を登録する（既存カテゴリの内部PKを借りる）
            var existing = sqlMapper.findByName(EXISTING_NAME);
            Integer categoryPk = existing.getCategoryId();

            String productUuid = UUID.randomUUID().toString();
            ProductRow pr = new ProductRow();
            pr.setProductUuid(productUuid);
            pr.setName("SQL更新前商品");
            pr.setPrice(300);
            pr.setCategoryId(categoryPk);
            sqlMapper.insertProduct(pr);

            String stockUuid = UUID.randomUUID().toString();
            StockRow sr = new StockRow();
            sr.setStockUuid(stockUuid);
            sr.setStock(10);
            sr.setProductId(pr.getId());
            sqlMapper.insertStock(sr);

            // 商品を更新（product_uuid で特定。名称・単価を変更）
            ProductRow updateP = new ProductRow();
            updateP.setProductUuid(productUuid);
            updateP.setName("SQL更新後商品");
            updateP.setPrice(750);
            int updatedProduct = sqlMapper.updateProduct(updateP);
            assertEquals(1, updatedProduct, "更新された商品行数は1であること");

            // 在庫を更新（stock_uuid で特定。在庫数を変更）
            StockRow updateS = new StockRow();
            updateS.setStockUuid(stockUuid);
            updateS.setStock(42);
            int updatedStock = sqlMapper.updateStock(updateS);
            assertEquals(1, updatedStock, "更新された在庫行数は1であること");

            // findByUuid で反映を確認
            var found = sqlMapper.findByUuid(productUuid);
            assertNotNull(found);
            assertEquals("SQL更新後商品", found.getName());
            assertEquals(750, found.getPrice().intValue());
            assertEquals(42, found.getStock().getStock().intValue());
            // カテゴリは変更していないので不変
            assertEquals(existing.getCategory().getCategoryUuid(),
                    found.getCategory().getCategoryUuid());
            // 在庫の同一性（stock_uuid）が保たれていること
            assertEquals(stockUuid, found.getStock().getStockUuid());
        }

        @Test
        @DisplayName("存在しない product_uuid の更新は 0 行")
        void update_missing_returnsZero() {
            ProductRow updateP = new ProductRow();
            updateP.setProductUuid(UUID.randomUUID().toString());
            updateP.setName("存在しない商品の更新");
            updateP.setPrice(500);
            assertEquals(0, sqlMapper.updateProduct(updateP));
        }
    }
}