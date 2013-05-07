package parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import parser.record.ActionRecord;
import parser.record.HeaderRecord;

/**
 * android trace 记录格式解析器
 *
 * @author jingqi
 */
public class TraceRecordParser {

	/**
	 * 分析过程中的状态
	 */
	private static enum State {
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
	 * 分析 android trace 文件中的文本部分，每一行输入一次
	 *
	 * @return 0, 继续分析文本输入，每一行输入一次<br/>
	 *         >0, 结束分析文本，分析二进制数据(返回值是期望的二进制长度)
	 */
	public int inputLine(final String s) {
		Pattern p;
		Matcher m;
		switch (state) {
		case EXPECT_VERSION: // 预期字符串: "*version"
			if (!Pattern.matches("\\s*" + "\\*" + "\\s*" + "(version)" + "\\s*", s))
				throw new IllegalArgumentException("unknown format : " + s);

			state = State.PARSING_VERSION;
			return 0;

		case PARSING_VERSION: // 分析 android trace 文件版本号: "3"
			p = Pattern.compile("\\s*" + "(\\d+)" + "\\s*");
			m = p.matcher(s);
			if (!m.matches())
				throw new IllegalArgumentException("unknown format : " + s);
			final int version = Integer.valueOf(m.group(1));
			if (version != 3)
				throw new RuntimeException("unsupported android trace file version");
			tptpAdapter.addKeyFileVersion(version);
			state = State.PARSING_OPTIONS;
			return 0;

		case PARSING_OPTIONS: // 分析选项信息，例如："data-file-overflow=false"
			p = Pattern.compile("\\s*" + "([\\w\\-]+)" + "\\s*=\\s*" + "([\\w\\-]+)" + "\\s*");
			m = p.matcher(s);
			if (m.matches())
				return 0;

			// 状态转移
			if (Pattern.matches("\\s*" + "\\*" + "\\s*" + "threads" + "\\s*", s)) {
				state = State.PARSING_THREDS;
				return 0;
			}
			throw new IllegalArgumentException("unknown format : " + s);

		case PARSING_THREDS: // 分析线程信息，例如："22	AsyncTask #1"
			p = Pattern.compile("\\s*" + "(\\d+)" + "(\\s+" + "([^\\s](.*[^\\s])?))?" + "\\s*");
			m = p.matcher(s);
			if (m.matches()) {
				final int tid = Integer.valueOf(m.group(1));
				String tname;
				if (m.group(2) != null) // 线程名称可能为空
					tname = m.group(3);
				else
					tname = "";
				tptpAdapter.addThread(tid, tname);
				return 0;
			}

			// 状态转移
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

			// 状态转移
			p = Pattern.compile("\\s*" + "\\*" + "\\s*" + "end" + "\\s*");
			m = p.matcher(s);
			if (m.matches()) {
				state = State.EXPECT_HEAD_RECORD;
				return HeaderRecord.size();
			}
			throw new IllegalArgumentException("unknown format : " + s);

		default:
			throw new IllegalArgumentException("unknown state : " + state);
		}
	}

	/**
	 *
	 * @return >0, 继续输入二进制，(返回值是期望的下次输入的二进制长度)<br/>
	 *         else, 分析完毕
	 */
	public int inputRecord(final byte[] b) {
		switch (state) {
		case EXPECT_HEAD_RECORD:
			HeaderRecord hr = new HeaderRecord(b);
			if (hr.getVersion() != 3)
				throw new RuntimeException();
			tptpAdapter.addDataFileHead(hr.getVersion(), hr.getOffset(), hr.getStartDateTime());
			if (hr.getOffset() == HeaderRecord.size()) {
				state = State.PARSING_RECORDS;
				return 9;
			} else if (hr.getOffset() > HeaderRecord.size()) {
				state = State.EXPECT_BLANK;
				return hr.getOffset() - HeaderRecord.size();
			} else {
				throw new IllegalStateException("offset is lower than already readed : " + hr.getOffset());
			}

		case EXPECT_BLANK:
			state = State.PARSING_RECORDS;
			return ActionRecord.size();

		case PARSING_RECORDS:
			ActionRecord ar = new ActionRecord(b);
			tptpAdapter.addMethodAction(ar.getThreadId(), ar.getMethodAddr(), ar.getMethodAction(), ar.getDeltaTime());
			return ActionRecord.size();
		}
		return -1;
	}

	public void end() {
		tptpAdapter.end();
	}
}
