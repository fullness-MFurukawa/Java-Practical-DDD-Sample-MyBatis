package jp.co.fullness.ddd.infrastructure.product;

import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.model.product.Product;
import jp.co.fullness.ddd.domain.model.product.ProductId;
import jp.co.fullness.ddd.domain.model.product.ProductName;
import jp.co.fullness.ddd.domain.model.product.ProductRepository;
import jp.co.fullness.ddd.infrastructure.exception.InternalException;
import jp.co.fullness.ddd.infrastructure.stock.ProductStockRow;

/**
 * {@link ProductRepository} の MyBatis による実装。
 *
 * <p>SQL 実行は {@link ProductSqlMapper}（MyBatis）へ、Row ↔ 集約 の変換は
 * {@link ProductAssembler} へ委譲する。</p>
 *
 * <p>MyBatis の例外は mybatis-spring により Spring の
 * {@link org.springframework.dao.DataAccessException} に翻訳されるため、そこを捕捉する。</p>
 */
@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductSqlMapper sqlMapper;
    private final ProductAssembler assembler;

    public ProductRepositoryImpl(ProductSqlMapper sqlMapper, ProductAssembler assembler) {
        this.sqlMapper = sqlMapper;
        this.assembler = assembler;
    }

    @Override
    public void create(Product product) {
        if (product == null) {
            throw new DomainException("商品は必須です。");
        }
        try {
            // カテゴリUUID(文字列) → カテゴリの内部PK(INT) を解決
            String categoryUuid = assembler.extractCategoryUuid(product);
            Integer categoryPk = sqlMapper.findCategoryPkByUuid(categoryUuid);
            if (categoryPk == null) {
                throw new DomainException("指定された商品カテゴリが存在しません。");
            }

            // 集約 → Row（外部キーは未設定）
            ProductRow pr = assembler.toProductRow(product);
            ProductStockRow sr = assembler.toStockRow(product);

            // product に category_id を補完して INSERT（採番PKは pr.id に書き戻る）
            pr.setCategoryId(categoryPk);
            sqlMapper.insertProduct(pr);

            // stock に product_id を補完して INSERT
            sr.setProductId(pr.getId());
            sqlMapper.insertStock(sr);

        } catch (DomainException ex) {
            throw ex;   // ドメイン例外はそのまま伝播させる
        } catch (DataAccessException ex) {
            throw new InternalException("商品登録中にデータベースエラーが発生しました。", ex);
        } catch (Exception ex) {
            throw new InternalException("商品登録処理中に予期しないエラーが発生しました。", ex);
        }
    }

    @Override
    public void update(Product product) {
        if (product == null) {
            throw new DomainException("商品は必須です。");
        }
        try {
            // 集約 → Row（変更後の名称・単価・在庫数を反映。UUIDで対象を特定する）
            ProductRow pr = assembler.toProductRow(product);
            ProductStockRow sr = assembler.toStockRow(product);

            // 商品(product)を product_uuid で特定し、名称・単価を UPDATE
            // ※カテゴリは「商品を変更する」ユースケースの変更対象外のため更新しない
            int updatedProduct = sqlMapper.updateProduct(pr);
            if (updatedProduct == 0) {
                // 事前に findById で存在確認済みのため、ここに到達するのは想定外(並行削除など)
                throw new InternalException("更新対象の商品が見つかりませんでした。");
            }

            // 在庫(product_stock)を stock_uuid で特定し、在庫数を UPDATE
            sqlMapper.updateStock(sr);

        } catch (DomainException ex) {
            throw ex;          // ドメイン例外はそのまま伝播させる
        } catch (InternalException ex) {
            throw ex;          // 自前で投げた InternalException を generic catch で二重ラップしない
        } catch (DataAccessException ex) {
            throw new InternalException("商品変更中にデータベースエラーが発生しました。", ex);
        } catch (Exception ex) {
            throw new InternalException("商品変更処理中に予期しないエラーが発生しました。", ex);
        }
    }

    @Override
    public boolean existsByName(ProductName productName) {
        if (productName == null) {
            throw new DomainException("商品名は必須です。");
        }
        try {
            return sqlMapper.existsByName(productName.value());
        } catch (DataAccessException ex) {
            throw new InternalException("商品名の存在確認中にデータベースエラーが発生しました。", ex);
        } catch (Exception ex) {
            throw new InternalException("商品名の存在確認処理中に予期しないエラーが発生しました。", ex);
        }
    }

    @Override
    public Optional<Product> findById(ProductId productId) {
        if (productId == null) {
            throw new DomainException("商品Idは必須です。");
        }
        try {
            ProductRow row = sqlMapper.findByUuid(productId.value());
            if (row == null) {
                return Optional.empty();
            }
            // JOIN でネスト格納された関連を Assembler に渡して合成する
            return Optional.of(assembler.assemble(row, row.getCategory(), row.getStock()));
        } catch (DomainException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new InternalException("商品情報の取得中にデータベースエラーが発生しました。", ex);
        } catch (Exception ex) {
            throw new InternalException("商品情報の取得処理中に予期しないエラーが発生しました。", ex);
        }
    }

    @Override
    public Optional<Product> findByName(ProductName productName) {
        if (productName == null) {
            throw new DomainException("商品名は必須です。");
        }
        try {
            ProductRow row = sqlMapper.findByName(productName.value());
            if (row == null) {
                return Optional.empty();
            }
            return Optional.of(assembler.assemble(row, row.getCategory(), row.getStock()));
        } catch (DomainException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new InternalException("商品名による検索中にデータベースエラーが発生しました。", ex);
        } catch (Exception ex) {
            throw new InternalException("商品名による検索処理中に予期しないエラーが発生しました。", ex);
        }
    }
}