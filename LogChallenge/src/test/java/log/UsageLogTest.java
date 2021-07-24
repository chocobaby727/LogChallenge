package log;

import static log.UsageLog.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;


public class UsageLogTest {

	@Test
	public void outputFailureInfoの正常系テスト() {
		String fileName = "example.log";
		List<LogData> logList = loadLog(fileName);
		Map<String, List<LogData>> serverAdressToLogData = createServerAdressToLogData(logList);
		int n = 0;
		
		outputFailureInfo(serverAdressToLogData, n);
		
		
	}

	@Test
	public void testOutputOverloadInfo() {
		fail("まだ実装されていません");
	}

	@Test
	public void testOutputNetworkInfo() {
		fail("まだ実装されていません");
	}

	@Test
	public void createServerAdressToLogDataの正常系メソッド() {
		String fileName = "example.log";
		List<LogData> logList = loadLog(fileName);
		
		Map<String, List<LogData>> serverAdressToLogData = createServerAdressToLogData(logList);
		
		assertNotNull(serverAdressToLogData);
	}

	@Test
	public void createSubnetToLogDataの正常系メソッド() {
		String fileName = "example.log";
		List<LogData> logList = loadLog(fileName);
		
		Map<String, List<LogData>> subnetToLogData = createSubnetToLogData(logList);
		
		assertNotNull(subnetToLogData);
	}

	@Test
	public void loadLogの正常系テスト() {
		String fileName = "example.log";
		
		List<LogData> logList = loadLog(fileName);
		
		assertNotNull(logList);
	}
	
	@Test(expected = IOException.class)
	public void loadLogの異常系テスト() {
		String fileName = "";
		
		List<LogData> logList = loadLog(fileName);
		
		assertNotNull(logList);
	}

}
