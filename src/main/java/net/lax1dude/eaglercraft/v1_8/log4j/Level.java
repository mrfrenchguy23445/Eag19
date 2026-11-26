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

import java.io.PrintStream;

public enum Level {

	TRACE(0, "TRACE", false), DEBUG(1, "DEBUG", false), INFO(2, "INFO", false),
	WARN(3, "WARN", false), ERROR(4, "ERROR", true), FATAL(5, "FATAL", true),
	OFF(Integer.MAX_VALUE, "DISABLED", false);

	public final int levelInt;
	public final String levelName;
	public final PrintStream stdout;
	public final boolean isErr;

	private Level(int levelInt, String levelName, boolean stderr) {
		this.levelInt = levelInt;
		this.levelName = levelName;
		this.stdout = stderr ? System.err : System.out;
		this.isErr = stderr;
	}

	PrintStream getPrintStream() {
		return stdout;
	}

}
