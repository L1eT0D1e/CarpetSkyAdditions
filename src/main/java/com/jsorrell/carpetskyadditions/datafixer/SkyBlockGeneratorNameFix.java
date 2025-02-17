package com.jsorrell.carpetskyadditions.datafixer;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

// Convert all instances of minecraft:skyblock to skyblock:skyblock
public class SkyBlockGeneratorNameFix extends DataFix {
  private static final String NAME = "SkyBlockGeneratorNameFix";

  public SkyBlockGeneratorNameFix(Schema outputSchema) {
    super(outputSchema, true);
  }

  @Override
  protected TypeRewriteRule makeRule() {
    Type<?> inputType = this.getInputSchema().getType(TypeReferences.WORLD_GEN_SETTINGS);
    OpticFinder<?> inputDimensionsField = inputType.findField("dimensions");
    Type<?> outputType = this.getOutputSchema().getType(TypeReferences.WORLD_GEN_SETTINGS);
    Type<?> outputDimensionsFieldType = outputType.findFieldType("dimensions");
    return this.fixTypeEverywhereTyped(NAME, inputType, outputType, inputWorldGenSettings -> inputWorldGenSettings.updateTyped(inputDimensionsField, outputDimensionsFieldType, inputDimensions -> {
      Dynamic<?> dynamicDimensions = inputDimensions.write().result().orElseThrow(() -> new IllegalStateException("Malformed WorldGenSettings.dimensions"));
      dynamicDimensions = dynamicDimensions.updateMapValues(pair -> pair.mapSecond(dimensionDynamic -> dimensionDynamic.update("generator", dimensionGeneratorDynamic -> {
        String generatorType = dimensionGeneratorDynamic.get("type").asString("");
        if ("minecraft:skyblock".equals(generatorType)) {
          return dimensionGeneratorDynamic.update("type", generatorTypeDynamic -> generatorTypeDynamic.createString("skyblock:skyblock"));
        }
        return dimensionGeneratorDynamic;
      })));
      return outputDimensionsFieldType.readTyped(dynamicDimensions).result().orElseThrow(() -> new IllegalStateException(NAME + " failed.")).getFirst();
    }));
  }
}
