package model;

import java.util.List;

/**
 * エンジニア情報へのデータアクセスを抽象化するインターフェース
 *
 * このインターフェースは、エンジニア情報へのアクセス方法を抽象化し、
 * 実装の詳細から独立した標準的なデータ操作を定義
 *
 * @author Nakano
 */
public interface EngineerDAO {

    /**
     * 全エンジニア情報を取得
     * データストアからすべてのエンジニア情報を読み込み、リストとして返します。
     *
     * @return エンジニア情報のリスト（データが存在しない場合は空のリスト）
     */
    List<EngineerDTO> findAll();

    /**
     * IDによりエンジニア情報を取得
     * 指定されたIDに一致するエンジニア情報を検索し、返します。
     *
     * @param id 検索するエンジニアID（null不可）
     * @return エンジニア情報（存在しない場合はnull）
     */
    EngineerDTO findById(String id);

    /**
     * エンジニア情報を保存
     * 新規エンジニア情報をデータストアに保存します。
     * 既に同じIDのエンジニアが存在する場合の動作は実装に依存します。
     *
     * @param engineer 保存するエンジニア情報（null不可）
     */
    void save(EngineerDTO engineer);

    /**
     * エンジニア情報を更新
     * 既存のエンジニア情報を更新します。
     * 指定されたIDのエンジニアが存在しない場合の動作は実装に依存します。
     *
     * @param engineer 更新するエンジニア情報（null不可）
     */
    void update(EngineerDTO engineer);

    /**
     * 複数のエンジニア情報を削除
     * 指定されたIDリストに含まれるすべてのエンジニア情報を削除します。
     * 存在しないIDが含まれている場合は無視されます。
     *
     * @param ids 削除するエンジニアIDのリスト（null不可）
     */
    boolean deleteAll(List<String> ids);

}
