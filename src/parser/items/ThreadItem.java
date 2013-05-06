package parser.items;

import java.util.LinkedList;

public class ThreadItem {

	private final int tid;
	private final String name;

	/** 标志位，标记是否已经记录到日志中 */
	private boolean logged = false;

	/** 函数调用计数 */
	private int nextTicket = 0;

	private final LinkedList<MethodItem> callStack = new LinkedList<MethodItem>();
	private final LinkedList<Integer> callStackTickets = new LinkedList<Integer>();

	public ThreadItem(final int tid, final String name) {
		this.tid = tid;
		this.name = name;
	}

	public String getThreadName() {
		return name;
	}

	public boolean hasLogged() {
		return logged;
	}

	public void setLogged(final boolean logged) {
		this.logged = logged;
	}

	/** 函数压栈 */
	public void pushMethodCall(final MethodItem m) {
		callStack.addLast(m);
		callStackTickets.addLast(Integer.valueOf(nextTicket++));
	}

	/** 函数出栈 */
	public void popMethodCall(final MethodItem m) {
		final MethodItem poped = callStack.removeLast();
		callStackTickets.removeLast();
		if (m != poped)
			throw new IllegalArgumentException("calling stack with wrong state");
	}

	/** 调用栈深度 */
	public int getDepthOfCallStack() {
		return callStack.size();
	}

	public int getTicketOfCurrentMethod() {
		return callStackTickets.getLast();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("thread : ");
		sb.append(tid).append(' ').append(name).append('\n');
		return sb.toString();
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ThreadItem))
			return false;
		final ThreadItem t = (ThreadItem) o;
		return tid == t.tid && name.equals(t.name);
	}

	@Override
	public int hashCode() {
		return tid * 17 + name.hashCode();
	}
}
