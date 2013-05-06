package parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * 将 Android Trace 文件中的数据转换为一定顺序的记录序列
 */
public class TraceFileScaner {

	private final File traceFile;
	private final TraceRecordParser parser;

	/**
	 * @param f
	 *            Android Trace 文件
	 * @param p
	 *            Android Trace 格式解析器
	 */
	public TraceFileScaner(final File f, final TraceRecordParser p) {
		traceFile = f;
		parser = p;
	}

	/** 将文件流拆解为记录流 */
	public void process() throws IOException {
		int bytesToRead = 0;

		// 分析文本部分
		final FileReader fr = new FileReader(traceFile);
		try {
			while (bytesToRead == 0) {
				final StringBuilder line = new StringBuilder();
				int c = fr.read();
				while (c != '\n') {
					if (c == -1)
						throw new IllegalArgumentException("unexpected EOF");
					line.append((char) c);
					c = fr.read();
				}
				bytesToRead = parser.inputLine(line.toString());
			}
		} finally {
			fr.close();
		}

		// 查找二进制头部
		if (bytesToRead != 16)
			throw new IllegalStateException("unexpected length");
		final FileInputStream fis = new FileInputStream(traceFile);
		try {
			int magic = 0; // 首部魔术数字 0x574f4c53 ('SLOW')
			while (magic != 0x574f4c53) {
				magic >>>= 8;
				magic &= 0x00FFFFFF;
				final int b = fis.read();
				if (b == -1)
					throw new IllegalArgumentException("unexpected EOF");
				magic |= (b << 24);
			}
			byte[] record = new byte[bytesToRead];
			record[0] = (byte) (magic & 0xFF);
			record[1] = (byte) ((magic >> 8) & 0xFF);
			record[2] = (byte) ((magic >> 16) & 0xFF);
			record[3] = (byte) ((magic >> 24) & 0xFF);
			int rs = fis.read(record, 4, bytesToRead - 4);
			if (rs != bytesToRead - 4)
				throw new IllegalArgumentException("unexpected EOF");
			bytesToRead = parser.inputRecord(record);

			// 获取二进制记录
			final long totalLength = fis.available();
			long currentReaded = 0;
			int lastPercent = 0;
			while (bytesToRead > 0) {
				record = new byte[bytesToRead];
				currentReaded += bytesToRead;
				rs = fis.read(record);
				if (rs != record.length)
					break;
				bytesToRead = parser.inputRecord(record);

				// log
				if (currentReaded * 100 / totalLength - lastPercent >= 1) {
					lastPercent = (int) (currentReaded * 100 / totalLength);
					Logger.getLogger(this.getClass().getCanonicalName()).info(
							String.format("processed %d%%", lastPercent));
				}
			}
		} finally {
			fis.close();
		}

		parser.end();
		Logger.getLogger(this.getClass().getCanonicalName()).info(String.format("processing done."));
	}
}
