package jp.co.fullness.ddd.infrastructure.stock;

import lombok.Getter;
import lombok.Setter;

/**
 * product_stock テーブルの1行を受け取る Row DTO（MyBatis の受け皿）。
 *
 * <p>ドメインへの変換は {@code StockRowMapper}（MapStruct）が担う。</p>
 */
@Getter
@Setter
public class ProductStockRow {

    /** 主キー（自動採番） */
    private Integer id;

    /** 識別Id（stock_uuid, VARCHAR(36)） */
    private String stockUuid;

    /** 在庫数 */
    private Integer stock;

    /** 商品Id（外部キー・INSERT 時に補完） */
    private Integer productId;
}
