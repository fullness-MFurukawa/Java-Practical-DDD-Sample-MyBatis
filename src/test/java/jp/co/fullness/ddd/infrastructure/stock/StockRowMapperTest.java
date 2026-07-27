package jp.co.fullness.ddd.infrastructure.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.model.stock.Stock;
import jp.co.fullness.ddd.domain.model.stock.StockId;
import jp.co.fullness.ddd.domain.model.stock.StockQuantity;

/**
 * {@link StockRowMapper} の単体テスト（Spring コンテナ経由）。
 *
 * <p>Stock は永続化が必要なため双方向（{@code toDomain} / {@code fromDomain}）で検証する。
 * DI で注入される MapStruct 実装 Bean をそのまま使う。</p>
 */
@SpringBootTest
@DisplayName("StockRowMapper: MyBatis Row ⇔ Stock の相互変換（DI 経由）")
class StockRowMapperTest {

    @Autowired
    private StockRowMapper mapper;

    private static final String UUID_STR = "22222222-2222-2222-2222-222222222222";

    /** ProductStockRow は同一パッケージなので import 不要 */
    private ProductStockRow row(String stockUuid, Integer stock) {
        ProductStockRow r = new ProductStockRow();
        r.setStockUuid(stockUuid);
        r.setStock(stock);
        return r;
    }

    @Nested
    @DisplayName("toDomain: Row → Stock")
    class ToDomain {

        @Test
        @DisplayName("有効な Row を Stock に変換できる")
        void valid() {
            Stock stock = mapper.toDomain(row(UUID_STR, 50));

            assertEquals(UUID_STR, stock.getStockId().value());
            assertEquals(50, stock.getQuantity().value().intValue());
        }

        @Test
        @DisplayName("Row が null なら例外")
        void nullRow() {
            assertThrows(DomainException.class, () -> mapper.toDomain(null));
        }

        @Test
        @DisplayName("stock_uuid が空白なら例外")
        void blankUuid() {
            assertThrows(DomainException.class, () -> mapper.toDomain(row("  ", 50)));
        }

        @Test
        @DisplayName("在庫数が null なら例外")
        void nullQuantity() {
            assertThrows(DomainException.class, () -> mapper.toDomain(row(UUID_STR, null)));
        }

        @Test
        @DisplayName("在庫数が範囲外（100 超）なら例外（VO のバリデーション）")
        void outOfRangeQuantity() {
            assertThrows(DomainException.class, () -> mapper.toDomain(row(UUID_STR, 101)));
        }
    }

    @Nested
    @DisplayName("fromDomain: Stock → Row")
    class FromDomain {

        @Test
        @DisplayName("Stock を Row に変換できる（product_id は未設定）")
        void valid() {
            Stock stock = Stock.restore(StockId.fromString(UUID_STR), StockQuantity.of(30));

            ProductStockRow row = mapper.fromDomain(stock);

            assertEquals(UUID_STR, row.getStockUuid());
            assertEquals(30, row.getStock().intValue());
            // 外部キー product_id は Mapper では設定しない（Repository が補完する）
            assertNull(row.getProductId());
        }

        @Test
        @DisplayName("Stock が null なら例外")
        void nullDomain() {
            assertThrows(DomainException.class, () -> mapper.fromDomain(null));
        }
    }
}