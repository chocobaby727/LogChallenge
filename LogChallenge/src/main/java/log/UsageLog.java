package log;

import static java.util.stream.Collectors.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 設問に回答するためのコードが記載されています
 */
public class UsageLog {

	/**
	 * 故障情報を出力します
	 * 
	 * @param serverAdressToLogData サーバアドレスごとにグループ化されたMap
	 * @param n                     タイムアウト制限回数
	 */
	public static void outputFailureInfo(Map<String, List<LogData>> serverAdressToLogData, int n) {
		List<Integer> count = new ArrayList<Integer>();

		// 連続して発生するかもしれないタイムアウトの1回目の情報を格納するための変数です
		LogData provisional = new LogData();

		// n回目のタイムアウト（故障状態）の情報を格納するための変数です
		LogData confirm = new LogData();

		System.out.println("【故障情報】");

		serverAdressToLogData.forEach((k, v) -> {
			v.forEach(log -> {
				if (log.getResult().equals("-")) {
					// pingがタイムアウトしていればカウントアップします
					count.add(0);

					if (provisional.getConfirmationDate() == null) {
						provisional.setConfirmationDate(log.getConfirmationDate());
					}

					// 規定のタイムアウト回数を越えた場合、故障が発生したとみなします
					if (count.size() >= n) {
						confirm.setServerAddress(log.getServerAddress());
					}

				} else {
					// pingの応答が復活したのでカウントをリセットします
					count.clear();

					// 故障していたのなら、サーバーアドレスと故障期間を出力します
					if (confirm.getServerAddress() != null) {

						// 故障発生時刻
						LocalDateTime from = provisional.getConfirmationDate();

						// ping応答復活時刻
						LocalDateTime to = log.getConfirmationDate();

						System.out.println("故障状態のサーバアドレス：" + confirm.getServerAddress());
						System.out.println("故障期間：" + calcResponseTime(from, to) + "秒");

						confirm.setServerAddress(null);
					}
					provisional.setConfirmationDate(null);
				}
			});
		});
	}

	/**
	 * 応答時間（2つの引数の差）を計算して返却します
	 * 
	 * @param from 測定開始時刻
	 * @param to   測定終了時刻
	 * @return 応答時間（秒）
	 */
	private static long calcResponseTime(LocalDateTime from, LocalDateTime to) {
		return Duration.between(from, to).toSeconds();
	}

	/**
	 * 直近のログを検査し、過負荷情報を出力します
	 * 
	 * @param serverAdressToLogData サーバアドレスごとにグループ化されたMap
	 * @param m                     検査対象の直近ログ件数
	 * @param t                     許容平均応答時間
	 */
	public static void outputOverloadInfo(Map<String, List<LogData>> serverAdressToLogData, int m, double t) {
		System.out.println("\n【過負荷情報】");

		// 過負荷がかかっているサーバーの情報を格納します
		Map<String, Double> overloads = new LinkedHashMap<String, Double>();

		serverAdressToLogData.forEach((k, v) -> {

			for (int i = 0; i <= v.size(); i += m) {
				List<LogData> extractedLogData = new LinkedList<LogData>();
				try {
					// m件のログ情報を抽出します
					extractedLogData = v.subList(i, m + i);

				} catch (IndexOutOfBoundsException e) {
					return;
				}

				// 1件ごとの応答時間を格納するリストです
				List<Long> responseTimes = new LinkedList<Long>();

				for (int j = 0; j <= extractedLogData.size() - 2; j++) {
					// 1件ごとの応答時間を求めます
					LocalDateTime from = extractedLogData.get(j).getConfirmationDate();
					LocalDateTime to = extractedLogData.get(j + 1).getConfirmationDate();
					responseTimes.add(calcResponseTime(from, to));
				}

				// 応答時間の平均を求めます
				Double averageResponseTime = responseTimes.stream().mapToLong(value -> value).average().getAsDouble();

				if (averageResponseTime.compareTo(t) > -1) {

					/*
					 * 最初のループでは上が実行されますが、以降は値が入っているのでelse句が実行されます。
					 * 同じキー（サーバーアドレス）を追加することで、応答時間の平均を足し合わせた値に更新しています。
					 */
					if (overloads.getOrDefault(k, 0.0).compareTo(0.0) == 0) {
						overloads.put(k, averageResponseTime);
					} else {
						double total = overloads.get(k);
						total += averageResponseTime;
						overloads.put(k, total);
					}
				}
			}
		});

		if (overloads.isEmpty()) {
			System.out.println("問題ありません");
		} else {
			overloads.forEach((k, v) -> {
				System.out.println(k + "が過負荷状態です。");

				// 平均応答時間の合計に、検査したログの件数ー１をかけることで、過負荷状態期間を求めます。
				System.out.println("過負荷状態期間：" + v * (m - 1) + "秒");
			});
		}

	}

	/**
	 * ネットワーク経路にあるスイッチの障害を検査します
	 * 
	 * @param subnetToLogData サブネットごとにグループ化されたMap
	 * @param n               タイムアウト制限回数
	 */
	public static void outputNetworkInfo(Map<String, List<LogData>> subnetToLogData, int n) {
		System.out.println("\n【サブネット情報出力】");

		List<Integer> count = new ArrayList<Integer>();

		// 連続して発生するかもしれないタイムアウトの1回目の情報を格納します
		LogData provisional = new LogData();

		// n回目のタイムアウト（故障状態）の情報を格納します
		LogData confirm = new LogData();

		Map<String, Long> ans = new LinkedHashMap<String, Long>();

		subnetToLogData.forEach((k, v) -> {
			v.forEach(log -> {
				if (log.getResult().equals("-")) {
					// pingがタイムアウトしていればカウントアップします
					count.add(0);

					if (provisional.getConfirmationDate() == null) {
						provisional.setConfirmationDate(log.getConfirmationDate());
					}

					// 規定のタイムアウト回数を越えた場合、故障とみなします
					if (count.size() >= n) {
						confirm.setSubnet(log.getSubnet());
					}

				} else {
					// pingの応答が復活したのでカウントをリセットします
					count.clear();

					// 故障していたのなら、サーバーアドレスと故障期間を出力する
					if (confirm.getSubnet() != null) {

						// 故障発生時刻
						LocalDateTime from = provisional.getConfirmationDate();

						// ping応答復活時刻
						LocalDateTime to = log.getConfirmationDate();

						// 故障期間を求めます
						long failureTime = calcResponseTime(from, to);

						/*
						 * 最初のループでは上が実行されますが、以降は値が入っているのでelse句が実行されます。
						 * 同じキー（サーバーアドレス）を追加することで、故障期間を足し合わせた値に更新しています。
						 */
						if (ans.getOrDefault(k, (long) 0.0).compareTo((long) 0.0) == 0) {
							ans.put(k, failureTime);
						} else {
							Long total = ans.get(k);
							total += failureTime;
							ans.put(k, total);
						}

						confirm.setSubnet(null);
					}
					provisional.setConfirmationDate(null);
				}
			});
		});

		ans.forEach((k, v) -> {
			System.out.println("故障中のサブネット：" + k);
			System.out.println("故障期間：" + v + "秒");
		});
	}

	/**
	 * サーバーアドレスに紐づけたログ情報を返却します
	 * 
	 * @param logList ログ情報
	 * @return serverAdressToLogData サーバーアドレスに紐づけたログ情報
	 */
	public static Map<String, List<LogData>> createServerAdressToLogData(List<LogData> logList) {
		Map<String, List<LogData>> serverAdressToLogData = new LinkedHashMap<>();

		// すべてのサーバアドレスを取得します
		List<String> allServerAdress = logList.stream().map(LogData::getServerAddress).distinct().collect(toList());

		// サーバーアドレスをキーに、一致するログ情報と紐づけて連想配列に格納します
		allServerAdress.forEach(address -> {
			serverAdressToLogData.put(address,
					logList.stream().filter(log -> log.getServerAddress().equals(address)).collect(toList()));
		});
		return serverAdressToLogData;

	}

	/**
	 * サブネットに紐づけたログ情報を返却します
	 * 
	 * @param logList ログ情報
	 * @return serverAdressToLogData サブネットに紐づけたログ情報
	 */
	public static Map<String, List<LogData>> createSubnetToLogData(List<LogData> logList) {
		Map<String, List<LogData>> subnetToLogData = new LinkedHashMap<>();

		// すべてのサブネットを取得します
		List<String> allSubnet = logList.stream().map(LogData::getSubnet).distinct().collect(toList());

		// サブネットをキーに、一致するログ情報と紐づけて連想配列に格納します
		allSubnet.forEach(subnet -> {
			subnetToLogData.put(subnet,
					logList.stream().filter(log -> log.getSubnet().equals(subnet)).collect(toList()));
		});
		return subnetToLogData;

	}

	/**
	 * ログファイルから情報を読み込みます<br>
	 * 内部で下記のメソッドを呼び出します<br>
	 * {@link #refillToLogData(String)}
	 * 
	 * @return logList LogData
	 */
	public static List<LogData> loadLog(String fileName) {
		Path path = Paths.get(fileName);
		List<LogData> logList = new LinkedList<LogData>();

		try (BufferedReader in = Files.newBufferedReader(path)) {
			String line;
			while ((line = in.readLine()) != null) {
				if (!line.isBlank()) {
					logList.add(refillToLogData(line));
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return logList;
	}

	/**
	 * 受け取ったログ情報をLogData型に変換して返却します
	 * 
	 * @param line 1件分のログ情報
	 * @return LogData
	 */
	private static LogData refillToLogData(String line) {
		LogData data = new LogData();
		String[] splitLog = line.split(",");

		// フォーマットを指定します
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

		// 日付をLocalDateTime型に変換します
		try {
			Date confirmationDate = sdf.parse(splitLog[0]);
			LocalDateTime ldt = LocalDateTime.ofInstant(confirmationDate.toInstant(), ZoneId.systemDefault());
			data.setConfirmationDate(ldt);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		data.setServerAddress(splitLog[1]);
		data.setResult(splitLog[2]);

		// cidr部分を抽出します
		String[] temp = splitLog[1].split("\\.");
		String cidr = temp[temp.length - 1].split("/")[1].trim();
		String subnet;

		if (cidr.equals("8")) {
			subnet = "255.0.0.0";
		} else if (cidr.equals("16")) {
			subnet = "255.255.0.0";
		} else {
			subnet = "255.255.255.0";
		}

		data.setSubnet(subnet);

		return data;
	}

}
