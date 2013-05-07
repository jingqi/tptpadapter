package parser.record;

/**
 * android trace 二进制记录
 *
 * @author jingqi
 */
public class ActionRecord {

	public static enum MethodAction {
		// method entry
		ENTRY,
		// method exit
		EXIT,
		// method "exited" when unrolled by exception handling
		EXIT_EXCEPTION,
		// (reserved)
		RESERVED,
	}

	// thread id
	private int threadId;
	// method id/address
	private long methodAddr;
	// method action
	private int methodAction;
	// time delta since start(in usec)
	private long deltaTime;

	public ActionRecord(byte[] buf) {
		this(buf, 0);
	}

	public ActionRecord(byte[] buf, int off) {
		threadId = (buf[off] & 0xFF) | ((buf[off + 1] & 0xFF) << 8);

		long methodAddress = (buf[off + 2] & 0xFFL) | ((buf[off + 3] & 0xFFL) << 8) |
				((buf[off + 4] & 0xFFL) << 16) | ((buf[off + 5] & 0xFFL) << 24);

		methodAction = (int) (methodAddress & 0x03);

		methodAddr = methodAddress & (~0x03L);

		deltaTime = (buf[off + 6] & 0xFFL) | ((buf[off + 7] & 0xFFL) << 8) |
				((buf[off + 8] & 0xFFL) << 16) | ((buf[off + 9] & 0xFFL) << 24);
	}

	public int getThreadId() {
		return threadId;
	}

	public long getMethodAddr() {
		return methodAddr;
	}

	public MethodAction getMethodAction() {
		switch (methodAction) {
		case 0:
			return MethodAction.ENTRY;

		case 1:
			return MethodAction.EXIT;

		case 2:
			return MethodAction.EXIT_EXCEPTION;

		case 3:
			return MethodAction.RESERVED;

		default:
			throw new RuntimeException();
		}
	}

	public long getDeltaTime() {
		return deltaTime;
	}

	public static int size() {
		return 9 + 5;
	}
}
