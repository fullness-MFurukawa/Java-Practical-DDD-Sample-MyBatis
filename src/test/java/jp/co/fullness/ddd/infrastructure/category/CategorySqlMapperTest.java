package jp.co.fullness.ddd.infrastructure.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * {@link CategorySqlMapper}（MyBatis の SQL マッパー）の結合テスト（実 PostgreSQL に接続）。
 *
 * <p>SQL / 自動マッピングの層を Row DTO の入出力で直接検証する。読み取りのみ。</p>
 *
 * <p><b>前提：</b>ローカル PostgreSQL が起動し、{@code restapi_exercise} に
 * サンプルデータ（カテゴリ 5 件）が投入済みであること。</p>
 */
@SpringBootTest
@DisplayName("CategorySqlMapper（MyBatis SQL）結合テスト（ローカル PostgreSQL / サンプルデータ前提）")
class CategorySqlMapperTest {

    @Autowired
    private CategorySqlMapper sqlMapper;

    @Nested
    @DisplayName("findByUuid")
    class FindByUuid {

        @Test
        @DisplayName("実在する category_uuid で1件取得できる")
        void existing() {
            // category_uuid はランダム生成なので、findAll から実在の1件を取り UUID で引き直す
            List<ProductCategoryRow> all = sqlMapper.findAll();
            assertFalse(all.isEmpty(), "サンプルのカテゴリが存在すること");
            ProductCategoryRow sample = all.get(0);

            ProductCategoryRow found = sqlMapper.findByUuid(sample.getCategoryUuid());

            assertNotNull(found);
            assertEquals(sample.getId(), found.getId());
            assertEquals(sample.getCategoryUuid(), found.getCategoryUuid());
            assertEquals(sample.getName(), found.getName());
        }

        @Test
        @DisplayName("存在しない category_uuid なら null")
        void missing() {
            assertNull(sqlMapper.findByUuid(UUID.randomUUID().toString()));
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("サンプルのカテゴリ『文房具』が取得できる")
        void containsSampleCategory() {
            boolean hasStationery = sqlMapper.findAll().stream()
                    .anyMatch(r -> "文房具".equals(r.getName()));

            assertTrue(hasStationery, "サンプルデータに『文房具』カテゴリが存在すること");
        }
    }
}