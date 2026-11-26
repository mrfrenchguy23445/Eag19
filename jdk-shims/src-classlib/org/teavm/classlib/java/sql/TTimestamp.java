package org.teavm.classlib.java.sql;

/*
 * java.sql.Timestamp shim for Via's shaded Gson. SqlTimestampTypeAdapter only
 * calls the (long) constructor and treats the result as a java.util.Date.
 */
public class TTimestamp extends org.teavm.classlib.java.util.TDate {

	private static final long serialVersionUID = 7552975554117588091L;

	public TTimestamp(long time) {
		super(time);
	}

	public static TTimestamp valueOf(String time) {
		if (time == null) {
			throw new IllegalArgumentException("timestamp string is null");
		}
		return new TTimestamp(org.teavm.classlib.java.util.TDate.parse(time));
	}

	public int getNanos() {
		return 0;
	}

	public void setNanos(int nanos) {
		if (nanos < 0 || nanos > 999999999) {
			throw new IllegalArgumentException("nanos out of range");
		}
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof TTimestamp && getTime() == ((TTimestamp) other).getTime();
	}

	@Override
	public int hashCode() {
		long time = getTime();
		return (int) (time ^ (time >>> 32));
	}

	@Override
	public String toString() {
		int year = getYear() + 1900;
		int month = getMonth() + 1;
		int day = getDate();
		return TDate.pad(year, 4) + '-' + TDate.pad(month, 2) + '-' + TDate.pad(day, 2)
				+ " 00:00:00.0";
	}

}
