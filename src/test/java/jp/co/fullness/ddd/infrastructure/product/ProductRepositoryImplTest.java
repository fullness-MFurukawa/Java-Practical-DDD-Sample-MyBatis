package jp.co.fullness.ddd.infrastructure.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jp.co.fullness.ddd.domain.model.category.Category;
import jp.co.fullness.ddd.domain.model.product.Product;
import jp.co.fullness.ddd.domain.model.product.ProductId;
import jp.co.fullness.ddd.domain.model.product.ProductName;
import jp.co.fullness.ddd.domain.model.product.ProductPrice;
import jp.co.fullness.ddd.domain.model.product.ProductRepository;
import jp.co.fullness.ddd.domain.model.stock.StockQuantity;

/**
 * {@link ProductRepositoryImpl}（MyBatis 実装）の結合テスト（実 PostgreSQL に接続）。
 *
 * <p><b>前提条件：</b></p>
 * <ul>
 *   <li>ローカルの PostgreSQL が起動し、{@code restapi_exercise} に付属 SQL の
 *       サンプルデータが投入済みであること。</li>
 *   <li>MyBatis（mybatis-spring）により、DSL 実行が Spring のトランザクションに
 *       追随すること（下記 @Transactional でのロールバックが効く前提）。</li>
 * </ul>
 *
 * <p>プロジェクトが単一 ORM になったため {@code @Profile} / {@code @ActiveProfiles} は不要。
 * クラスに {@code @Transactional} を付けているため、各テストの永続化操作は終了時に
 * 自動ロールバックされ、サンプルデータを汚さない。</p>
 */
@SpringBootTest
@Transactional
@DisplayName("ProductRepositoryImpl（MyBatis）結合テスト（ローカル PostgreSQL / サンプルデータ前提）")
class ProductRepositoryImplTest {

    @Autowired
    private ProductRepository repository;

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
            assertTrue(repository.existsByName(ProductName.of(EXISTING_NAME)));
        }
        @Test
        @DisplayName("存在しない商品名なら false")
        void exists_false() {
            assertFalse(repository.existsByName(ProductName.of(MISSING_NAME)));
        }
    }

    @Nested
    @DisplayName("findByName")
    class FindByName {
        @Test
        @DisplayName("存在する商品を取得できる（カテゴリ・在庫も合成される）")
        void find_existing() {
            Optional<Product> found = repository.findByName(ProductName.of(EXISTING_NAME));
            assertTrue(found.isPresent(), "サンプルデータの商品が取得できること");
            Product p = found.get();
            assertEquals(EXISTING_NAME, p.getName().value());
            assertEquals(120, p.getPrice().value().intValue());
            assertEquals("文房具", p.getCategory().getName().value());
            assertEquals(80, p.getStock().getQuantity().value().intValue());
        }
        @Test
        @DisplayName("存在しない商品名なら空の Optional")
        void find_missing() {
            assertTrue(repository.findByName(ProductName.of(MISSING_NAME)).isEmpty());
        }
    }

    @Nested
    @DisplayName("create → findById（ラウンドトリップ）")
    class CreateAndFind {
        @Test
        @DisplayName("新規商品を登録し、ID で取得できる")
        void create_then_findById() {
            // 既存商品から実在するカテゴリを借りる（外部キーが解決できることを保証する）
            Category category = repository.findByName(ProductName.of(EXISTING_NAME))
                    .orElseThrow(() -> new AssertionError("前提のサンプル商品が見つからない"))
                    .getCategory();
            Product newProduct = Product.createNew(
                    ProductName.of("結合テスト商品"),
                    ProductPrice.of(500),
                    category,
                    StockQuantity.of(15));
            ProductId newId = newProduct.getProductId();
            repository.create(newProduct);
            Optional<Product> found = repository.findById(newId);
            assertTrue(found.isPresent(), "登録した商品が ID で取得できること");
            Product p = found.get();
            assertEquals("結合テスト商品", p.getName().value());
            assertEquals(500, p.getPrice().value().intValue());
            assertEquals(category.getCategoryId().value(), p.getCategory().getCategoryId().value());
            assertEquals(15, p.getStock().getQuantity().value().intValue());
            assertTrue(repository.existsByName(ProductName.of("結合テスト商品")));
        }
        @Test
        @DisplayName("存在しない ID なら空の Optional")
        void findById_missing() {
            assertTrue(repository.findById(ProductId.createNew()).isEmpty());
        }
    }

    @Nested
    @DisplayName("update（名称・単価・在庫数の変更）")
    class Update {
        @Test
        @DisplayName("既存商品の名称・単価・在庫数を変更し、ID で取得して反映を確認できる")
        void update_then_findById() {
            // 実在するカテゴリを借りて更新対象の商品を新規登録する
            Category category = repository.findByName(ProductName.of(EXISTING_NAME))
                    .orElseThrow(() -> new AssertionError("前提のサンプル商品が見つからない"))
                    .getCategory();

            Product target = Product.createNew(
                    ProductName.of("変更前商品"),
                    ProductPrice.of(300),
                    category,
                    StockQuantity.of(10));
            ProductId id = target.getProductId();
            repository.create(target);

            // 登録済みの集約を取得し、名称・単価・在庫数を変更する
            Product loaded = repository.findById(id)
                    .orElseThrow(() -> new AssertionError("登録した商品が取得できない"));
            // 在庫行の同一性（stock_uuid）が保持されることを後で確認するため控えておく
            String stockUuidBefore = loaded.getStock().getStockId().value();

            loaded.rename(ProductName.of("変更後商品"));
            loaded.reprice(ProductPrice.of(750));
            loaded.changeStock(StockQuantity.of(42));

            // 永続化（更新）
            repository.update(loaded);

            // ID で取得して変更が反映されていることを検証
            Product updated = repository.findById(id)
                    .orElseThrow(() -> new AssertionError("更新後の商品が取得できない"));
            assertEquals("変更後商品", updated.getName().value());
            assertEquals(750, updated.getPrice().value().intValue());
            assertEquals(42, updated.getStock().getQuantity().value().intValue());

            // カテゴリは変更対象外なので不変であること
            assertEquals(category.getCategoryId().value(),
                    updated.getCategory().getCategoryId().value());
            // 在庫行の同一性（stock_uuid）が保持されていること（＝再INSERTではなく更新である担保）
            assertEquals(stockUuidBefore, updated.getStock().getStockId().value());

            // 旧名では存在しなくなり、新名で存在すること
            assertFalse(repository.existsByName(ProductName.of("変更前商品")));
            assertTrue(repository.existsByName(ProductName.of("変更後商品")));
        }
    }
}