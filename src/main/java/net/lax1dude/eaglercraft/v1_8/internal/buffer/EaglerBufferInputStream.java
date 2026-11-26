/*
 * Copyright (c) 2022 lax1dude. All Rights Reserved.
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

package net.lax1dude.eaglercraft.v1_8.internal.buffer;

import java.io.IOException;
import java.io.InputStream;

public class EaglerBufferInputStream extends InputStream {

	private final ByteBuffer buffer;

	public EaglerBufferInputStream(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public int read() throws IOException {
		if(buffer.remaining() <= 0) {
			return -1;
		}
		return (int)buffer.get() & 0xFF;
	}

	@Override
	public int read(byte b[], int off, int len) throws IOException {
		int p = buffer.position();
		int l = buffer.limit();
		int r = l - p;
		if(r < len) {
			len = r;
		}
		if(len > 0) {
			buffer.get(b, off, len);
		}
		return len;
	}

	@Override
	public long skip(long n) throws IOException {
		int p = buffer.position();
		int l = buffer.limit();
		int r = l - p;
		if(r < n) {
			n = r;
		}
		if(n > 0) {
			buffer.position(p + (int)n);
		}
		return n;
	}

	@Override
	public int available() throws IOException {
		return buffer.remaining();
	}

}
