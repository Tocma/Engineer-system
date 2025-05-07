package model;

import java.util.List;

/**
 * エンジニア情報へのデータアクセスを抽象化するインターフェース
 * CRUD操作の定義と標準化
 *
 * <p>
 * このインターフェースは、エンジニア情報へのアクセス方法を抽象化し、
 * 実装の詳細から独立した標準的なデータ操作を定義します。
 * これにより、データストレージの実装が変更されても、上位層のコードを変更せずに済みます。
 * </p>
 *
 * @author Nakano
 * @version 4.0.0
 * @since 2025-04-15
 */
public interface EngineerDAO {

    /**
     * 全エンジニア情報を取得
     *
     * @return エンジニア情報のリスト
     */
    List<EngineerDTO> findAll();

    /**
     * IDによりエンジニア情報を取得
     *
     * @param id 検索するエンジニアID
     * @return エンジニア情報（存在しない場合はnull）
     */
    EngineerDTO findById(String id);

    /**
     * エンジニア情報を保存
     *
     * @param engineer 保存するエンジニア情報
     */
    void save(EngineerDTO engineer);

    /**
     * エンジニア情報を更新
     *
     * @param engineer 更新するエンジニア情報
     */
    void update(EngineerDTO engineer);

    /**
     * エンジニア情報を削除
     *
     * @param id 削除するエンジニアID
     */
    void deleteAll(List<String> ids);



}
