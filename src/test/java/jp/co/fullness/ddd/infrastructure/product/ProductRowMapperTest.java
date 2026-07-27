package jp.co.fullness.ddd.infrastructure.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.model.category.Category;
import jp.co.fullness.ddd.domain.model.category.CategoryName;
import jp.co.fullness.ddd.domain.model.product.Product;
import jp.co.fullness.ddd.domain.model.product.ProductName;
import jp.co.fullness.ddd.domain.model.product.ProductPrice;
import jp.co.fullness.ddd.domain.model.stock.StockQuantity;

/**
 * {@link ProductRowMapper} の単体テスト（Spring コンテナ経由）。
 *
 * <p>{@code toDomain} は「カテゴリ・在庫を伴わない骨格（skeleton）」の {@link Product} を返すこと、
 * {@code fromDomain} は {@code category_id} を設定しないこと（Repository が補完する）を検証する。</p>
 */
@SpringBootTest
@DisplayName("ProductRowMapper: MyBatis Row ⇔ Product（骨格）の相互変換（DI 経由）")
class ProductRowMapperTest {

    @Autowired
    private ProductRowMapper mapper;

    private static final String UUID_STR = "33333333-3333-3333-3333-333333333333";

    /** ProductRow は同一パッケージなので import 不要 */
    private ProductRow row(String productUuid, String name, Integer price) {
        ProductRow r = new ProductRow();
        r.setProductUuid(productUuid);
        r.setName(name);
        r.setPrice(price);
        return r;
    }

    @Nested
    @DisplayName("toDomain: Row → Product（骨格）")
    class ToDomain {

        @Test
        @DisplayName("有効な Row を骨格 Product に変換できる（カテゴリ・在庫は null）")
        void valid() {
            Product product = mapper.toDomain(row(UUID_STR, "油性ボールペン", 120));

            assertEquals(UUID_STR, product.getProductId().value());
            assertEquals("油性ボールペン", product.getName().value());
            assertEquals(120, product.getPrice().value().intValue());
            assertNull(product.getCategory());
            assertNull(product.getStock());
        }

        @Test
        @DisplayName("Row が null なら例外")
        void nullRow() {
            assertThrows(DomainException.class, () -> mapper.toDomain(null));
        }

        @Test
        @DisplayName("product_uuid が空白なら例外")
        void blankUuid() {
            assertThrows(DomainException.class, () -> mapper.toDomain(row("  ", "商品", 120)));
        }

        @Test
        @DisplayName("name が空白なら例外")
        void blankName() {
            assertThrows(DomainException.class, () -> mapper.toDomain(row(UUID_STR, "  ", 120)));
        }

        @Test
        @DisplayName("price が null なら例外")
        void nullPrice() {
            assertThrows(DomainException.class, () -> mapper.toDomain(row(UUID_STR, "商品", null)));
        }

        @Test
        @DisplayName("price が範囲外（50 未満）なら例外（VO のバリデーション）")
        void outOfRangePrice() {
            assertThrows(DomainException.class, () -> mapper.toDomain(row(UUID_STR, "商品", 10)));
        }
    }

    @Nested
    @DisplayName("fromDomain: Product → Row")
    class FromDomain {

        @Test
        @DisplayName("Product を Row に変換できる（category_id は未設定）")
        void valid() {
            Category category = Category.createNew(CategoryName.of("文房具"));
            Product product = Product.createNew(
                    ProductName.of("油性ボールペン"),
                    ProductPrice.of(120),
                    category,
                    StockQuantity.of(80));

            ProductRow row = mapper.fromDomain(product);

            assertEquals(product.getProductId().value(), row.getProductUuid());
            assertEquals("油性ボールペン", row.getName());
            assertEquals(120, row.getPrice().intValue());
            // 外部キー category_id は Mapper では設定しない（Repository が補完する）
            assertNull(row.getCategoryId());
        }

        @Test
        @DisplayName("Product が null なら例外")
        void nullDomain() {
            assertThrows(DomainException.class, () -> mapper.fromDomain(null));
        }
    }
}
