package jp.co.fullness.ddd.infrastructure.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.model.category.Category;
import jp.co.fullness.ddd.domain.model.category.CategoryName;
import jp.co.fullness.ddd.domain.model.product.Product;
import jp.co.fullness.ddd.domain.model.product.ProductId;
import jp.co.fullness.ddd.domain.model.product.ProductName;
import jp.co.fullness.ddd.domain.model.product.ProductPrice;
import jp.co.fullness.ddd.domain.model.stock.Stock;
import jp.co.fullness.ddd.domain.model.stock.StockQuantity;
import jp.co.fullness.ddd.infrastructure.category.CategoryRowMapper;
import jp.co.fullness.ddd.infrastructure.category.CategoryRow;
import jp.co.fullness.ddd.infrastructure.stock.StockRow;
import jp.co.fullness.ddd.infrastructure.stock.StockRowMapper;

/**
 * {@link ProductAssembler} の単体テスト（DB 不要 / Mockito）。
 *
 * <p>各 Row Mapper をモック化し、Assembler 自身のロジック（skeleton への attach 合成、
 * null ガード、分解メソッドの委譲）だけを検証する。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductAssembler: Row群 ⇔ Product集約 の合成/分解")
class ProductAssemblerTest {

    @Mock
    private ProductRowMapper productRowMapper;
    @Mock
    private CategoryRowMapper categoryRowMapper;
    @Mock
    private StockRowMapper stockRowMapper;

    @InjectMocks
    private ProductAssembler assembler;

    private Category sampleCategory() {
        return Category.createNew(CategoryName.of("文房具"));
    }

    private Product sampleSkeleton() {
        return Product.restoreSkeleton(
                ProductId.createNew(),
                ProductName.of("油性ボールペン"),
                ProductPrice.of(120));
    }

    private Product sampleFullProduct() {
        return Product.createNew(
                ProductName.of("油性ボールペン"),
                ProductPrice.of(120),
                sampleCategory(),
                StockQuantity.of(80));
    }

    @Nested
    @DisplayName("assemble: Row群 → Product集約 の合成")
    class Assemble {

        @Test
        @DisplayName("骨格に Category と Stock を attach して合成する")
        void success() {
            ProductRow pr = new ProductRow();
            CategoryRow cr = new CategoryRow();
            StockRow sr = new StockRow();

            Product skeleton = sampleSkeleton();
            Category category = sampleCategory();
            Stock stock = Stock.createNew(StockQuantity.of(80));

            when(productRowMapper.toDomain(any())).thenReturn(skeleton);
            when(categoryRowMapper.toDomain(any())).thenReturn(category);
            when(stockRowMapper.toDomain(any())).thenReturn(stock);

            Product result = assembler.assemble(pr, cr, sr);

            assertSame(skeleton, result);
            assertSame(category, result.getCategory());
            assertSame(stock, result.getStock());
        }

        @Test
        @DisplayName("ProductRow が null なら例外（Mapper は呼ばれない）")
        void nullProductRow() {
            assertThrows(DomainException.class,
                    () -> assembler.assemble(null, new CategoryRow(), new StockRow()));
        }

        @Test
        @DisplayName("ProductCategoryRow が null なら例外")
        void nullCategoryRow() {
            assertThrows(DomainException.class,
                    () -> assembler.assemble(new ProductRow(), null, new StockRow()));
        }

        @Test
        @DisplayName("ProductStockRow が null なら例外")
        void nullStockRow() {
            assertThrows(DomainException.class,
                    () -> assembler.assemble(new ProductRow(), new CategoryRow(), null));
        }
    }

    @Nested
    @DisplayName("分解: Product集約 → Row")
    class Decompose {

        @Test
        @DisplayName("toProductRow は ProductRowMapper.fromDomain に委譲する")
        void toProductRow_delegates() {
            Product product = sampleFullProduct();
            ProductRow expected = new ProductRow();
            when(productRowMapper.fromDomain(product)).thenReturn(expected);

            assertSame(expected, assembler.toProductRow(product));
        }

        @Test
        @DisplayName("toProductRow は null なら例外")
        void toProductRow_null() {
            assertThrows(DomainException.class, () -> assembler.toProductRow(null));
        }

        @Test
        @DisplayName("toStockRow は Product の Stock を取り出して委譲する")
        void toStockRow_delegates() {
            Product product = sampleFullProduct();
            StockRow expected = new StockRow();
            when(stockRowMapper.fromDomain(product.getStock())).thenReturn(expected);

            assertSame(expected, assembler.toStockRow(product));
        }

        @Test
        @DisplayName("toStockRow は Stock 未設定（骨格）なら例外")
        void toStockRow_noStock() {
            assertThrows(DomainException.class, () -> assembler.toStockRow(sampleSkeleton()));
        }

        @Test
        @DisplayName("extractCategoryUuid は Category の UUID 文字列を返す")
        void extractCategoryUuid_success() {
            Category category = sampleCategory();
            Product product = Product.createNew(
                    ProductName.of("油性ボールペン"),
                    ProductPrice.of(120),
                    category,
                    StockQuantity.of(80));

            assertEquals(category.getCategoryId().value(), assembler.extractCategoryUuid(product));
        }

        @Test
        @DisplayName("extractCategoryUuid は Category 未設定（骨格）なら例外")
        void extractCategoryUuid_noCategory() {
            assertThrows(DomainException.class, () -> assembler.extractCategoryUuid(sampleSkeleton()));
        }
    }
}