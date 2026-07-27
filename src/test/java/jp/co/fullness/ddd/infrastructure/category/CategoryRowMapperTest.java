package jp.co.fullness.ddd.infrastructure.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.model.category.Category;

/**
 * {@link CategoryRowMapper} の単体テスト（Spring コンテナ経由）。
 *
 * <p>この Mapper は {@code @Mapper(componentModel = "spring")} により Spring の Bean として
 * 生成される（実装は MapStruct が生成する {@code CategoryRowMapperImpl}）。DI で実際に
 * 注入される Bean をそのまま検証するため {@code @SpringBootTest} を用いる。</p>
 *
 * <p>変換ロジック自体は DB を参照しないが、{@code @SpringBootTest} はコンテキスト全体を
 * 起動するため、実行時はローカル PostgreSQL が起動している必要がある。</p>
 */
@SpringBootTest
@DisplayName("CategoryRowMapper: MyBatis Row → Category の変換（DI 経由）")
class CategoryRowMapperTest {

    @Autowired
    private CategoryRowMapper mapper;

    /** ProductCategoryRow は同一パッケージなので import 不要 */
    private ProductCategoryRow row(String categoryUuid, String name) {
        ProductCategoryRow r = new ProductCategoryRow();
        r.setCategoryUuid(categoryUuid);
        r.setName(name);
        return r;
    }

    @Nested
    @DisplayName("正常系")
    class Success {

        @Test
        @DisplayName("有効な Row を Category に変換できる")
        void toDomain_valid() {
            String uuid = "11111111-1111-1111-1111-111111111111";

            Category category = mapper.toDomain(row(uuid, "文房具"));

            assertEquals(uuid, category.getCategoryId().value());
            assertEquals("文房具", category.getName().value());
        }
    }

    @Nested
    @DisplayName("異常系（DomainException を送出する）")
    class Failure {

        @Test
        @DisplayName("Row が null なら例外")
        void toDomain_nullRow() {
            assertThrows(DomainException.class, () -> mapper.toDomain(null));
        }

        @Test
        @DisplayName("category_uuid が空文字/空白なら例外")
        void toDomain_blankUuid() {
            assertThrows(DomainException.class, () -> mapper.toDomain(row("   ", "文房具")));
        }

        @Test
        @DisplayName("name が空文字/空白なら例外")
        void toDomain_blankName() {
            String uuid = "11111111-1111-1111-1111-111111111111";
            assertThrows(DomainException.class, () -> mapper.toDomain(row(uuid, "   ")));
        }

        @Test
        @DisplayName("category_uuid が UUID 形式でないなら例外（VO のバリデーション）")
        void toDomain_invalidUuidFormat() {
            assertThrows(DomainException.class, () -> mapper.toDomain(row("not-a-uuid", "文房具")));
        }
    }
}
