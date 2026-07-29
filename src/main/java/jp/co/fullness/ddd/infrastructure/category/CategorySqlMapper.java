package jp.co.fullness.ddd.infrastructure.category;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * カテゴリ取得用の MyBatis SQL マッパー（SQL 本体は
 * {@code resources/mapper/CategorySqlMapper.xml} に定義）。
 *
 * <p>この {@code @Mapper} は <b>MyBatis の</b> {@link org.apache.ibatis.annotations.Mapper}
 * であり、MapStruct の {@code @Mapper} とは別物である点に注意。SQL の実行はこの XML
 * マッパーが担う。</p>
 *
 * <p>返却型は Row DTO であり、ドメイン {@code Category} への変換は
 * {@code CategoryRepositoryImpl} が {@code CategoryRowMapper} を通して行う。
 * product_category は単一テーブル・ネストなしなので ResultMap は不要
 *（{@code map-underscore-to-camel-case} により category_uuid → categoryUuid が自動対応）。</p>
 */
@Mapper
public interface CategorySqlMapper {

    /**
     * category_uuid でカテゴリを1件取得する。
     *
     * @param categoryUuid カテゴリの識別Id
     * @return 見つかれば {@link CategoryRow}、無ければ null
     */
    CategoryRow findByUuid(@Param("categoryUuid") String categoryUuid);

    /**
     * すべてのカテゴリを id 昇順で取得する。
     *
     * @return カテゴリ行のリスト（0件なら空リスト）
     */
    List<CategoryRow> findAll();
}