package jp.co.fullness.ddd.infrastructure.product;

import org.mapstruct.Mapper;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.mapper.DomainBiMapper;
import jp.co.fullness.ddd.domain.model.product.Product;
import jp.co.fullness.ddd.domain.model.product.ProductId;
import jp.co.fullness.ddd.domain.model.product.ProductName;
import jp.co.fullness.ddd.domain.model.product.ProductPrice;

/**
 * MyBatis の {@link ProductRow} とエンティティ {@link Product}（骨格）を相互変換する Mapper。
 *
 * <p>{@code toDomain} はフラットな product カラムのみを使い、カテゴリ・在庫を伴わない
 * 骨格（{@code restoreSkeleton}）を返す。ネストした {@code row.category} / {@code row.stock} は
 * ここでは扱わず、Assembler が別 Mapper で変換・合成する。</p>
 */
@Mapper(componentModel = "spring")
public interface ProductRowMapper extends DomainBiMapper<ProductRow, Product> {

    @Override
    default Product toDomain(ProductRow row) {
        if (row == null) {
            throw new DomainException("商品情報が取得できません。");
        }

        String productUuid = row.getProductUuid();
        String name = row.getName();
        Integer price = row.getPrice();

        if (productUuid == null || productUuid.isBlank()) {
            throw new DomainException("商品UUIDが不正です。");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("商品名が未設定です。");
        }
        if (price == null) {
            throw new DomainException("商品価格が未設定です。");
        }

        // カテゴリ・在庫は別 Mapper で変換し、後段（Assembler）で合成する
        return Product.restoreSkeleton(
                ProductId.fromString(productUuid),
                ProductName.of(name),
                ProductPrice.of(price));
    }

    @Override
    default ProductRow fromDomain(Product domain) {
        if (domain == null) {
            throw new DomainException("Product エンティティが null です。");
        }

        ProductRow row = new ProductRow();
        row.setProductUuid(domain.getProductId().value());
        row.setName(domain.getName().value());
        row.setPrice(domain.getPrice().value());
        // 外部キー category_id はここでは設定しない（Repository が補完する）
        return row;
    }
}
