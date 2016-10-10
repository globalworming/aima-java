package aima.core.environment.map2d;

public class InState implements Comparable<InState> {
	private String location;

	public InState(String location) {
		this.location = location;
	}

	public String getLocation() {
		return location;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof InState) {
			return this.getLocation().equals(((InState) obj).getLocation());
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return getLocation().hashCode();
	}

	@Override
	public String toString() {
		return "In("+getLocation()+")";
	}


	@Override
	public int compareTo(InState o) {
		if (getLocation() == null) {
			return -1;
		}
		if (o.getLocation() == null) {
			return 1;
		}
		return getLocation().compareTo(o.getLocation());
	}
}
