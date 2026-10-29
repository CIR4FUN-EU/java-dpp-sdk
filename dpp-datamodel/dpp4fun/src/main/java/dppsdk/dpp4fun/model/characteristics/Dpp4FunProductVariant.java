package dppsdk.dpp4fun.model.characteristics;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.unit.*;

/** Frozen D4.1 Characteristics templates. */
public enum Dpp4FunProductVariant {
    CHAIR,
    CRIB,
    FABRIC,
    LOCKER,
    PARTICLE_BOARD,
    SOFA;

    /** Returns this variant's catalog key in lower camel case. */
    public String id() {
        String[] words = name().toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder id = new StringBuilder(words[0]);
        for (int index = 1; index < words.length; index++) {
            id.append(Character.toUpperCase(words[index].charAt(0))).append(words[index].substring(1));
        }
        return id.toString();
    }
}
