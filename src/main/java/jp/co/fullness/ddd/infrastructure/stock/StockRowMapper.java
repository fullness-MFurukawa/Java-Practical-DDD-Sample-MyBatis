package jp.co.fullness.ddd.infrastructure.stock;


import org.mapstruct.Mapper;

import jp.co.fullness.ddd.domain.exception.DomainException;
import jp.co.fullness.ddd.domain.mapper.DomainBiMapper;
import jp.co.fullness.ddd.domain.model.stock.Stock;
import jp.co.fullness.ddd.domain.model.stock.StockId;
import jp.co.fullness.ddd.domain.model.stock.StockQuantity;

/**
 * MyBatis の {@link StockRow} とエンティティ {@link Stock} を相互変換する Mapper。
 *
 * <p>腐敗防止層（ACL）として、永続化構造（Row）とドメイン構造（Stock）の依存を絶つ。</p>
 */
@Mapper(componentModel = "spring")
public interface StockRowMapper extends DomainBiMapper<StockRow, Stock> {

    @Override
    default Stock toDomain(StockRow row) {
        if (row == null) {
            throw new DomainException("在庫情報が取得できません。");
        }

        String stockUuid = row.getStockUuid();
        Integer quantity = row.getStock();

        if (stockUuid == null || stockUuid.isBlank()) {
            throw new DomainException("在庫UUIDが不正です。");
        }
        if (quantity == null) {
            throw new DomainException("在庫数が未設定です。");
        }

        return Stock.restore(
                StockId.fromString(stockUuid),
                StockQuantity.of(quantity));
    }

    @Override
    default StockRow fromDomain(Stock domain) {
        if (domain == null) {
            throw new DomainException("Stock エンティティが null です。");
        }

        StockRow row = new StockRow();
        row.setStockUuid(domain.getStockId().value());
        row.setStock(domain.getQuantity().value());
        // 外部キー product_id はここでは設定しない（Repository が補完する）
        return row;
    }
}