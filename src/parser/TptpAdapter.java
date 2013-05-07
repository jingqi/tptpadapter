package parser;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import parser.items.*;
import parser.record.ActionRecord.MethodAction;

/**
 * 将 Android Trace 记录适配到 TPTP Trace 记录
 *
 * @author jingqi
 */
public class TptpAdapter {

	private long startTimeUsec = -1;
	private long endTimeUsec = -1;
	private int nextMethodId = 1;
	private int nextClassId = 1;

	private final Map<Integer, ThreadItem> threads = new HashMap<Integer, ThreadItem>();
	private final Map<Integer, ClassItem> classesIds = new HashMap<Integer, ClassItem>();
	private final Map<String, ClassItem> classesNames = new HashMap<String, ClassItem>();
	private final Map<Long, MethodItem> methods = new HashMap<Long, MethodItem>();

	private final ITptpHandler tptpHandler;

	final String processUuid = "b83695a9-0ba4-4061-b4ad-f59fcebf0e43";
	final String agentUuid = "e08d817f-869c-40b9-b2c5-6796929ee60c";
	final String traceUuid = "6307309-4467-46c0-a44c-b513e3d652a7";

	private static final Logger logger = Logger.getLogger(TptpAdapter.class.getCanonicalName());

	public TptpAdapter(ITptpHandler handler) {
		tptpHandler = handler;
	}

	/**
	 * 将us(微秒)转换为s(秒)
	 */
	private static long usec2sec(long usec) {
		return usec / 1000000;
	}

	/**
	 * 将us(微秒)转换为ns(纳秒)
	 */
	private static long usec2nsec(long usec) {
		return (usec % 1000000) * 1000;
	}

	/**
	 * 将 android trace 中的类名替换为 tptp trace 中的类名称
	 */
	private static String androidName2TptpName(String className) {
		final StringBuilder sb = new StringBuilder(className.length());
		for (int i = 0; i < className.length(); ++i) {
			char c = className.charAt(i);
			if (c == '/')
				c = '.';
			sb.append(c);
		}
		return sb.toString();
	}

	public void addKeyFileVersion(int version) {
		logger.info("key file version : " + version);
		// nothing to do
	}

	public void addThread(int tid, String name) {
		logger.info("add thread : " + tid + " " + name);
		final ThreadItem ti = new ThreadItem(tid, name);
		threads.put(Integer.valueOf(tid), ti);
	}

	public void addMethod(long methodAddress, String className, String methodName, String signature,
			String sourceFile, int sourceLine) {
		className = androidName2TptpName(className);
		if (methodName.equals("<init>")) {
			methodName = "-init-";
		} else if (methodName.equals("<clinit>")) {
			methodName = "-clinit-";
		} else if (!Pattern.matches("[\\w$]+", methodName)) {
			throw new IllegalArgumentException("unknow method name : " + methodName);
		}
		ClassItem ci = classesNames.get(className);
		if (ci == null) {
			logger.info("add class : " + className);
			ci = new ClassItem(nextClassId++, className);
			classesNames.put(className, ci);
			classesIds.put(Integer.valueOf(ci.getClassId()), ci);
		}

		// logger.info("add method : " + methodName + " " + signature);
		final MethodItem mi = new MethodItem(nextMethodId++, methodAddress, methodName, signature, ci.getClassId());
		methods.put(Long.valueOf(methodAddress), mi);
	}

	/**
	 * @param offset
	 *            The application is expected to parse all of the header fields,
	 *            then seek to "offset to data" from the start of the file. From
	 *            there it just reads 9-byte records until EOF is reached.
	 *
	 * @param startDateTime
	 *            start date/time in usec is the output from gettimeofday().
	 *            It's mainly there so that you can tell if the output was
	 *            generated yesterday or three months ago.
	 */
	public void addDataFileHead(final int version, final int offset, final long startDateTime) {
		logger.info("data file version : " + version);
		startTimeUsec = startDateTime;
		endTimeUsec = startDateTime;

		// start
		tptpHandler.handleStart();
		tptpHandler.handleNode("", "localhost", "127.0.0.1", -480, usec2sec(startDateTime), usec2nsec(startDateTime));
		tptpHandler.handleProcessCreate(processUuid, 3812, "", usec2sec(startDateTime), usec2nsec(startDateTime));
		tptpHandler.handleAgentCreate(agentUuid, "2.000", processUuid, "org.eclipse.tptp.jvmti", "Profiler",
				"server=controlled", usec2sec(startDateTime), usec2nsec(startDateTime));
		tptpHandler.handleTraceStart(traceUuid, agentUuid, usec2sec(startDateTime), usec2nsec(startDateTime));

		// filters
		// tptpHandler.handleFilter("java$lang", "EXCLUDE", "PREFIX", "",
		// "EXCLUDE", "PREFIX");
		// tptpHandler.handleFilter("dalvik", "EXCLUDE", "PREFIX", "",
		// "EXCLUDE", "PREFIX");

		// options
		tptpHandler.handleOption("FILTERS", "false");
		tptpHandler.handleOption("OPTIONS", "true");
		// tptpHandler.handleOption("STACK_INFORMATION", "normal");
		tptpHandler.handleOption("TICKET", "true");
		tptpHandler.handleOption("TIMESTAMPS", "true");
		tptpHandler.handleOption("TRACE_IDREFS", "false");
		tptpHandler.handleOption("CPU_TIME", "true");
		tptpHandler.handleOption("org.eclipse.tptp.platform.jvmti.client.ATTR_EXEC_DATA", "true");
		tptpHandler.handleOption("org.eclipse.tptp.platform.jvmti.client.ATTR_EXEC_FLOW", "true");
		tptpHandler.handleOption("org.eclipse.tptp.platform.jvmti.client.ATTR_EXEC_AUTO_POLLING", "true");
		tptpHandler.handleOption("org.eclipse.tptp.platform.jvmti.client.ATTR_EXEC_MANUAL_POLLING", "false");
	}

	/**
	 * @param methodAction
	 *            method action sits in the two least-significant bits of the
	 *            method word. The currently defined meanings are: <br/>
	 *            0 - method entry <br/>
	 *            1 - method exit <br/>
	 *            2 - method "exited" when unrolled by exception handling <br/>
	 *            3 - (reserved)
	 *
	 * @param deltaTime
	 *            32-bit integer can hold about 70 minutes of time in
	 *            microseconds.
	 */
	public void addMethodAction(int threadId, long methodAddress, MethodAction methodAction,
			long deltaTime) {
		final long time = startTimeUsec + deltaTime;
		endTimeUsec = time;

		final ThreadItem ti = threads.get(Integer.valueOf(threadId));
		if (!ti.hasLogged()) {
			tptpHandler.handleThreadStart(threadId, usec2sec(time), usec2nsec(time), ti.getThreadName(), null, null);
			ti.setLogged(true);
		}

		final MethodItem mi = methods.get(Long.valueOf(methodAddress));
		final ClassItem ci = classesIds.get(Integer.valueOf(mi.getClassId()));
		if (!ci.hasLogged()) {
			tptpHandler.handleClassDef(ci.getClassId(), ci.getClassName(), null, usec2sec(time), usec2nsec(time));
			ci.setLogged(true);
		}

		if (!mi.hasLogged()) {
			tptpHandler.handleMethodDef(mi.getMethodId(), mi.getMethodName(), mi.getSignature(), -1, -1, mi
					.getClassId());
			mi.setLogged(true);
		}

		switch (methodAction) {
		case ENTRY:
			// logger.info("enter method : " + mi.getMethodName());
			ti.pushMethodCall(mi);
			tptpHandler.handleMethodEntry(threadId, ci.getClassId(), mi.getMethodId(), usec2sec(time),
					usec2nsec(time), ti.getTicketOfCurrentMethod(), ti.getDepthOfCallStack());
			break;

		case EXIT:
		case EXIT_EXCEPTION:
			if (0 == ti.getDepthOfCallStack()) {
				logger.warning("failed to exit method without calling record : " + mi.getMethodName());
				break;
			}

			// logger.info("exit method : " + mi.getMethodName());
			tptpHandler.handleMethodExit(threadId, ci.getClassId(), mi.getMethodId(), usec2sec(time),
					usec2nsec(time), ti.getTicketOfCurrentMethod());
			ti.popMethodCall(mi);
			break;

		default:
			throw new IllegalArgumentException("wrong type of method action");
		}
	}

	public void end() {
		tptpHandler.handleTraceEnd(usec2sec(endTimeUsec), usec2nsec(endTimeUsec));
		tptpHandler.handleAgentDestroy(agentUuid, usec2sec(endTimeUsec), usec2nsec(endTimeUsec));
		tptpHandler.handleEnd();
	}
}
