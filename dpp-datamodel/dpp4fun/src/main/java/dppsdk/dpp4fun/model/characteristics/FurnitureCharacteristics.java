package dppsdk.dpp4fun.model.characteristics;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;

/** Common marker for one source-specific D4.1 Characteristics variant. */
public sealed interface FurnitureCharacteristics permits ChairCharacteristics, CribCharacteristics,
        FabricCharacteristics, LockerCharacteristics, ParticleBoardCharacteristics, SofaCharacteristics {
    Dpp4FunProductVariant variant();
}
