package parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * android trace 记录格式解析器
 */
public class TraceRecordParser {

	/**
	 * 分析过程中的状态
	 */
	enum State {
		/**
		 * 期待 "*version" 提示语句
		 */
		EXPECT_VERSION,

		/**
		 * 分析日志格式版本
		 */
		PARSING_VERSION,

		/**
		 * 分析选项
		 */
		PARSING_OPTIONS,

		/**
		 * 分析线程列表
		 */
		PARSING_THREDS,

		/**
		 * 分析方法列表
		 */
		PARSING_METHODS,

		/**
		 * 分析头部信息
		 */
		EXPECT_HEAD_RECORD,

		/**
		 * 分析空白
		 */
		EXPECT_BLANK,

		/**
		 * 分析二进制记录
		 */
		PARSING_RECORDS,
	}

	private State state = State.EXPECT_VERSION;

	private final TptpAdapter tptpAdapter;

	public TraceRecordParser(final TptpAdapter fmt) {
		tptpAdapter = fmt;
	}

	/**
	 * @return 0, continue with text (one line each time)<br/>
	 *         >0, continue with binary (the returned value is length expected)<br/>
	 */
	public int inputLine(final String s) {
		Pattern p;
		Matcher m;
		switch (state) {
		case EXPECT_VERSION:
			if (!Pattern.matches("\\s*" + "\\*" + "\\s*" + "version" + "\\s*", s))
				throw new IllegalArgumentException("unknown format : " + s);
			state = State.PARSING_VERSION;
			return 0;

		case PARSING_VERSION:
			p = Pattern.compile("\\s*" + "(\\d+)" + "\\s*");
			m = p.matcher(s);
			if (!m.matches())
				throw new IllegalArgumentException("unknown format : " + s);
			final int version = Integer.valueOf(m.group(1));
			tptpAdapter.addKeyFileVersion(version);
			state = State.PARSING_OPTIONS;
			return 0;

		case PARSING_OPTIONS:
			p = Pattern.compile("\\s*" + "([\\w\\-]+)" + "\\s*=\\s*" + "([\\w\\-]+)" + "\\s*");
			m = p.matcher(s);
			if (m.matches()) {
				return 0;
			}

			if (Pattern.matches("\\s*" + "\\*" + "\\s*" + "threads" + "\\s*", s)) {
				state = State.PARSING_THREDS;
				return 0;
			}
			throw new IllegalArgumentException("unknown format : " + s);

		case PARSING_THREDS:
			p = Pattern.compile("\\s*" + "(\\d+)" + "(\\s+" + "([^\\s](.*[^\\s])?))?" + "\\s*");
			m = p.matcher(s);
			if (m.matches()) {
				final int tid = Integer.valueOf(m.group(1));
				String tname;
				if (m.group(2) != null)
					tname = m.group(3);
				else
					tname = "";
				tptpAdapter.addThread(tid, tname);
				return 0;
			}

			p = Pattern.compile("\\s*" + "\\*" + "\\s*" + "methods" + "\\s*");
			m = p.matcher(s);
			if (m.matches()) {
				state = State.PARSING_METHODS;
				return 0;
			}
			throw new IllegalArgumentException("unknown format : " + s);

		case PARSING_METHODS:
			p = Pattern.compile("\\s*" + "0x([\\da-f]{8})" + "\\s+" + "([^\\s]+)" + "\\s+" + "([^\\s]+)" + "\\s+"
					+ "([^\\s]+)" + "\\s*");
			m = p.matcher(s);
			if (m.matches()) {
				final long methodAddress = Long.valueOf(m.group(1), 16);
				final String className = m.group(2);
				final String methodName = m.group(3);
				final String signature = m.group(4);
				tptpAdapter.addMethod(methodAddress, className, methodName, signature, null, -1);
				return 0;
			}

			p = Pattern.compile("\\s*" + "0x([\\da-f]{8})" + "\\s+" + "([^\\s]+)" + "\\s+" + "([^\\s]+)" + "\\s+"
					+ "([^\\s]+)" + "\\s+" + "([^\\s]+)" + "\\s+" + "(\\-?\\d+)" + "\\s*");
			m = p.matcher(s);
			if (m.matches()) {
				final long methodAddress = Long.valueOf(m.group(1), 16);
				final String className = m.group(2);
				final String methodName = m.group(3);
				final String signature = m.group(4);
				final String sourceFile = m.group(5);
				final int sourceLine = Integer.valueOf(m.group(6));
				tptpAdapter.addMethod(methodAddress, className, methodName, signature, sourceFile, sourceLine);
				return 0;
			}

			p = Pattern.compile("\\s*" + "\\*" + "\\s*" + "end" + "\\s*");
			m = p.matcher(s);
			if (m.matches()) {
				state = State.EXPECT_HEAD_RECORD;
				return 16;
			}
			throw new IllegalArgumentException("unknown format : " + s);

		default:
			throw new IllegalArgumentException("unknown state : " + state);
		}
	}

	/**
	 * @return >0, continue with binary (the returned value is length expected)<br/>
	 *         else, get to the end
	 */
	public int inputRecord(final byte[] b) {
		switch (state) {
		case EXPECT_HEAD_RECORD:
			if (b.length != 16)
				return -1;
			final int magic = (b[0] & 0xFF) | ((b[1] & 0xFF) << 8) | ((b[2] & 0xFF) << 16) | ((b[3] & 0xFF) << 24);
			if (magic != 0x574f4c53)
				return -1;
			final int version = ((b[4] & 0xFF) | ((b[5] & 0xFF) << 8));
			final int offset = ((b[6] & 0xFF) | ((b[7] & 0xFF) << 8));
			final long startDateTime = (b[8] & 0xFFL) | ((b[9] & 0xFFL) << 8) | ((b[10] & 0xFFL) << 16)
					| ((b[11] & 0xFFL) << 24) | ((b[12] & 0xFFL) << 32) | ((b[13] & 0xFFL) << 40)
					| ((b[14] & 0xFFL) << 48) | ((b[15] & 0xFFL) << 56);
			tptpAdapter.addDataFileHead(version, offset, startDateTime);
			if (offset == 16) {
				state = State.PARSING_RECORDS;
				return 9;
			} else if (offset > 16) {
				state = State.EXPECT_BLANK;
				return offset - 16;
			} else {
				throw new IllegalStateException("offset is lower than already readed : " + offset);
			}

		case EXPECT_BLANK:
			state = State.PARSING_RECORDS;
			return 9;

		case PARSING_RECORDS:
			final int threadId = b[0] & 0xFF;
			long methodAddress = (b[1] & 0xFFL) | ((b[2] & 0xFFL) << 8) | ((b[3] & 0xFFL) << 16)
					| ((b[4] & 0xFFL) << 24);
			final int methodAction = (int) (methodAddress & 0x03);
			methodAddress &= ~0x03L;
			final long time = (b[5] & 0xFFL) | ((b[6] & 0xFFL) << 8) | ((b[7] & 0xFFL) << 16) | ((b[8] & 0xFFL) << 24);
			tptpAdapter.addMethodAction(threadId, methodAddress, methodAction, time);
			return 9;
		}
		return -1;
	}

	public void end() {
		tptpAdapter.end();
	}
}
