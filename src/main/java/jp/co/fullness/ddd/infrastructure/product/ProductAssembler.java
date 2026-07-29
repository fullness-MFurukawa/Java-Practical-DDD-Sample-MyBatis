package jp.co.fullness.ddd.infrastructure.product;

import org.springframework.stereotype.Component;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.model.product.Product;
import jp.co.fullness.ddd.infrastructure.category.CategoryRowMapper;
import jp.co.fullness.ddd.infrastructure.category.CategoryRow;
import jp.co.fullness.ddd.infrastructure.stock.ProductStockRow;
import jp.co.fullness.ddd.infrastructure.stock.StockRowMapper;

/**
 * Product 集約の「合成（Row → 集約）」および「分解（集約 → Row）」を担うアセンブラ（MyBatis 版）。
 *
 * <p>責務は<b>型変換と合成/分解のみ</b>で、SQL 実行は Repository が担う。
 * 骨格（{@code restoreSkeleton}）にカテゴリ・在庫を {@code attach} して集約を組み立てる。</p>
 */
@Component
public class ProductAssembler {

    private final ProductRowMapper productRowMapper;
    private final CategoryRowMapper categoryRowMapper;
    private final StockRowMapper stockRowMapper;

    public ProductAssembler(ProductRowMapper productRowMapper,
                            CategoryRowMapper categoryRowMapper,
                            StockRowMapper stockRowMapper) {
        this.productRowMapper = productRowMapper;
        this.categoryRowMapper = categoryRowMapper;
        this.stockRowMapper = stockRowMapper;
    }

    // ----------------------------------------------------------------------
    // 合成（Row → 集約）
    // ----------------------------------------------------------------------

    /**
     * MyBatis の Row DTO 3種から完全な {@link Product} を合成する。
     *
     * <p>骨格（{@code restoreSkeleton}）にカテゴリ・在庫を {@code attach} して合成する。</p>
     *
     * @param pr Product の行（product_uuid, name, price）
     * @param cr Category の行（category_uuid, name）
     * @param sr Stock の行（stock_uuid, stock）
     * @return 合成済みの Product 集約
     * @throws DomainException 必須項目欠落や不正値の場合
     */
    public Product assemble(ProductRow pr, CategoryRow cr, ProductStockRow sr) {
        if (pr == null) throw new DomainException("ProductRow が null です。");
        if (cr == null) throw new DomainException("ProductCategoryRow が null です。");
        if (sr == null) throw new DomainException("ProductStockRow が null です。");

        var product = productRowMapper.toDomain(pr);          // skeleton
        product.attachCategory(categoryRowMapper.toDomain(cr));
        product.attachStock(stockRowMapper.toDomain(sr));
        return product;
    }

    // ----------------------------------------------------------------------
    // 分解（集約 → Row）
    // ----------------------------------------------------------------------

    /**
     * 集約から ProductRow を作る（INSERT 用）。外部キー category_id は Repository で補完する。
     */
    public ProductRow toProductRow(Product product) {
        if (product == null) throw new DomainException("Product が null です。");
        return productRowMapper.fromDomain(product);
    }

    /**
     * 集約から ProductStockRow を作る（INSERT 用）。外部キー product_id は Repository で補完する。
     */
    public ProductStockRow toStockRow(Product product) {
        if (product == null) throw new DomainException("Product が null です。");
        var stock = product.getStock();
        if (stock == null) throw new DomainException("Product に Stock が設定されていません。");
        return stockRowMapper.fromDomain(stock);
    }

    /**
     * 集約から Category の UUID（文字列）を取り出す。Repository で category_id を解決するために利用する。
     */
    public String extractCategoryUuid(Product product) {
        if (product == null) throw new DomainException("Product が null です。");
        var category = product.getCategory();
        if (category == null) throw new DomainException("Product に Category が設定されていません。");
        return category.getCategoryId().value();
    }
}