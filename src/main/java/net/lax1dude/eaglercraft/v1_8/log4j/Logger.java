/*
 * Copyright (c) 2022-2024 lax1dude. All Rights Reserved.
 *
 * This software is provided by the copyright holders and contributors "as is" and
 * any express or implied warranties, including, but not limited to, the implied
 * warranties of merchantability and fitness for a particular purpose are
 * disclaimed. In no event shall the copyright holder or contributors be liable
 * for any direct, indirect, incidental, special, exemplary, or consequential
 * damages (including, but not limited to, procurement of substitute goods or
 * services; loss of use, data, or profits; or business interruption) however
 * caused and on any theory of liability, whether in contract, strict liability,
 * or tort (including negligence or otherwise) arising in any way out of the use
 * of this software, even if advised of the possibility of such damage.
 *
 */

package net.lax1dude.eaglercraft.v1_8.log4j;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Eaglercraft's log4j-style logging shim.
 *
 * <p>Ported from the 1.8 tree. The original reached into {@code EagRuntime} for the thread
 * name and stack traces and into {@code PlatformRuntime} to forward browser JS exceptions.
 * Both of those are 1.8-only and are not part of the pruned 1.19.4 lax1dude closure, so
 * this copy uses {@link Thread#getName()} / {@link Throwable#printStackTrace} instead and
 * keeps the exact public API the rest of the port expects.
 */
public class Logger {

	public final String loggerName;

	Logger(String name) {
		this.loggerName = name;
	}

	public void trace(String msg) {
		log(Level.TRACE, msg);
	}

	public void trace(String msg, Object... args) {
		log(Level.TRACE, msg, args);
	}

	public void trace(Throwable msg) {
		log(Level.WARN, msg);
	}

	public void debug(String msg) {
		log(Level.DEBUG, msg);
	}

	public void debug(String msg, Object... args) {
		log(Level.DEBUG, msg, args);
	}

	public void debug(Throwable msg) {
		log(Level.DEBUG, msg);
	}

	public void info(String msg) {
		log(Level.INFO, msg);
	}

	public void info(String msg, Object... args) {
		log(Level.INFO, msg, args);
	}

	public void info(Throwable msg) {
		log(Level.INFO, msg);
	}

	public void warn(String msg) {
		log(Level.WARN, msg);
	}

	public void warn(String msg, Object... args) {
		log(Level.WARN, msg, args);
	}

	public void warn(Throwable msg) {
		log(Level.WARN, msg);
	}

	public void error(String msg) {
		log(Level.ERROR, msg);
	}

	public void error(String msg, Object... args) {
		log(Level.ERROR, msg, args);
	}

	public void error(Throwable msg) {
		log(Level.ERROR, msg);
	}

	public void fatal(String msg) {
		log(Level.FATAL, msg);
	}

	public void fatal(String msg, Object... args) {
		log(Level.FATAL, msg, args);
	}

	public void fatal(Throwable msg) {
		log(Level.FATAL, msg);
	}

	private static final SimpleDateFormat fmt = new SimpleDateFormat("hh:mm:ss+SSS");
	private static final Date dateInstance = new Date();

	private String prefix(Level level) {
		dateInstance.setTime(System.currentTimeMillis());
		return "[" + fmt.format(dateInstance) + "]" +
			"[" + Thread.currentThread().getName() + "/" + level.levelName + "]" +
			"[" + loggerName + "]: ";
	}

	public void log(Level level, String msg) {
		if (level.levelInt >= LogManager.logLevel.levelInt) {
			synchronized (LogManager.logLock) {
				String line = prefix(level) + msg;
				level.getPrintStream().println(line);
				if (LogManager.logRedirector != null) {
					LogManager.logRedirector.log(line, level.isErr);
				}
			}
		}
	}

	public void log(Level level, String msg, Object... args) {
		if (level.levelInt >= LogManager.logLevel.levelInt) {
			synchronized (LogManager.logLock) {
				String line = prefix(level) + formatParams(msg, args);
				level.getPrintStream().println(line);
				if (LogManager.logRedirector != null) {
					LogManager.logRedirector.log(line, level.isErr);
				}
			}
		}
	}

	public static String formatParams(String msg, Object... args) {
		if (args.length > 0) {
			StringBuilder builtString = new StringBuilder();
			for (int i = 0; i < args.length; ++i) {
				int idx = msg.indexOf("{}");
				if (idx != -1) {
					builtString.append(msg, 0, idx);
					builtString.append(args[i]);
					msg = msg.substring(idx + 2);
				} else {
					break;
				}
			}
			builtString.append(msg);
			return builtString.toString();
		} else {
			return msg;
		}
	}

	public void log(Level level, Throwable msg) {
		logExcp(level, "Exception Thrown", msg);
	}

	private void logExcp(final Level level, String h, Throwable msg) {
		if (msg == null) {
			log(level, "{}: <null>", h);
			return;
		}
		log(level, "{}: {}", h, msg.toString());
		// 1.8 used EagRuntime.getStackTrace(msg, e -> log(level, "    at {}", e))
		msg.printStackTrace(level.getPrintStream());
		Throwable cause = msg.getCause();
		if (cause != null) {
			logExcp(level, "Caused By", cause);
		}
	}

	public boolean isDebugEnabled() {
		return LogManager.logLevel.levelInt <= Level.DEBUG.levelInt;
	}

}
