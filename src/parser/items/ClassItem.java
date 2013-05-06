package parser.items;

public class ClassItem {
	private final int classId;
	private final String name;

	private boolean logged = false;

	public ClassItem(final int id, final String name) {
		classId = id;
		this.name = name;
	}

	public String getClassName() {
		return name;
	}

	public int getClassId() {
		return classId;
	}

	public boolean hasLogged() {
		return logged;
	}

	public void setLogged(final boolean logged) {
		this.logged = logged;
	}

	@Override
	public String toString() {
		return "Class : " + name;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ClassItem))
			return false;

		final ClassItem c = (ClassItem) o;
		return classId == c.classId && name.equals(c.name);
	}

	@Override
	public int hashCode() {
		return classId * 17 + name.hashCode();
	}
}
