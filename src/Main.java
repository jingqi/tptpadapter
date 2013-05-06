import java.io.*;

import parser.*;

public class Main {

	/**
	 * android 性能跟踪文件是 .trace 文件
	 * TPTP(eclipse 的性能分析插件)性能文件是 .trcbin .trcxml 文件(分别对应二进制和xml格式)
	 */
	public static void main(final String[] args) throws IOException {

		String source = null, dest = null;

		// for debug
//		source = "./testopen.trace";
//		dest = "./output.trcxml";

		// get source
		if (source == null) {
			if (args.length >= 1) {
				source = args[0];
			} else {
				System.out.println("usage: convert Android trace file to TPTP xml trace file\n"
					+ "command: \n\t./convert sourcefile [destfile]");
				return;
			}
		}

		if (dest == null) {
			if (args.length >= 2) {
				dest = args[1];
			} else if (source.toLowerCase().endsWith(".trace")) {
				dest = source.substring(0, source.length() - 5) + "trcxml";
			} else {
				dest = source + ".trcxml";
			}
		}

		// 文件写入器
		final FileWriter fw = new FileWriter(dest);

		// TPTP Trace 文件写入器
		final TptpXmlFileWriter tfw = new TptpXmlFileWriter(fw);

		// TPTP 适配器
		final TptpAdapter ta = new TptpAdapter(tfw);

		// Android Trace 记录解析器
		final TraceRecordParser trp = new TraceRecordParser(ta);

		// android trace 文件
		final File tracefile = new File(source);

		// Android Trace 文件流转换为记录流
		final TraceFileScaner tfs = new TraceFileScaner(tracefile, trp);

		// 开始处理
		try {
			tfs.process();
		} finally {
			fw.close();
		}
	}
}
