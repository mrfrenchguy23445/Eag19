package org.teavm.classlib.java.sql;

/*
 * java.sql.Time shim for Via's shaded Gson. SqlTypesSupport loads it
 * reflectively and SqlTimeTypeAdapter calls the (long) constructor; Gson never
 * needs JDBC.
 */
public class TTime extends org.teavm.classlib.java.util.TDate {

	private static final long serialVersionUID = 9110747963329111458L;

	public TTime(long time) {
		super(time);
	}

	public static TTime valueOf(String time) {
		if (time == null) {
			throw new IllegalArgumentException("time string is null");
		}
		return new TTime(org.teavm.classlib.java.util.TDate.parse(time));
	}

	@Override
	public String toString() {
		return pad(getHours(), 2) + ':' + pad(getMinutes(), 2) + ':' + pad(getSeconds(), 2);
	}

	private static String pad(int value, int width) {
		String text = Integer.toString(value);
		while (text.length() < width) {
			text = '0' + text;
		}
		return text;
	}

}
