package answer;
import static log.UsageLog.*;

import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Map;

import log.LogData;

public class Answer {
	/** 読み込むログファイル名 */
	private static final String FILE_NAME = "example.log";
	
	/** タイムアウト制限回数 */
	private static final int N = 2;
	
	/** 検査対象の直近ログ件数 */
	private static final int M = 3;
	
	/** 許容平均応答時間 */
	private static final double T = 4.0;
	
	public static void main(String[] args) throws NoSuchFileException {
		List<LogData> logList = loadLog(FILE_NAME);
		
		Map<String, List<LogData>> serverAdressToLogData = createServerAdressToLogData(logList);
		Map<String, List<LogData>> subnetToLogData = createSubnetToLogData(logList);
		
		// 故障情報を出力します
		outputFailureInfo(serverAdressToLogData, N);

		// 過負荷情報を出力します
		outputOverloadInfo(serverAdressToLogData, M, T);

		// ネットワーク経路にあるスイッチの障害情報を出力します
		outputNetworkInfo(subnetToLogData, N);
	}
}