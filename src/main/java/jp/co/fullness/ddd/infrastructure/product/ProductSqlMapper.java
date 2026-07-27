package jp.co.fullness.ddd.infrastructure.product;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.fullness.ddd.infrastructure.stock.ProductStockRow;

/**
 * MyBatis の SQL マッパー（SQL 本体は {@code resources/mapper/ProductSqlMapper.xml} に定義）。
 *
 * <p>この {@code @Mapper} は <b>MyBatis の</b> {@link org.apache.ibatis.annotations.Mapper}
 * であり、MapStruct の {@code @Mapper} とは別物である点に注意。SQL の実行は
 * この XML マッパーが担う。</p>
 *
 * <p>返却型は Row DTO であり、ドメインへは Repository が Assembler を通して変換する。</p>
 */
@Mapper
public interface ProductSqlMapper {

    /**
     * 指定された商品名が存在するかを返す。
     *
     * @param name 商品名
     * @return 存在すれば true
     */
    boolean existsByName(@Param("name") String name);

    /**
     * category_uuid から product_category の主キー（内部PK）を解決する。
     *
     * @param categoryUuid カテゴリの識別Id
     * @return 主キー。存在しなければ null
     */
    Integer findCategoryPkByUuid(@Param("categoryUuid") String categoryUuid);

    /**
     * product_uuid で商品を1件取得する（カテゴリ・在庫を JOIN してネスト格納）。
     *
     * @param productUuid 商品の識別Id
     * @return ProductRow（カテゴリ・在庫を含む）。存在しなければ null
     */
    ProductRow findByUuid(@Param("productUuid") String productUuid);

    /**
     * 商品名で商品を1件取得する（カテゴリ・在庫を JOIN してネスト格納）。
     *
     * @param name 商品名
     * @return ProductRow（カテゴリ・在庫を含む）。存在しなければ null
     */
    ProductRow findByName(@Param("name") String name);

    /**
     * 商品を1件登録する。採番された主キーは {@code row.id} に書き戻される
     *（XML の {@code useGeneratedKeys="true" keyProperty="id"}）。
     * 事前に {@code categoryId} を補完しておくこと。
     *
     * @param row 登録する商品行
     */
    void insertProduct(ProductRow row);

    /**
     * 在庫を1件登録する。事前に {@code productId} を補完しておくこと。
     *
     * @param row 登録する在庫行
     */
    void insertStock(ProductStockRow row);
}