package util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import util.LogHandler.LogType;

/**
 * システムパフォーマンス測定・監視クラス
 * 既存のLogHandlerと統合し、メソッド実行時間、メモリ使用量、リソース利用状況を測定
 * 
 * 主な機能:
 * - メソッド実行時間の測定
 * - メモリ使用量の監視
 * - パフォーマンスレポートの生成
 * - 既存ログシステムとの統合
 * 
 * @author System Performance Team
 */
public class PerformanceMonitor {

    /** シングルトンインスタンス */
    private static final PerformanceMonitor INSTANCE = new PerformanceMonitor();

    /** 実行時間測定用のマップ */
    private final ConcurrentMap<String, Long> startTimes = new ConcurrentHashMap<>();

    /** パフォーマンス記録の保存用マップ */
    private final ConcurrentMap<String, PerformanceRecord> performanceRecords = new ConcurrentHashMap<>();

    /** メモリ監視用MXBean */
    private final MemoryMXBean memoryBean;

    /** LogHandlerインスタンス */
    private final LogHandler logHandler;

    /** 測定開始時のメモリ状態を保存 */
    private final ConcurrentMap<String, MemorySnapshot> memorySnapshots = new ConcurrentHashMap<>();

    /** 統計情報カウンター */
    private final AtomicLong totalMeasurements = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);

    /**
     * パフォーマンス測定レコードを保持するクラス
     */
    public static class PerformanceRecord {
        private final String methodName;
        private final long executionTime;
        private final long memoryUsedBefore;
        private final long memoryUsedAfter;
        private final LocalDateTime timestamp;
        private final String threadName;

        public PerformanceRecord(String methodName, long executionTime,
                long memoryUsedBefore, long memoryUsedAfter,
                LocalDateTime timestamp, String threadName) {
            this.methodName = methodName;
            this.executionTime = executionTime;
            this.memoryUsedBefore = memoryUsedBefore;
            this.memoryUsedAfter = memoryUsedAfter;
            this.timestamp = timestamp;
            this.threadName = threadName;
        }

        // Getters
        public String getMethodName() {
            return methodName;
        }

        public long getExecutionTime() {
            return executionTime;
        }

        public long getMemoryUsedBefore() {
            return memoryUsedBefore;
        }

        public long getMemoryUsedAfter() {
            return memoryUsedAfter;
        }

        public long getMemoryDelta() {
            return memoryUsedAfter - memoryUsedBefore;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getThreadName() {
            return threadName;
        }
    }

    /**
     * メモリスナップショットクラス
     */
    private static class MemorySnapshot {
        private final long heapUsed;

        public MemorySnapshot(long heapUsed, long heapMax, long nonHeapUsed) {
            this.heapUsed = heapUsed;
            System.currentTimeMillis();
        }

        public long getHeapUsed() {
            return heapUsed;
        }

    }

    /**
     * プライベートコンストラクタ（シングルトンパターン）
     */
    private PerformanceMonitor() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.logHandler = LogHandler.getInstance();
        ResourceManager.getInstance();

        // 初期化ログ
        logHandler.log(LogType.SYSTEM, "PerformanceMonitor初期化完了");
    }

    /**
     * シングルトンインスタンス取得
     * 
     * @return PerformanceMonitorインスタンス
     */
    public static PerformanceMonitor getInstance() {
        return INSTANCE;
    }

    /**
     * メソッドの実行時間測定を開始
     * 
     * @param methodName 測定対象メソッド名
     */
    public void startMeasurement(String methodName) {
        String key = generateKey(methodName);
        long startTime = System.nanoTime();

        startTimes.put(key, startTime);

        // メモリスナップショット取得
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();

        MemorySnapshot snapshot = new MemorySnapshot(
                heapMemory.getUsed(),
                heapMemory.getMax(),
                nonHeapMemory.getUsed());

        memorySnapshots.put(key, snapshot);

        logHandler.log(Level.FINE, LogType.SYSTEM,
                String.format("パフォーマンス測定開始: %s [Thread: %s]",
                        methodName, Thread.currentThread().getName()));
    }

    /**
     * メソッドの実行時間測定を終了し、結果を記録
     * 
     * @param methodName 測定対象メソッド名
     * @return 実行時間（ナノ秒）
     */
    public long endMeasurement(String methodName) {
        String key = generateKey(methodName);
        Long startTime = startTimes.remove(key);
        MemorySnapshot startSnapshot = memorySnapshots.remove(key);

        if (startTime == null) {
            logHandler.log(Level.WARNING, LogType.SYSTEM,
                    "測定開始時間が見つかりません: " + methodName);
            return -1;
        }

        long endTime = System.nanoTime();
        long executionTime = endTime - startTime;

        // 終了時メモリ取得
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        long endMemoryUsed = heapMemory.getUsed();
        long startMemoryUsed = startSnapshot != null ? startSnapshot.getHeapUsed() : 0;

        // パフォーマンスレコード作成
        PerformanceRecord record = new PerformanceRecord(
                methodName,
                executionTime,
                startMemoryUsed,
                endMemoryUsed,
                LocalDateTime.now(),
                Thread.currentThread().getName());

        performanceRecords.put(key + "_" + System.currentTimeMillis(), record);

        // 統計更新
        totalMeasurements.incrementAndGet();
        totalExecutionTime.addAndGet(executionTime);

        // ログ出力
        logHandler.log(LogType.SYSTEM,
                String.format("パフォーマンス測定完了: %s - 実行時間: %.3fms, メモリ変化: %+.2fMB",
                        methodName,
                        executionTime / 1_000_000.0,
                        (endMemoryUsed - startMemoryUsed) / (1024.0 * 1024.0)));

        return executionTime;
    }

    /**
     * 指定されたメソッドの処理を測定しながら実行
     * 
     * @param methodName メソッド名
     * @param task       実行するタスク
     * @return 実行時間（ナノ秒）
     */
    public long measureExecution(String methodName, Runnable task) {
        long executionTime = 0;
        try {
            task.run();
        } catch (Exception e) {
            logHandler.logError(LogType.SYSTEM,
                    "測定中にエラーが発生: " + methodName, e);
            throw e;
        } finally {
            executionTime = endMeasurement(methodName);
        }
        return executionTime;
    }

    /**
     * 現在のシステムメモリ使用状況を取得
     * 
     * @return メモリ使用状況の文字列
     */
    public String getCurrentMemoryStatus() {
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();

        return String.format(
                "ヒープメモリ: %.2f/%.2fMB (%.1f%%), " +
                        "非ヒープメモリ: %.2fMB",
                heapMemory.getUsed() / (1024.0 * 1024.0),
                heapMemory.getMax() / (1024.0 * 1024.0),
                (double) heapMemory.getUsed() / heapMemory.getMax() * 100,
                nonHeapMemory.getUsed() / (1024.0 * 1024.0));
    }

    /**
     * パフォーマンス統計レポートを生成
     * 
     * @return パフォーマンス統計情報
     */
    public PerformanceReport generateReport() {
        List<PerformanceRecord> records = new ArrayList<>(performanceRecords.values());

        if (records.isEmpty()) {
            return new PerformanceReport(Collections.emptyList(), 0, 0, getCurrentMemoryStatus());
        }

        // 実行時間でソート（降順）
        records.sort(Comparator.comparingLong(PerformanceRecord::getExecutionTime).reversed());

        long avgExecutionTime = totalExecutionTime.get() / totalMeasurements.get();

        return new PerformanceReport(records, totalMeasurements.get(), avgExecutionTime, getCurrentMemoryStatus());
    }

    /**
     * パフォーマンス統計情報をログファイルに出力
     */
    public void logPerformanceStatistics() {
        PerformanceReport report = generateReport();

        logHandler.log(LogType.SYSTEM, "=== パフォーマンス統計サマリー ===");
        logHandler.log(LogType.SYSTEM, String.format("総測定回数: %d回", report.getTotalMeasurements()));
        logHandler.log(LogType.SYSTEM, String.format("平均実行時間: %.3fms",
                report.getAverageExecutionTime() / 1_000_000.0));
        logHandler.log(LogType.SYSTEM, String.format("現在のメモリ状況: %s", report.getMemoryStatus()));

        // 実行時間上位5件をログに記録
        logHandler.log(LogType.SYSTEM, "=== 実行時間上位5件 ===");
        report.getRecords().stream()
                .limit(5)
                .forEach(record -> logHandler.log(LogType.SYSTEM,
                        String.format("%s: %.3fms (メモリ変化: %+.2fMB) [%s]",
                                record.getMethodName(),
                                record.getExecutionTime() / 1_000_000.0,
                                record.getMemoryDelta() / (1024.0 * 1024.0),
                                record.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
    }

    /**
     * パフォーマンス記録をクリア
     */
    public void clearRecords() {
        performanceRecords.clear();
        startTimes.clear();
        memorySnapshots.clear();
        totalMeasurements.set(0);
        totalExecutionTime.set(0);

        logHandler.log(LogType.SYSTEM, "パフォーマンス記録をクリアしました");
    }

    /**
     * キー生成用のヘルパーメソッド
     * 
     * @param methodName メソッド名
     * @return 一意キー
     */
    private String generateKey(String methodName) {
        return methodName + "_" + Thread.currentThread().getId();
    }

    /**
     * パフォーマンスレポートクラス
     */
    public static class PerformanceReport {
        private final List<PerformanceRecord> records;
        private final long totalMeasurements;
        private final long averageExecutionTime;
        private final String memoryStatus;

        public PerformanceReport(List<PerformanceRecord> records, long totalMeasurements,
                long averageExecutionTime, String memoryStatus) {
            this.records = new ArrayList<>(records);
            this.totalMeasurements = totalMeasurements;
            this.averageExecutionTime = averageExecutionTime;
            this.memoryStatus = memoryStatus;
        }

        public List<PerformanceRecord> getRecords() {
            return records;
        }

        public long getTotalMeasurements() {
            return totalMeasurements;
        }

        public long getAverageExecutionTime() {
            return averageExecutionTime;
        }

        public String getMemoryStatus() {
            return memoryStatus;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== パフォーマンスレポート ===\n");
            sb.append(String.format("総測定回数: %d\n", totalMeasurements));
            sb.append(String.format("平均実行時間: %.3fms\n", averageExecutionTime / 1_000_000.0));
            sb.append(String.format("現在のメモリ状況: %s\n", memoryStatus));
            sb.append("\n=== 実行時間上位10件 ===\n");

            records.stream()
                    .limit(10)
                    .forEach(record -> sb.append(String.format(
                            "%s: %.3fms (メモリ変化: %+.2fMB)\n",
                            record.getMethodName(),
                            record.getExecutionTime() / 1_000_000.0,
                            record.getMemoryDelta() / (1024.0 * 1024.0))));

            return sb.toString();
        }
    }
}