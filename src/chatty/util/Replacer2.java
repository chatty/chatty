
package chatty.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Provide a String list with what to replace:
 * [target1] [item1a] [item1b]
 * [target2] [item2a] [item2b] [item2c]
 * ...
 * 
 * The items will be searched for and replaces with their respective target. All
 * items must be shorter or equal in length to target for the index convertion
 * to work in all cases (other items are ignored).
 * 
 * This provides index correction, where the indices of the changed String can
 * be converted to the equivalent indices of the original String, via the Result
 * object's methods. So for example if "abcd" is turned into "acd" then the
 * index 3 ("d" in changed String) can be converted to 4 ("d" in original
 * String).
 * 
 * Replacement is done in two steps, first any items that represent single
 * codepoints are replaced in an optimized way, then any other items are
 * replaced using regex. This is mostly optimized for replacing a lot of single
 * characters (codepoints). Using regex for all single characters in the
 * LOOKALIKES preset (over 1000) appeared to be considerably slower than the
 * current method.
 *
 * @author tduva
 */
public class Replacer2 {

    //==========================
    // Create Replacer
    //==========================
    
    public static Replacer2 create(Collection<String> input) {
        StringBuilder wordsRegexBuilder = new StringBuilder();
        Map<Integer, String> charsMapping = new HashMap<>();
        Map<String, String> wordsMapping = new HashMap<>();
        for (String line : input) {
            List<Part> split = parse(line);
            if (split.size() > 1) {
                String target = split.get(0).text;
                List<Part> searchList = split.subList(1, split.size());
                Set<String> words = new LinkedHashSet<>();
                //--------------------------
                // Separate chars/words
                //--------------------------
                for (Part partItem : searchList) {
                    if (!partItem.valid) {
                        // Currently only checked if target <= part
                        continue;
                    }
                    if (partItem.text.equals(target)) {
                        continue;
                    }
                    String part = partItem.text;
                    if (singleCodepoint(part)) {
                        charsMapping.put(part.codePointAt(0), target);
                        /**
                         * Add uppercase/lowercase variant as well, since char
                         * replacing directly looks up codepoints from the map.
                         */
                        // Uppercase may rarely turn into two characters?
                        addAdditionalChar(charsMapping, target, part, part.toUpperCase(Locale.ROOT));
                        addAdditionalChar(charsMapping, target, part, part.toLowerCase(Locale.ROOT));
                    }
                    else if (!part.isEmpty()) {
                        // Built into a regex, so quote just in case
                        words.add(Pattern.quote(part));
                        // Lookup should be normalized to lowercase
                        wordsMapping.put(part.toLowerCase(Locale.ROOT), target);
                    }
                }
                //--------------------------
                // Words regex
                //--------------------------
                String wordsRegex = StringUtil.join(words, "|");
                if (!wordsRegex.isEmpty()) {
                    if (wordsRegexBuilder.length() != 0) {
                        wordsRegexBuilder.append("|");
                    }
                    wordsRegexBuilder.append("(").append(wordsRegex).append(")");
                }
            }
        }
        Debugging.println("substRegex", "SubstRegex: %s", wordsRegexBuilder.toString());
        Pattern wordsPattern = Pattern.compile(wordsRegexBuilder.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        return new Replacer2(wordsPattern, wordsMapping, charsMapping);
    }
    
    private static void addAdditionalChar(Map<Integer, String> map, String target, String source, String additional) {
        if (validChar(target, additional)) {
            int codepoint = additional.codePointAt(0);
            if (!map.containsKey(codepoint) && checkName(codepoint, source.codePointAt(0))) {
                map.put(codepoint, target);
            }
        }
    }
    
    private static final Pattern NAME_CHECK = Pattern.compile("(CAPITAL|SMALL) ");
    
    /**
     * Checks that both codepoints share the same name, except for "CAPITAL" or
     * "SMALL". If no name can be retrieved for either one, the check fails.
     * 
     * @param codepointA
     * @param codepointB
     * @return 
     */
    private static boolean checkName(int codepointA, int codepointB) {
        String a = Character.getName(codepointA);
        String b = Character.getName(codepointB);
        if (a == null || b == null) {
            return false;
        }
        a = NAME_CHECK.matcher(a).replaceAll("");
        b = NAME_CHECK.matcher(b).replaceAll("");
        return a.equals(b);
    }
    
    private static boolean singleCodepoint(String input) {
        return input.codePointCount(0, input.length()) == 1;
    }
    
    private static boolean valid(String target, String part) {
        return target.length() <= part.length();
    }
    
    private static boolean validChar(String target, String part) {
        return valid(target, part) && singleCodepoint(part);
    }
    
    public static List<Part> parse(String input) {
        List<Part> result = new ArrayList<>();
        String[] split = input.split(" ");
        boolean combine = false;
        String target = null;
        for (String part : split) {
            if (result.isEmpty()) {
                target = part;
                result.add(new Part(part, false, true));
            }
            else if (part.equals("#")) {
                combine = !combine;
            }
            else if (combine && part.codePointCount(0, part.length()) == 1) {
                String character = part;
                result.add(new Part(part, false, valid(target, part)));
                for (String generated : generateDiacritics(character)) {
                    result.add(new Part(generated, true, valid(target, part)));
                }
            }
            else {
                result.add(new Part(part, false, valid(target, part)));
            }
        }
        return result;
    }
    
    public static class Part {
        
        public final String text;
        public final boolean autoGenerated;
        public final boolean valid;

        public Part(String text, boolean autoGenerated, boolean invalid) {
            this.text = text;
            this.autoGenerated = autoGenerated;
            this.valid = invalid;
        }
        
    }
    
    //==========================
    // Replacer
    //==========================
    
    private final Pattern wordsPattern;
    private final Map<String, String> wordsMapping;
    private final Map<Integer, String> charsMapping;

    private Replacer2(Pattern wordsPattern, Map<String, String> wordsMapping, Map<Integer, String> charsMapping) {
        this.wordsPattern = wordsPattern;
        this.wordsMapping = wordsMapping;
        this.charsMapping = charsMapping;
    }

    @Override
    public String toString() {
        return String.format("%s", wordsPattern);
    }

    public Result replace(String message) {
        Result charsResult = replaceChars(message);
        if (charsResult != null) {
            message = charsResult.changedText;
        }
        Result wordsResult = replaceWords(message);

        // Handle results
        if (charsResult == null) {
            return wordsResult;
        }
        else if (wordsResult == null) {
            return charsResult;
        }
        //--------------------------
        // Combine offsets
        //--------------------------
        Map<Integer, Integer> offsets = new TreeMap<>();
        /**
         * Adjust indices of the chars replacement step based on what text was
         * removed by the words replacement step.
         */
        for (Map.Entry<Integer, Integer> entry : charsResult.offsets.entrySet()) {
            int index = entry.getKey();
            int offset = entry.getValue();
            int prevOffsets = 0;
            int minIndex = 0;
            for (Map.Entry<Integer, Integer> entry2 : wordsResult.offsets.entrySet()) {
                int index2 = entry2.getKey();
                int offset2 = entry2.getValue();
                int origStart = index2 + prevOffsets;
                if (origStart < index) {
                    prevOffsets += offset2;
                    minIndex = index2;
                }
                else {
                    break;
                }
            }
            index = Math.max(minIndex, index - prevOffsets);
            offsets.put(index, offset);
        }
        /**
         * The indices of the words replacement step are already correct, since
         * they are based on text that will be returned, so they just need to be
         * added as well. If an index already exists from the chars step, then
         * add the offsets together.
         */
        for (Map.Entry<Integer, Integer> entry2 : wordsResult.offsets.entrySet()) {
            int index2 = entry2.getKey();
            int offset2 = entry2.getValue();
            if (offsets.containsKey(index2)) {
                offset2 += offsets.get(index2);
            }
            offsets.put(index2, offset2);
        }
        return new Result(wordsResult.changedText, offsets);
    }

    public Result replaceChars(String message) {
        if (charsMapping.isEmpty()) {
            return null;
        }
        Map<Integer, Integer> offsets = null;
        IntStream codePoints = message.codePoints();
        StringBuilder b = new StringBuilder();
        int index = 0;
        for (int codePoint : codePoints.toArray()) {
            int charCount = Character.charCount(codePoint);
            String target = charsMapping.get(codePoint);
            if (target != null) {
                b.append(target);
                int lengthDiff = charCount - target.length();
                if (lengthDiff != 0) {
                    if (offsets == null) {
                        offsets = new TreeMap<>();
                    }
                    updateOffsets(offsets, index + target.length() - 1, lengthDiff);
                }
            }
            else {
                b.appendCodePoint(codePoint);
            }
            index += charCount;
        }
        return new Result(b.toString(), offsets);
    }

    /**
     * Replace words, which in this case means anything longer than a single
     * codepoint. This uses regex to search and replace in one pass.
     * 
     * @param message
     * @return 
     */
    public Result replaceWords(String message) {
        if (wordsMapping.isEmpty()) {
            return null;
        }
        Map<Integer, Integer> offsets = null;
        Matcher m = wordsPattern.matcher(message);
        StringBuilder b = null;
        int lastAppendPos = 0;
        while (m.find()) {
            if (b == null) {
                b = new StringBuilder();
            }
            b.append(message, lastAppendPos, m.start());
            // Normalize lookup to lowercase for case-insensitivity
            String base = wordsMapping.get(m.group().toLowerCase(Locale.ROOT));
            if (base == null) {
                return null;
            }
            int lengthDiff = m.group().length() - base.length();
            if (lengthDiff != 0) {
                if (offsets == null) {
                    offsets = new TreeMap<>();
                }
                updateOffsets(offsets, m.start() + base.length() - 1, lengthDiff);
            }
            b.append(base);
            lastAppendPos = m.end();
        }
        if (b == null) {
            return null;
        }
        b.append(message, lastAppendPos, message.length());
        return new Result(b.toString(), offsets);
    }

    /**
     * Add to the offsets that are used to map the changed (shortened) text's
     * indices to the original text.
     * 
     * @param offsets
     * @param index The original index
     * @param diff The difference in length between the original text and
     * replacement
     */
    private static void updateOffsets(Map<Integer, Integer> offsets, int index, int diff) {
        int prevOffset = 0;
        for (Map.Entry<Integer, Integer> entry : offsets.entrySet()) {
            if (index > entry.getKey()) {
                prevOffset += entry.getValue();
            }
        }
        offsets.put(index - prevOffset, diff);
    }
    
    /**
     * Brute-forces diacritic marks on the given character. Only returns
     * characters that end up as a single codepoint after trying to add the
     * diacrictic mark.
     * 
     * @param character A single character
     * @return 
     */
    private static Collection<String> generateDiacritics(String character) {
        Set<String> result = new LinkedHashSet<>();
        for (int i = 0x0300; i <= 0x036F; i++) {
            String combined = Normalizer.normalize(character + new String(Character.toChars(i)), Normalizer.Form.NFC);
            if (singleCodepoint(combined)) {
                result.add(combined);
            }
        }
        return result;
    }
    
    //==========================
    // Replacing Result
    //==========================
    
    public static class Result {
        
        private static final Map<Integer, Integer> EMPTY = new HashMap<>();
        
        private final String changedText;
        private final Map<Integer, Integer> offsets;

        public Result(String changedText, Map<Integer, Integer> offsets) {
            this.changedText = changedText;
            this.offsets = offsets == null ? EMPTY : offsets;
        }
        
        public String getChangedText() {
            return changedText;
        }
        
        /**
         * Convert an index from the modified String to the corresponding index
         * from the original String.
         *
         * <p>
         * Example (not typical, but it's demonstrates convertion better):
         * <pre>
         * The String 'test123' was changed to 't123'
         *             0123456                  0123
         * </pre>
         *
         * <p>
         * If a regex matches on 't123', the index range would be 0-4 (end
         * exclusive), so on the original this would only be 'tes'. This method
         * converts the index range to 0-7, for 'test123'.
         *
         * <p>
         * If a regex matches on 't', the index range would be 0-1. Since the
         * end index is after the last matched character it is after the removed
         * section and thus includes the offset, so the converted range is 0-4,
         * which on the original is 'test'.
         *
         * <p>
         * If a regex matches on '123', the index range would be 1-4 ('est' on
         * the original), the converted range 4-7 ('123' on the original).
         *
         * @param index An index of the changed String
         * @return The corresponding index of the original String
         */
        public int indexToOriginal(int index) {
            return index + getOffset(index);
        }
        
        /**
         * Same as {@link #indexToOriginal(int)}, but only the offset without including
         * the given index.
         * 
         * @param index
         * @return 
         */
        public int getOffset(int index) {
            if (offsets == null) {
                return 0;
            }
            int resultOffset = 0;
            for (Map.Entry<Integer, Integer> entry : offsets.entrySet()) {
                int changedIndex = entry.getKey();
                int offset = entry.getValue();
                if (index > changedIndex) {
                    resultOffset += offset;
                }
                else {
                    break;
                }
            }
            return resultOffset;
        }
        
        public int indexToChanged(int index) {
            if (offsets == null) {
                return 0;
            }
            for (Map.Entry<Integer, Integer> entry : offsets.entrySet()) {
                int changedIndex = entry.getKey();
                int offset = entry.getValue();
                if (index > changedIndex) {
                    index -= offset;
                }
                else {
                    break;
                }
            }
            return index;
        }
        
        /**
         * Outputs the result with a numbers below for easier debugging.
         * 
         * @return 
         */
        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append("'").append(changedText).append("'").append(offsets);
            b.append("\n ");
            for (int i=0;i<changedText.length();i++) {
                if (i%10 == 0) {
                    b.append("|");
                }
                else {
                    b.append(i%10);
                }
            }
            return b.toString();
        }
        
    }
    
    //==========================
    // Presets
    //==========================
    
    /**
     * Lookalikes for the latin alphabet (a-z), for use with a Replacer2.
     *
     * Automatically generated (some characters filtered) from:
     * https://www.unicode.org/Public/security/14.0.0/confusables.txt
     * https://www.unicode.org/copyright.html
     */
    public static final List<String> LOOKALIKES = Arrays.asList(new String[]{
        "a # a 𝝰 𝙰 ａ 𝗮 𝐴 ᗅ 𝑨 𝚨 𝕬 𝓪 𝞪 𝖠 ɑ 𝔞 𝘢 𝛢 𝙖 𝝖 𝒜 𝜜 𝐚 𖽀 𝓐 𝞐 𝑎 𝗔 𝕒 𝘈 𝖆 Ꭺ 𝚊 ꓮ а 𝐀 α 𝔄 𝒂 𝛂 𝔸 ⍺ 𝒶 𝜶 𐊠 𝛼 𝘼 𝖺",
        "b # b 𝗯 ｂ 𝕭 ƅ 𝙱 ь 𝓫 Ꮟ 𝑩 𝚩 ꓐ 𝔟 𝜝 𝘣 𝛣 𝖡 𝙗 𝝗 𐊂 𐌁 𝗕 𝐛 𝑏 𝕓 𝓑 𝞑 𝖇 𝔅 ℬ 𝚋 ᖯ 𝘉 ᑲ β в 𝘽 Ꞵ Ᏼ 𝒃 𝐁 ᗷ 𝒷 𐊡 𝐵 𝖻 𝔹",
        "c # c 𝗰 с ℂ 𝕮 ｃ ᴄ 𝙲 𐐽 𝓬 𝑪 𝔠 𝒞 𝘤 𝖢 𝙘 𐌂 𝗖 ꓚ 𝐜 Ꮯ 𝑐 𐔜 𝕔 ⲥ 𝓒 𝖈 𝚌 ℭ 𝘊 ꮯ ϲ 𝘾 𝒄 🝌 𝐂 𑣲 𝒸 𑣩 𐊢 𝐶 𝖼 ⅽ",
        "d # d ԁ 𝕯 𝓭 ⅅ 𝙳 ⅆ 𝗱 𝘥 𝑫 𝒟 ꓒ 𝐝 ꓓ 𝖣 𝔡 𝗗 𝕕 ᗞ 𝙙 Ꭰ 𝚍 𝓓 𝑑 Ꮷ 𝔇 ᗪ 𝒅 𝘋 𝖉 ᑯ 𝘿 𝖽 𝐃 𝐷 𝔻 ⅾ 𝒹",
        "e # e 𝕰 𝓮 𝙴 ｅ 𝗲 ⅇ 𝘦 𝛦 𝑬 𝚬 𝜠 𝐞 𝖤 𝔢 𝗘 𝕖 𐊆 𝙚 𝝚 𝚎 𝓔 𝞔 𝑒 𝔈 𝒆 𝘌 Ꭼ ℮ 𝖊 ℯ ℰ 𝙀 ꓰ 𝖾 ꬲ 𑢮 𝐄 е ε 𝐸 ⴹ 𑢦 𝔼 ҽ ⋿",
        "f # f 𝓯 𝑭 ք 𝈓 𝗳 𝕱 𑢢 𝘧 𝖥 𝐟 𝔣 Ꞙ 𝕗 ꞙ 𝓕 𐊇 𝙛 ẝ ϝ ꓝ 𝗙 𐔥 𝚏 𝘍 𝑓 𑣂 𝒇 𝐅 𝖋 𝟊 𝔉 𝖿 ℱ 𝔽 ᖴ ꬵ 𝙁 𝙵 𝒻 𐊥 ſ 𝐹",
        "g # g 𝓰 Ꮐ ց 𝑮 ᶃ 𝗴 𝕲 ｇ 𝘨 ℊ 𝖦 ƍ ԍ 𝐠 𝔤 𝒢 ꓖ 𝕘 𝓖 𝙜 𝗚 𝚐 ɡ 𝘎 𝑔 𝒈 𝐆 𝖌 𝔊 𝗀 𝔾 Ᏻ 𝙂 𝙶 𝐺",
        "h # h 𝑯 Ꮒ 𝚮 𝕳 𝓱 ｈ 𝛨 𝖧 𝔥 ℋ ℌ ℍ ℎ 𝘩 ⲏ 𝙝 𐋏 𝜢 𝐡 𝓗 𝞖 𝝜 𝗛 𝕙 𝘏 𝖍 𝚑 ꓧ 𝐇 𝒉 հ 𝒽 𝙃 𝗁 η 𝙷 𝗵 һ Ꮋ ᕼ 𝐻 н",
        "i # i 𝓲 𝞲 ꙇ ⅈ ｉ 𝔦 𝘪 ӏ 𝙞 𝚤 𝐢 і 𝑖 ˛ 𝕚 𝖎 Ꭵ 𝚒 𑣃 ɩ ɪ 𝒊 𝛊 ⅰ ı 𝒾 𝜾 ⍳ 𝜄 ꭵ 𝗂 𝝸 ℹ ι 𝗶 ͺ ι",
        "j # j 𝓳 𝑱 ⅉ 𝔧 ｊ 𝒥 𝘫 ᒍ 𝖩 𝙟 𝗝 𝐣 ј 𝑗 ꓙ 𝕛 𝓙 𝖏 𝔍 𝚓 𝘑 𝙅 Ꭻ 𝒋 𝐉 𝒿 Ʝ ϳ 𝐽 𝗃 𝕁 𝗷 𝕵 𝙹 Ϳ",
        "k # k 𝓴 𝑲 𝚱 𝔨 𝒦 ｋ 𝜥 𝘬 𝛫 𝖪 𝙠 𝝟 𝗞 𝐤 ⲕ ᛕ ꓗ 𝑘 𝕜 𝓚 𝞙 𝖐 𝔎 𝚔 𝘒 Ꮶ K 𝙆 𝒌 𐔘 𝐊 𝓀 𝐾 𝗄 𝕂 𝗸 𝕶 κ к 𝙺",
        "l # l 𝚰 𝘭 𝖨 𝐥 𝖫 𝔩 ℐ ℑ 𐊊 𐌉 ℒ ℓ ⲓ 𝜤 𝞘 𝚕 𝟏 ∣ 𝙇 ᒪ 𝗅 𝕀 𝐿 𝙄 𝕃 𝓁 ι 𐌠 𝐼 𝑰 ǀ 𖼖 ᛁ 𝟭 𝕴 𝑳 𑢣 ｉ ｌ 𝛪 ӏ ⵏ 𝗟 ⳑ 𝝞 𝕝 𝟣 і 𝙡 𝓘 𝗜 𝓛 Ꮮ 𐔦 𝟙 𝑙 𝘐 𝔏 ꓡ 𝒍 𝘓 𝖑 ￨ 🯱 𝐈 ɩ 𝈪 𝐋 ⅰ ۱ ꓲ 𖼨 𑢲 𝙸 𝟷 𝕷 𐑃 𝓵 | ⅼ ⏽ 𝙻 𝗹",
        "m # m 𝛭 𝑴 𝚳 𝜧 𐌑 𝖬 ｍ 𝗠 ᛖ 𝝡 ⲙ 𝓜 𝞛 ꓟ 𝔐 𝘔 𝙈 𐊰 𝐌 𝑀 ᗰ ℳ 𝕄 Ꮇ 𝕸 ϻ 𝙼 μ м ⅿ",
        "n # n 𝘯 𝛮 𝖭 𝚴 𝜨 𝐧 𝔫 ｎ 𝒩 𝕟 𝓝 𝙣 ℕ 𝝢 𝗡 𝚗 𝘕 ⲛ 𝞜 𝑛 ꓠ 𝒏 𝐍 𝖓 𝔑 𝗇 𐔓 𝙉 𝙽 𝓃 𝑁 ո 𝓷 𝑵 ռ 𝗻 ν 𝕹",
        "o # o 𝘰 𑣠 ం ಂ ം ං 𝖮 օ 〇 𝐨 𐊒 𑣗 𝔬 𝒪 𝜪 ᴏ ᴑ 𐓪 𝞞 𝚘 𑣈 𝘖 ဝ ⲟ 𝛐 ഠ ଠ 𝟎 𝛔 𝗈 𝝈 𝕆 𝙊 𐔖 𐊫 ℴ 𝝄 𝑂 𝞸 𝚶 𝞼 ꬽ о ο ၀ 𝛰 σ 𝟬 ｏ ๐ ໐ 𝕠 𐐬 ዐ 𝓞 𝙤 𝝤 ⵔ 𝟢 𝗢 𝟘 𝑜 𝒐 𝜎 𝐎 𝖔 ௦ ੦ ० ૦ ౦ ೦ ൦ 𝔒 ০ ୦ 🯰 𝜊 𑓐 𝝾 𝙾 ꓳ 𑢵 ۵ 𝞂 𝓸 𝟶 𝑶 𐓂 𝗼 𝕺 ჿ",
        "p # p 𝖯 𝔭 𝘱 𝜬 𝒫 𐊕 𝐩 𝞠 ℙ 𝘗 𝖕 𝜚 𝚙 ⲣ 𝝔 𝛒 𝟈 𝝆 𝓅 𝙋 𝗉 𝑃 𝚸 𝞺 р ρ 𝛲 𝝦 𝙥 ｐ 𝛠 𝓟 ꓑ 𝑝 𝗣 𝕡 𝐏 𝞎 Ꮲ 𝔓 𝒑 𝜌 ᑭ 𝞀 ϱ 𝙿 𝗽 ⍴ 𝑷 𝕻 𝓹",
        "q # q 𝖰 𝔮 𝘲 𝙦 𝒬 𝐪 𝓠 𝑞 𝗤 ⵕ 𝕢 𝘘 𝖖 ℚ ԛ 𝚚 𝐐 գ 𝔔 𝒒 զ 𝓆 𝙌 𝗊 𝚀 𝗾 𝑄 𝑸 𝕼 𝓺",
        "r # r 𝔯 ꮁ 𝘳 ⲅ ꭇ 𝖱 ᖇ ꭈ 𐒴 𝙧 𝗥 𝐫 𝑟 Ꮢ 𝕣 𝓡 𝖗 ℛ ℜ 𝚛 ℝ 𝘙 Ꭱ 𖼵 𝙍 ꓣ 𝒓 ᴦ Ʀ 𝐑 𝓇 𝑅 𝗋 𝗿 г 𝕽 𝚁 𝈖 𝓻 𝑹",
        "s # s 𝔰 𝒮 𝘴 𝖲 𝙨 𝗦 𝐬 𐊖 𝑠 ｓ 𝕤 ѕ Ꮥ 𝓢 𝖘 𝔖 Ꮪ 𝚜 𝘚 𑣁 𝙎 ꓢ 𝒔 𖼺 𝐒 𝓈 ꮪ 𝑆 𝗌 𝕊 𝘀 ꜱ 𝕾 𝚂 𝓼 𐑈 ƽ 𝑺 տ",
        "t # t 𝒯 𝜯 т 𝐭 τ 𝖳 𝔱 𝗧 𝕥 𐊗 𐌕 𝙩 𝝩 🝨 𝚝 ｔ ꓔ 𖼊 𝓣 𝞣 𝑡 ⟙ 𝔗 𝒕 𝘛 𝖙 𝙏 Ꭲ 𝗍 ⊤ 𝐓 ⲧ 𝑇 𐊱 𝕋 𑢼 𝓉 𝕿 𝓽 𝚃 𝘁 𝘵 𝛵 𝑻 𝚻",
        "u # u 𝒰 ሀ 𝐮 ⋃ 𝖴 υ 𝔲 𝗨 𑣘 𝕦 ʋ ᑌ 𝙪 ꭎ 𐓶 𝚞 ꭒ 𝓤 𝑢 𝔘 𝒖 𝛖 ᴜ 𝘜 𖽂 𝖚 ꞟ 𝜐 𝙐 𝗎 𝐔 𝑈 𑢸 ∪ 𝕌 𝓊 𝝊 𝖀 𝓾 𝞾 𝞄 𝚄 ꓴ 𝘂 𐓎 𝘶 𝑼 ս",
        "v # v 𝐯 𝔳 𝒱 𝚟 𝘝 𝒗 𝐕 𝖛 𝔙 ᴠ 𝗏 𝛎 𝕍 𝙑 𐔝 ∨ ꮩ 𝓋 𝑉 ᐯ 𝝂 ⴸ 𝘷 𝞶 𑜆 𝖵 ν ⋁ 𑢠 𝈍 𝕧 𝓥 𝙫 𝗩 𝑣 ｖ 𖼈 Ꮩ ꛟ 𑣀 ꓦ 𝜈 𝚅 𝓿 𝑽 ⅴ 𝘃 ѵ ۷ 𝖁 𝝼",
        "w # w 𝐰 ꮃ 𝔴 𝒲 𝕨 𝓦 𝙬 𝗪 𝚠 𝘞 𝑤 Ꮤ 𝒘 𝐖 𝖜 ԝ 𝔚 𝗐 ᴡ ѡ ա 𝕎 𝙒 𝚆 ꓪ 𝓌 𝑊 ɯ 𝔀 𑜏 𑜎 𑣯 𝑾 Ꮃ 𝘄 𝖂 𝘸 𝖶 𑣦 𑜊",
        "x # x 𝒳 𝜲 𝐱 𝞦 𐊐 𐌗 𝘟 𝖝 𝚡 𝐗 𝔛 𝒙 𝕏 𝓍 𝙓 𝗑 ⤫ ⤬ 𝑋 ⲭ ⨯ 𐊴 𝚾 Ꭓ ᚷ 𐌢 𝖷 𝔵 𑣬 𝘹 ᕁ 𝙭 х χ 𝓧 𝑥 𝝬 𝗫 𝕩 × ｘ ⵝ 𐔧 𝚇 𝘅 ꓫ ᙭ ᙮ 𝑿 ╳ 𝖃 𝔁 𝛸 ⅹ ᕽ",
        "y # y 𝜰 𝒴 𝐲 ᶌ 𝞬 𑣜 ʏ 𝘠 𝖞 𖽃 𝞤 𝚢 𝐘 𝔜 𝒚 𝕐 𝓎 𝙔 𝗒 ⲩ Ꭹ 𐊲 𝑌 ү γ 𝛄 𝜸 𝖸 𝔶 𝚼 ℽ Ꮍ 𝘺 𝙮 у υ 𑢤 𝝲 𝓨 𝑦 𝗬 𝕪 𝝪 ϒ ｙ ꭚ ɣ ყ 𝚈 𝘆 ꓬ 𝒀 𝛾 𝖄 𝔂 𝛶 ỿ",
        "z # z 𝙯 𝗭 𝚭 Ꮓ 𑣥 𝐳 𝑧 𝛧 𝕫 𝓩 𝖟 ꮓ 𝚣 𝘡 𝜡 ｚ 𝙕 𝞕 ꓜ 𝒛 𝝛 𝐙 𝓏 ᴢ 𝑍 ℤ 𝗓 𑣄 ℨ 𝘇 𝖅 𐋵 𝚉 𝔃 ζ 𝒁 𑢩 𝔷 𝒵 𝘻 𝖹"
    });
    
    //==========================
    // Test
    //==========================
    
    private static Result testResult;
    
    public static void main(String[] args) {
        Replacer2 item = create(Arrays.asList(new String[]{"a @ 𝒜 а", "t 𝒯", "t test", "hattrick hhat hat", "o ()"}));
        String message = "hh𝒜t testi𝒜bc bac𝒜 hat Аbc";
        System.out.println("'"+message+"'");
        Result result = item.replace(message);
        System.out.println(result);
//        System.out.println("###"+result.changedText+"### "+result.offsets);
        Pattern testPattern = Pattern.compile("testi𝒜bc");
        Matcher m = testPattern.matcher(message);
        if (m.find()) {
            int start = m.start();
            int end = m.end();
            System.out.println("Range: "+start+"-"+end);
            System.out.println("'" + message.substring(start, end) + "'");
            int start2 = result.indexToChanged(start);
            int end2 = result.indexToChanged(end);
            System.out.println("'" + result.changedText.substring(start2, end2) + "' Converted Range: " + start2 + "-" + end2);
        }
        
        List<String> data = LOOKALIKES;
        Replacer2 fullItem = create(data);
        long startTime = System.currentTimeMillis();
        for (int i=0;i<1000;i++) {
            for (String line : data) {
                testResult = fullItem.replace("ab ewf waef awef weafℤ awef awe fawef awe fawe fawef");
                testResult = fullItem.replace(line);
//                if (result != null) {
//                    System.out.println(result.changedText);
//                }
            }
        }
        System.out.println(System.currentTimeMillis() - startTime);
        
//        System.out.println("S".replaceAll("(?iu)ſ", "abc"));
    }
    
    
}
