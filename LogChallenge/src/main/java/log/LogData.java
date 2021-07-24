package log;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ログ一件分の情報を管理します
 */
public class LogData {
	
	/** 確認日時 */
	private LocalDateTime confirmationDate;
	
	/** サーバアドレス */
	private String serverAddress;
	
	/** サブネット */
	private String subnet;
	
	/** 応答結果 */
	private String result;
	

	public LocalDateTime getConfirmationDate() {
		return confirmationDate;
	}

	public void setConfirmationDate(LocalDateTime confirmationDate) {
		this.confirmationDate = confirmationDate;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public String getSubnet() {
		return subnet;
	}

	public void setSubnet(String subnet) {
		this.subnet = subnet;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(confirmationDate);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LogData other = (LogData) obj;
		return Objects.equals(confirmationDate, other.confirmationDate);
	}

	@Override
	public String toString() {
		return "LogData [confirmationDate=" + confirmationDate + ", serverAddress=" + serverAddress + ", result="
				+ result + "]";
	}

}
