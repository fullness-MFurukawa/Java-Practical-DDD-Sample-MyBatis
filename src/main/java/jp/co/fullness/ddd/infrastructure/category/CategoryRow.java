package jp.co.fullness.ddd.infrastructure.category;

import lombok.Getter;
import lombok.Setter;

/**
 * product_category テーブルの1行を受け取る Row DTO（MyBatis の受け皿）。
 *
 * <p>MyBatis はこの POJO の setter（または map-underscore-to-camel-case）で
 * カラム値を流し込む。ドメインへの変換は {@code CategoryRowMapper}（MapStruct）が担う。</p>
 */
@Getter
@Setter
public class CategoryRow {

    /** 主キー（自動採番） */
    private Integer id;

    /** 識別Id（category_uuid, VARCHAR(36)） */
    private String categoryUuid;

    /** カテゴリ名 */
    private String name;
}
