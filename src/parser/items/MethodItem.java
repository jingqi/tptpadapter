package parser.items;

public class MethodItem {

	private final int methodId;
	private final long address;
	private final int classId;
	private final String name;
	private final String signature;

	private boolean logged = false;

	public MethodItem(final int id, final long address, final String name, final String signature, final int classid) {
		methodId = id;
		this.address = address;
		this.name = name;
		this.signature = signature;
		this.classId = classid;
	}

	public int getMethodId() {
		return methodId;
	}

	public String getMethodName() {
		return name;
	}

	public int getClassId() {
		return classId;
	}

	public String getSignature() {
		return signature;
	}

	public boolean hasLogged() {
		return logged;
	}

	public void setLogged(final boolean logged) {
		this.logged = logged;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Method : ");
		sb.append(name).append(' ').append(signature).append('\n');
		return sb.toString();
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof MethodItem))
			return false;
		final MethodItem m = (MethodItem) o;
		return methodId == m.methodId && address == m.address && name.equals(m.name) && signature.equals(m.signature);
	}

	@Override
	public int hashCode() {
		int ret = methodId * 17;
		ret = (int) (ret * 17 + address);
		ret = ret * 17 + name.hashCode();
		ret = ret * 17 + signature.hashCode();
		return ret;
	}
}
