package org.teavm.classlib.java.sql;

/*
 * java.sql.Date shim for Via's shaded Gson. SqlTypesSupport loads the class
 * reflectively and SqlDateTypeAdapter calls the (long) constructor; Gson itself
 * never needs JDBC. The body is backed by the classlib's java.util.Date.
 */
public class TDate extends org.teavm.classlib.java.util.TDate {

	private static final long serialVersionUID = 6688390587573311555L;

	public TDate(long date) {
		super(date);
	}

	public static TDate valueOf(String date) {
		if (date == null) {
			throw new IllegalArgumentException("date string is null");
		}
		return new TDate(org.teavm.classlib.java.util.TDate.parse(date));
	}

	@Override
	public String toString() {
		int year = getYear() + 1900;
		int month = getMonth() + 1;
		int day = getDate();
		return pad(year, 4) + '-' + pad(month, 2) + '-' + pad(day, 2);
	}

	static String pad(int value, int width) {
		String text = Integer.toString(value);
		while (text.length() < width) {
			text = '0' + text;
		}
		return text;
	}

}
