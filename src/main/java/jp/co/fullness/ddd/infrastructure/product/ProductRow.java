package jp.co.fullness.ddd.infrastructure.product;

import jp.co.fullness.ddd.infrastructure.category.ProductCategoryRow;
import jp.co.fullness.ddd.infrastructure.stock.ProductStockRow;
import lombok.Getter;
import lombok.Setter;

/**
 * product テーブルの1行を受け取る Row DTO（MyBatis の受け皿）。
 *
 * <p>フラットな product カラムに加えて、JOIN 取得時にはネストした
 * {@link #category} / {@link #stock} を保持する（XML の {@code <association>} で
 * 流し込まれる）。単純な INSERT 時にはこれらは null。</p>
 */
@Getter
@Setter
public class ProductRow {

    /** 主キー（自動採番） */
    private Integer id;

    /** 識別Id（product_uuid, VARCHAR(36)） */
    private String productUuid;

    /** 商品名 */
    private String name;

    /** 単価 */
    private Integer price;

    /** カテゴリの外部キー（product_category.id・INSERT 時に補完） */
    private Integer categoryId;

    // ---- JOIN 取得時のみ設定される関連（ResultMap の association で流し込まれる） ----

    /** JOIN で取得したカテゴリ行 */
    private ProductCategoryRow category;

    /** JOIN で取得した在庫行 */
    private ProductStockRow stock;
}

