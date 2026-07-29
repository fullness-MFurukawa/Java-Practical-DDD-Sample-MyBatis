package jp.co.fullness.ddd.infrastructure.category;

import org.mapstruct.Mapper;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.mapper.ToDomainMapper;
import jp.co.fullness.ddd.domain.model.category.Category;
import jp.co.fullness.ddd.domain.model.category.CategoryId;
import jp.co.fullness.ddd.domain.model.category.CategoryName;

/**
 * MyBatis の {@link CategoryRow} をドメインの {@link Category} に変換する腐敗防止層（ACL）。
 *
 * <p>DB 由来の Row を、値の検証を VO（{@link CategoryId} / {@link CategoryName}）へ
 * 委ねてドメインの語彙に翻訳する。この層は「変換」に徹する。</p>
 */
@Mapper(componentModel = "spring")
public interface CategoryRowMapper extends ToDomainMapper<CategoryRow, Category> {

    @Override
    default Category toDomain(CategoryRow row) {
        if (row == null) {
            throw new DomainException("ProductCategoryRow が null です。");
        }

        String categoryUuid = row.getCategoryUuid();
        String name = row.getName();

        if (categoryUuid == null || categoryUuid.isBlank()) {
            throw new DomainException("category_uuid が取得できませんでした。");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("name が取得できませんでした。");
        }

        // VO のファクトリを通すことで、復元時にも不変条件を再検証する
        return Category.restore(
                CategoryId.fromString(categoryUuid),
                CategoryName.of(name));
    }
}
