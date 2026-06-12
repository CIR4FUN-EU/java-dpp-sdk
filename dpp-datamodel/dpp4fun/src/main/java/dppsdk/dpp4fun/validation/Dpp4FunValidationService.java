package dppsdk.dpp4fun.validation;

import dppsdk.core.model.Dpp;
import dppsdk.core.validation.ValidationException;
import dppsdk.core.validation.ValidationService;
import dppsdk.dpp4fun.model.BillOfMaterials;
import dppsdk.dpp4fun.model.Characteristics;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.Component;
import dppsdk.dpp4fun.model.Dimensions;
import dppsdk.dpp4fun.model.Material;
import dppsdk.dpp4fun.model.Part;
import dppsdk.dpp4fun.model.ProductClassification;

/**
 * Product-specific validation facade that layers DPP4Fun validators on top of the core service.
 */
public final class Dpp4FunValidationService {

    private final ValidationService validationService;

    public Dpp4FunValidationService() {
        this.validationService = new ValidationService()
                .register(Dimensions.class, new DimensionsValidator())
                .register(Material.class, new MaterialValidator())
                .register(Component.class, new ComponentValidator())
                .register(Part.class, new PartValidator())
                .register(ProductClassification.class, new ProductClassificationValidator())
                .register(Characteristics.class, new CharacteristicsValidator())
                .register(BillOfMaterials.class, new BillOfMaterialsValidator())
                .register(Dpp4Fun.class, new Dpp4FunValidator())
                .register(Dpp.class, value -> {
                    if (value instanceof Dpp4Fun dpp) {
                        new Dpp4FunValidator().validate(dpp);
                        return;
                    }
                    throw new ValidationException(
                            "No concrete validator available for Dpp subtype: " + value.getClass().getName());
                });
    }

    public void validate(Object value) {
        validationService.validate(value);
    }

    public ValidationService coreService() {
        return validationService;
    }
}

