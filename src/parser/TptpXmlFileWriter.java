package parser;

import java.io.*;

/**
 * 将 TPTP Trace 记录写入文件
 */
public class TptpXmlFileWriter extends FilterWriter implements ITptpHandler {

	public TptpXmlFileWriter(final FileWriter fw) {
		super(fw);
	}

	void writeLine(final String line) {
		try {
			super.out.write(line + '\n');
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void handleStart() {
		writeLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		writeLine("<TRACE>");
	}

	public void handleEnd() {
		writeLine("</TRACE>");
	}

	public void handleNode(final String id, final String host, final String ip, final int timezone,
			final long timeUTCSec, final long timeUTCNsec) {
		final String s = String.format(
				"<node nodeId=\"%s\" hostname=\"%s\" ipaddress=\"%s\" timezone=\"%d\" time=\"%d.%09d\"/>", id, host,
				ip, timezone, timeUTCSec, timeUTCNsec);
		writeLine(s);
	}

	public void handleProcessCreate(final String uuid, final int pid, final String nodeId, final long timeUTCSec,
			final long timeUTCNsec) {
		final String s = String.format(
				"<processCreate processId=\"%s\" pid=\"%d\" nodeIdRef=\"%s\" time=\"%d.%09d\"/>", uuid, pid, nodeId,
				timeUTCSec, timeUTCNsec);
		writeLine(s);
	}

	public void handleAgentCreate(final String uuid, final String version, final String puuid, final String name,
			final String type, final String parameters, final long timeUTCSec, final long timeUTCNsec) {
		final String s = String
				.format(
						"<agentCreate agentId=\"%s\" version=\"%s\" processIdRef=\"%s\" agentName=\"%s\" agentType=\"%s\" agentParameters=\"%s\" time=\"%d.%09d\"/>",
						uuid, version, puuid, name, type, parameters, timeUTCSec, timeUTCNsec);
		writeLine(s);
	}

	public void handleAgentDestroy(final String uuid, final long timeUTCSec, final long timeUTCNsec) {
		final String s = String.format("<agentDestroy agentIdRef=\"%s\" time=\"%d.%09d\"/>", uuid, timeUTCSec,
				timeUTCNsec);
		writeLine(s);
	}

	public void handleTraceStart(final String uuid, final String auuid, final long timeUTCSec, final long timeUTCNsec) {
		final String s = String.format("<traceStart traceId=\"%s\" agentIdRef=\"%s\" time=\"%d.%09d\"/>", uuid, auuid,
				timeUTCSec, timeUTCNsec);
		writeLine(s);
	}

	public void handleTraceEnd(final long timeUTCSec, final long timeUTCNsec) {
		final String s = String.format("<traceEnd time=\"%d.%09d\"/>", timeUTCSec, timeUTCNsec);
		writeLine(s);
	}

	public void handleFilter(final String pattern, final String mode, final String genericPattern,
			final String methodPattern, final String methodMode, final String methodGenericPattern) {
		final String s = String
				.format(
						"<filter pattern=\"%s\" mode=\"%s\" genericPattern=\"%s\" methodPattern=\"%s\" methodMode=\"%s\" methodGenericPattern=\"%s\"/>",
						pattern, mode, genericPattern, methodPattern, methodMode, methodGenericPattern);
		writeLine(s);
	}

	public void handleOption(final String key, final String value) {
		final String s = String.format("<option key=\"%s\" value=\"%s\"/>", key, value);
		writeLine(s);
	}

	public void handleRuntimeInitDone(final int tid, final long timeUTCSec, final long timeUTCNsec) {
		final String s = String.format("<runtimeInitDone threadIdRef=\"%d\" time=\"%d.%09d\"/>", tid, timeUTCSec,
				timeUTCNsec);
		writeLine(s);
	}

	public void handleRuntimeShutdown(final long timeUTCSec, final long timeUTCNSec) {
		final String s = String.format("<runtimeShutdown time=\"%d.%09d\"/>", timeUTCSec, timeUTCNSec);
		try {
			super.out.write(s);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	static String formatNum(final long num) {
		return String.format("%d", num);
	}

	static String formatTime(final long timeUTCSec, final long timeUTCNsec) {
		return String.format("%d.%09d", timeUTCSec, timeUTCNsec);
	}

	public void handleThreadStart(final int tid, final long timeUTCSec, final long timeUTCNsec, final String name,
			final String group, final String parent) {
		if (tid < 0 || timeUTCSec < 0 || timeUTCNsec < 0 || name == null)
			throw new IllegalArgumentException();

		final StringBuilder sb = new StringBuilder();
		sb.append("<threadStart threadId=\"").append(formatNum(tid)).append("\" time=\"").append(
				formatTime(timeUTCSec, timeUTCNsec)).append("\" threadName=\"").append(name).append("\"");
		if (group != null)
			sb.append(" groupName=\"").append(group).append("\"");
		if (parent != null)
			sb.append(" parentName=\"").append(parent).append("\"");
		sb.append("/>\n");
		try {
			super.out.write(sb.toString());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void handleClassDef(final int cid, final String name, final String sourceFile, final long timeUTCSec,
			final long timeUTCNsec) {
		if (cid < 0 || name == null || timeUTCSec < 0 || timeUTCNsec < 0)
			throw new IllegalArgumentException();

		final StringBuilder sb = new StringBuilder();
		sb.append("<classDef name=\"").append(name).append("\"");
		if (sourceFile != null)
			sb.append(" sourceName=\"").append(sourceFile).append("\"");
		sb.append(" classId=\"").append(formatNum(cid)).append("\" time=\"")
				.append(formatTime(timeUTCSec, timeUTCNsec)).append("\"/>\n");
		try {
			super.out.write(sb.toString());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void handleMethodDef(final long mid, final String name, final String signature, final int startLine,
			final int endLine, final int classId) {
		if (mid < 0 || name == null || signature == null || classId < 0)
			throw new IllegalArgumentException();

		final StringBuilder sb = new StringBuilder();
		sb.append("<methodDef name=\"").append(name).append("\" signature=\"").append(signature).append("\"");
		if (startLine >= 0)
			sb.append(" startLineNumber=\"").append(formatNum(startLine)).append("\"");
		if (endLine >= 0)
			sb.append(" endLineNumber=\"").append(formatNum(endLine)).append("\"");
		sb.append(" methodId=\"").append(formatNum(mid)).append("\" classIdRef=\"").append(formatNum(classId)).append(
				"\"/>\n");
		try {
			super.out.write(sb.toString());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void handleMethodEntry(final int tid, final int cid, final long mid, final long timeUTCSec,
			final long timeUTCNsec, final int ticket, final int stackDepth) {
		if (tid < 0 || cid < 0 || mid < 0 || timeUTCSec < 0 || timeUTCNsec < 0)
			throw new IllegalArgumentException();

		final StringBuilder sb = new StringBuilder();
		sb.append("<methodEntry threadIdRef=\"").append(formatNum(tid)).append("\" time=\"").append(
				formatTime(timeUTCSec, timeUTCNsec)).append("\" methodIdRef=\"").append(formatNum(mid)).append(
				"\" classIdRef=\"").append(formatNum(cid)).append("\"");
		if (ticket >= 0)
			sb.append(" ticket=\"").append(formatNum(ticket)).append("\"");
		if (stackDepth >= 0)
			sb.append(" stackDepth=\"").append(formatNum(stackDepth)).append("\"");
		sb.append("/>\n");
		try {
			super.out.write(sb.toString());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void handleMethodExit(final int tid, final int cid, final long mid, final long timeUTCSec,
			final long timeUTCNsec, final int ticket) {
		if (tid < 0 || cid < 0 || mid < 0 || timeUTCSec < 0 || timeUTCNsec < 0)
			throw new IllegalArgumentException();

		final StringBuilder sb = new StringBuilder();
		sb.append("<methodExit threadIdRef=\"").append(formatNum(tid)).append("\" methodIdRef=\"").append(
				formatNum(mid)).append("\" classIdRef=\"").append(formatNum(cid)).append("\"");
		if (ticket >= 0)
			sb.append(" ticket=\"").append(formatNum(ticket)).append("\"");
		sb.append(" time=\"").append(formatTime(timeUTCSec, timeUTCNsec)).append("\"/>\n");
		try {
			super.out.write(sb.toString());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void handleThreadEnd(final int tid, final long timeUTCSec, final long timeUTCNsec) {
		if (tid < 0 || timeUTCSec < 0 || timeUTCNsec < 0)
			throw new IllegalArgumentException();

		final String s = String
				.format("<threadEnd threadIdRef=\"%d\" time=\"%d.%09d\"/>", tid, timeUTCSec, timeUTCNsec);
		writeLine(s);
	}
}
