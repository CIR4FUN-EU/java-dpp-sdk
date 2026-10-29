package demo.repo.testsupport;

import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;

/** JSON encoding for Dpp4Fun values used as controller test request bodies. */
public final class Dpp4FunTestJson {
    private static final Dpp4FunJsonCodec CODEC = new Dpp4FunJsonCodec();

    private Dpp4FunTestJson() {
    }

    public static String encode(Dpp4Fun dpp) {
        return CODEC.toJson(dpp);
    }
}
