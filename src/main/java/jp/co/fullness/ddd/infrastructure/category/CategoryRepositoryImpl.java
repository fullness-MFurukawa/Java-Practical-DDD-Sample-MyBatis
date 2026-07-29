package jp.co.fullness.ddd.infrastructure.category;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.model.category.Category;
import jp.co.fullness.ddd.domain.model.category.CategoryId;
import jp.co.fullness.ddd.domain.model.category.CategoryRepository;
import jp.co.fullness.ddd.infrastructure.exception.InternalException;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategorySqlMapper sqlMapper;
    private final CategoryRowMapper rowMapper;   // MapStruct: ProductCategoryRow -> Category

    @Override
    public Optional<Category> findById(CategoryId categoryId) {
        if (categoryId == null) throw new DomainException("商品カテゴリIdは必須です。");
        try {
            CategoryRow row = sqlMapper.findByUuid(categoryId.value());
            return Optional.ofNullable(row).map(rowMapper::toDomain);
        } catch (DomainException ex) {
            throw ex;
        } catch (DataAccessException ex) {   // org.springframework.dao
            throw new InternalException("カテゴリ情報の取得中にデータベースエラーが発生しました。", ex);
        } catch (Exception ex) {
            throw new InternalException("カテゴリ情報の取得処理中に予期しないエラーが発生しました。", ex);
        }
    }

    @Override
    public List<Category> findAll() {
        try {
            return sqlMapper.findAll().stream()
                    .map(rowMapper::toDomain)
                    .toList();
        } catch (DomainException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new InternalException("カテゴリ一覧の取得中にデータベースエラーが発生しました。", ex);
        } catch (Exception ex) {
            throw new InternalException("カテゴリ一覧の取得処理中に予期しないエラーが発生しました。", ex);
        }
    }
}