/*
 * Copyright (c) 2025 lax1dude. All Rights Reserved.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */

package net.lax1dude.eaglercraft.v1_8;

import jdk_internal.bidi.Bidi;

public class EaglerBidiReorder {

    /**
     * The runs of {@code par0} in <em>visual</em> order.
     *
     * <p>Each returned entry is {@code {start, limit, level}}: the run's logical bounds in the
     * text {@code par0} was built from, and its embedding level. Odd level means
     * right-to-left. The bounds are logical even though the order is visual, so a caller
     * slicing the original text by them gets the right characters in the right order without
     * having to index backwards.
     *
     * <p>This exists because the JDK's {@code Bidi} reports its runs in logical order
     * ({@code getRunCount}/{@code getRunStart}/{@code getRunLimit}/{@code getRunLevel})
     * while ICU4J's offered {@code countRuns()}/{@code getVisualRun(int)}. Minecraft 1.19.4
     * uses the ICU4J spelling, and {@code Font.bidirectionalShaping} and
     * {@code FormattedBidiReorder} both need runs in visual order. Converting is a
     * {@link Bidi#reorderVisually} call over an array of run indices, which is the same step
     * {@link #bidiReorder(String)} has always done inline.
     *
     * <p>The levels array is permuted by the same call, so {@code level} here is the level of
     * the run now sitting at that visual position -- not the level of logical run
     * {@code i}.
     *
     * @param par0 the bidi analysis to take runs from
     * @return one {@code {start, limit, level}} triple per run, in visual order
     */
    public static int[][] visualRuns(Bidi par0) {
        int var1 = par0.getRunCount();
        // PORT NOTE (Eag19 / TeaVM WASM-GC): an empty paragraph has zero runs. Vanilla's
        // ICU4J spelling (countRuns()/getVisualRun()) simply iterates zero times, so an
        // empty line -- which StringSplitter deliberately emits as FormattedText.EMPTY
        // when a break lands on '\n', e.g. the blank line inside the
        // "accessibility.onboarding.screen.title" translation -- comes back as a blank
        // FormattedCharSequence. The JDK's BidiBase.reorderVisually() rejects an empty
        // levels array ("Value levelStart 0 is out of range 0 to -1"), so the zero-run
        // case must short-circuit here to reproduce the vanilla result.
        if (var1 <= 0) {
            return new int[0][];
        }
        byte[] var2 = new byte[var1];
        Object[] var3 = new Object[var1];

        for (int var4 = 0; var4 < var1; ++var4) {
            var2[var4] = (byte) par0.getRunLevel(var4);
            var3[var4] = Integer.valueOf(var4);
        }

        Bidi.reorderVisually(var2, 0, var3, 0, var1);
        int[][] var5 = new int[var1][];

        for (int var6 = 0; var6 < var1; ++var6) {
            int var7 = ((Integer) var3[var6]).intValue();
            var5[var6] = new int[]{par0.getRunStart(var7), par0.getRunLimit(var7), var2[var6]};
        }

        return var5;
    }

    /**
     * Taken from minecraft 1.6
     */
    public static String bidiReorder(String par1Str) {
        if (par1Str != null && Bidi.requiresBidi(par1Str.toCharArray(), 0, par1Str.length())) {
            Bidi bidi = new Bidi(par1Str, -2);
            byte[] abyte = new byte[bidi.getRunCount()];
            String[] astring = new String[abyte.length];
            int i;

            for (int j = 0; j < abyte.length; ++j) {
                int k = bidi.getRunStart(j);
                i = bidi.getRunLimit(j);
                int l = bidi.getRunLevel(j);
                String s1 = par1Str.substring(k, i);
                abyte[j] = (byte) l;
                astring[j] = s1;
            }

            String[] astring1 = (String[]) astring.clone();
            Bidi.reorderVisually(abyte, 0, astring, 0, abyte.length);
            StringBuilder stringbuilder = new StringBuilder();
            i = 0;

            while (i < astring.length) {
                byte b0 = abyte[i];
                int i1 = 0;

                while (true) {
                    if (i1 < astring1.length) {
                        if (!astring1[i1].equals(astring[i])) {
                            ++i1;
                            continue;
                        }

                        b0 = abyte[i1];
                    }

                    if ((b0 & 1) == 0) {
                        stringbuilder.append(astring[i]);
                    } else {
                        for (i1 = astring[i].length() - 1; i1 >= 0; --i1) {
                            char c0 = astring[i].charAt(i1);

                            if (c0 == 40) {
                                c0 = 41;
                            } else if (c0 == 41) {
                                c0 = 40;
                            }

                            stringbuilder.append(c0);
                        }
                    }

                    ++i;
                    break;
                }
            }

            return stringbuilder.toString();
        } else {
            return par1Str;
        }
    }

}