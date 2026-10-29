package dppsdk.core.validation.format;

import java.util.Locale;
import java.util.Set;

/** Pinned ISO 639 Set 1 and ISO 3166-1:2020 alpha-2 code sets. */
final class LocalizedLanguageValidator {

    /* Snapshot checked against ISO 639:2023 Set 1 / the ISO 639 MA's alpha-2 list. */
    private static final Set<String> LANGUAGE_CODES = Set.of((
            "aa ab ae af ak am an ar as av ay az ba be bg bh bi bm bn bo br bs ca ce ch co cr cs cu cv cy "
            + "da de dv dz ee el en eo es et eu fa ff fi fj fo fr fy ga gd gl gn gu gv ha he hi ho hr ht hu "
            + "hy hz ia id ie ig ii ik io is it iu ja jv ka kg ki kj kk kl km kn ko kr ks ku kv kw ky la lb lg li "
            + "ln lo lt lu lv mg mh mi mk ml mn mr ms mt my na nb nd ne ng nl nn no nr nv ny oc oj om or os pa "
            + "pi pl ps pt qu rm rn ro ru rw sa sc sd se sg si sk sl sm sn so sq sr ss st su sv sw ta te tg th ti "
            + "tk tl tn to tr ts tt tw ty ug uk ur uz ve vi vo wa wo xh yi yo za zh zu").split(" "));

    /* Snapshot checked against ISO 3166-1:2020 alpha-2 assigned code elements.
       Reserved and user-assigned codes are intentionally omitted. */
    private static final Set<String> COUNTRY_CODES = Set.of((
            "AD AE AF AG AI AL AM AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BJ BL BM BN BO BQ BR BS BT BV BW BY BZ "
            + "CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CW CX CY CZ DE DJ DK DM DO DZ EC EE EG EH ER ES ET FI FJ FK FM FO FR "
            + "GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW GY HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO "
            + "JP KE KG KH KI KM KN KP KR KW KY KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR "
            + "MS MT MU MV MW MX MY MZ NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY QA RE RO "
            + "RS RU RW SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR SS ST SV SX SY SZ TC TD TF TG TH TJ TK TL TM TN TO TR TT TV "
            + "TW TZ UA UG UM US UY UZ VA VC VE VG VI VN VU WF WS YE YT ZA ZM ZW").split(" "));

    private LocalizedLanguageValidator() { }

    static boolean hasRequiredShape(String value) {
        if (value == null || value.length() != 5 || value.charAt(2) != '-') return false;
        return isAsciiLetter(value.charAt(0)) && isAsciiLetter(value.charAt(1))
                && isAsciiLetter(value.charAt(3)) && isAsciiLetter(value.charAt(4));
    }

    static boolean isLanguageCode(String value) {
        return LANGUAGE_CODES.contains(value.toLowerCase(Locale.ROOT));
    }

    static boolean isCountryCode(String value) {
        return COUNTRY_CODES.contains(value.toUpperCase(Locale.ROOT));
    }

    private static boolean isAsciiLetter(char character) {
        return character >= 'a' && character <= 'z' || character >= 'A' && character <= 'Z';
    }
}
