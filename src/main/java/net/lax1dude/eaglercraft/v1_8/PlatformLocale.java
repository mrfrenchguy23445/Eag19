/*
 * Copyright (c) 2025 lax1dude. All Rights Reserved.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */

package net.lax1dude.eaglercraft.v1_8;

import java.util.Locale;
import java.util.MissingResourceException;

/**
 * ISO 3166-1 alpha-3 country codes, which the WASM-GC TeaVM classlib cannot derive.
 *
 * <p>This exists for one caller: {@code Minecraft.countryEqualsISO3}, which reads
 * {@code Locale.getDefault().getISO3Country()} to decide whether the player is in one of the
 * regions Mojang's {@code regional_compliancies.json} has a notification for. The JSON is keyed
 * by alpha-3 code, so without it every notification is silently filtered out.
 *
 * <p>The method it replaces cannot be shimmed. {@code java.util.Locale} is a class the classlib
 * owns, so there is nowhere to add {@code getISO3Country()} to, and the JDK implements it by
 * consulting an internal country table that has no classlib equivalent. The table below is that
 * table: 249 entries, the full set of alpha-2 codes ISO 3166-1 assigns, paired with the alpha-3
 * code the JDK returns for each.
 *
 * <p>It is generated, not typed. It was produced by running, on this project's JDK 17:
 *
 * <pre>
 * for (String iso2 : new String[]{Locale.getISOCountries()} sorted) {
 *     new Locale("", iso2).getISO3Country();
 * }
 * </pre>
 *
 * <p>so every value here is by construction what the method would have returned on the same JDK.
 * All 249 codes resolve -- none had to be dropped as unassigned -- and the alpha-2 half is
 * sorted, which is what makes the scan below a plain linear search.
 */
public final class PlatformLocale {

    private PlatformLocale() {}

    private static final int COUNT = 249;

    /**
     * The 249 ISO 3166-1 alpha-2 codes, concatenated, in ascending order.
     */
    private static final String ALPHA2 =
      ""
+ "ADAEAFAGAIALAMAOAQARASATAUAWAXAZBABBBDBEBFBGBHBIBJBLBMBNBOBQBRBS"
        + "BTBVBWBYBZCACCCDCFCGCHCICKCLCMCNCOCRCUCVCWCXCYCZDEDJDKDMDODZECEE"
        + "EGEHERESETFIFJFKFMFOFRGAGBGDGEGFGGGHGIGLGMGNGPGQGRGSGTGUGWGYHKHM"
        + "HNHRHTHUIDIEILIMINIOIQIRISITJEJMJOJPKEKGKHKIKMKNKPKRKWKYKZLALBLC"
        + "LILKLRLSLTLULVLYMAMCMDMEMFMGMHMKMLMMMNMOMPMQMRMSMTMUMVMWMXMYMZNA"
        + "NCNENFNGNINLNONPNRNUNZOMPAPEPFPGPHPKPLPMPNPRPSPTPWPYQARERORSRURW"
        + "SASBSCSDSESGSHSISJSKSLSMSNSOSRSSSTSVSXSYSZTCTDTFTGTHTJTKTLTMTNTO"
        + "TRTTTVTWTZUAUGUMUSUYUZVAVCVEVGVIVNVUWFWSYEYTZAZMZW";

    /**
     * The alpha-3 code for each entry of {@link #ALPHA2}, in the same order, three characters
     * per entry.
     */
    private static final String ALPHA3 =
      ""
+ "ANDAREAFGATGAIAALBARMAGOATAARGASMAUTAUSABWALAAZEBIHBRBBGDBELBFAB"
        + "GRBHRBDIBENBLMBMUBRNBOLBESBRABHSBTNBVTBWABLRBLZCANCCKCODCAFCOGCH"
        + "ECIVCOKCHLCMRCHNCOLCRICUBCPVCUWCXRCYPCZEDEUDJIDNKDMADOMDZAECUEST"
        + "EGYESHERIESPETHFINFJIFLKFSMFROFRAGABGBRGRDGEOGUFGGYGHAGIBGRLGMBG"
        + "INGLPGNQGRCSGSGTMGUMGNBGUYHKGHMDHNDHRVHTIHUNIDNIRLISRIMNINDIOTIR"
        + "QIRNISLITAJEYJAMJORJPNKENKGZKHMKIRCOMKNAPRKKORKWTCYMKAZLAOLBNLCA"
        + "LIELKALBRLSOLTULUXLVALBYMARMCOMDAMNEMAFMDGMHLMKDMLIMMRMNGMACMNPM"
        + "TQMRTMSRMLTMUSMDVMWIMEXMYSMOZNAMNCLNERNFKNGANICNLDNORNPLNRUNIUNZ"
        + "LOMNPANPERPYFPNGPHLPAKPOLSPMPCNPRIPSEPRTPLWPRYQATREUROUSRBRUSRWA"
        + "SAUSLBSYCSDNSWESGPSHNSVNSJMSVKSLESMRSENSOMSURSSDSTPSLVSXMSYRSWZT"
        + "CATCDATFTGOTHATJKTKLTLSTKMTUNTONTURTTOTUVTWNTZAUKRUGAUMIUSAURYUZ"
        + "BVATVCTVENVGBVIRVNMVUTWLFWSMYEMMYTZAFZMBZWE";

    /**
     * {@code locale.getISO3Country()}, for the locales the classlib will produce.
     *
     * <p>The two failure modes are the JDK's, deliberately, because the one caller catches
     * {@link MissingResourceException} to mean "this locale has no country, so no
     * notifications": an unset country throws with the JDK's {@code "empty country"} message,
     * and a country code that is not in the table throws with the code itself as both message
     * and key.
     */
    public static String iso3Country(Locale locale) {
        String country = locale.getCountry();

        if (country.length() != 2) {
            throw new MissingResourceException(country.isEmpty() ? "empty country" : country, "Locale", country);
        }

        for (int i = 0; i < COUNT; ++i) {
            int at = i << 1;

            if (ALPHA2.charAt(at) == country.charAt(0) && ALPHA2.charAt(at + 1) == country.charAt(1)) {
                int from = i * 3;
                return ALPHA3.substring(from, from + 3);
            }
        }

        throw new MissingResourceException(country, "Locale", country);
    }
}
