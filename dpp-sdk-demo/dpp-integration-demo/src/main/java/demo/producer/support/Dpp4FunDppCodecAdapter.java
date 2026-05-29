package demo.producer.support;

import dpp.repo.client.core.DppCodec;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import java.util.Objects;

public final class Dpp4FunDppCodecAdapter implements DppCodec<Dpp4Fun> {

    private final Dpp4FunJsonCodec codec;

    public Dpp4FunDppCodecAdapter() {
        this(new Dpp4FunJsonCodec());
    }

    public Dpp4FunDppCodecAdapter(Dpp4FunJsonCodec codec) {
        this.codec = Objects.requireNonNull(codec, "codec must not be null");
    }

    @Override
    public String toJson(Dpp4Fun dpp) {
        return codec.toJson(dpp);
    }

    @Override
    public Dpp4Fun fromJson(String json) {
        return codec.fromJson(json);
    }
}
