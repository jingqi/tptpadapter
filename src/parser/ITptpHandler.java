package parser;

/**
 * 处理 TPTP Trace 记录
 */
public interface ITptpHandler {

	void handleStart();

	void handleEnd();

	void handleNode(final String id, final String host, final String ip, final int timezone, final long timeUTCSec,
			final long timeUTCNsec);

	void handleProcessCreate(final String uuid, final int pid, final String nodeId, final long timeUTCSec,
			final long timeUTCNsec);

	void handleAgentCreate(final String uuid, final String version, final String puuid, final String name,
			final String type, final String parameters, final long timeUTCSec, final long timeUTCNsec);

	void handleAgentDestroy(final String uuid, final long timeUTCSec, final long timeUTCNsec);

	void handleTraceStart(final String uuid, final String auuid, final long timeUTCSec, final long timeUTCNsec);

	void handleRuntimeInitDone(final int tid, final long timeUTCSec, final long timeUTCNsec);

	void handleRuntimeShutdown(final long timeUTCSec, final long timeUTCNSec);

	void handleFilter(final String pattern, final String mode, final String genericPattern, final String methodPattern,
			final String methodMode, final String methodGenericPattern);

	void handleOption(final String key, final String value);

	/**
	 * 线程启动
	 * 
	 * @param timeUTCSec
	 *            时钟(1970-1-1 00:00:00 coordinated universal time) 单位 s
	 * @param timeUTCNsec
	 *            时钟(1970-1-1 00:00:00 coordinated universal time) 单位 ns
	 */
	void handleThreadStart(final int tid, final long timeUTCSec, final long timeUTCNsec, final String name,
			final String group, final String parent);

	void handleTraceEnd(final long timeUTCSec, final long timeUTCNsec);

	/**
	 * 线程终止
	 * 
	 * @param timeUTCSec
	 *            时钟(1970-1-1 00:00:00 coordinated universal time) 单位 s
	 * @param timeUTCNsec
	 *            时钟(1970-1-1 00:00:00 coordinated universal time) 单位 ns
	 */
	void handleThreadEnd(final int tid, final long timeUTCSec, final long timeUTCNsec);

	/**
	 * 类加载
	 * 
	 * @param timeUTCSec
	 *            时钟(1970-1-1 00:00:00 coordinated universal time) 单位 s
	 * @param timeUTCNsec
	 *            时钟(1970-1-1 00:00:00 coordinated universal time) 单位 ns
	 */
	void handleClassDef(final int cid, final String name, final String sourceFile, final long timeUTCSec,
			final long timeUTCNsec);

	/** 方法加载 */
	void handleMethodDef(final long mid, final String name, final String signature, final int startLine,
			final int endLine, final int classId);

	/**
	 * 方法执行
	 * 
	 * @param timeUTCSec
	 *            时钟(1970-1-1 00:00:00 coordinated universal time) 单位 s
	 * @param timeUTCNsec
	 *            时钟(1970-1-1 00:00:00 coordinated universal time) 单位 ns
	 */
	void handleMethodEntry(final int tid, final int cid, final long mid, final long timeUTCSec, final long timeUTCNsec,
			final int ticket, final int stackDepth);

	/**
	 * 方法退出
	 * 
	 * @param timeUTCSec
	 *            时钟(1970-1-1 00:00:00 coordinated universal time) 单位 s
	 * @param timeUTCNsec
	 *            时钟(1970-1-1 00:00:00 coordinated universal time) 单位 ns
	 */
	void handleMethodExit(final int tid, final int cid, final long mid, final long timeUTCSec, final long timeUTCNsec,
			final int ticket);

}
