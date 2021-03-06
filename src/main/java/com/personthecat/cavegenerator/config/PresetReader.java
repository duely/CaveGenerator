package com.personthecat.cavegenerator.config;

import com.personthecat.cavegenerator.world.*;
import com.personthecat.cavegenerator.world.GeneratorSettings.*;
import com.personthecat.cavegenerator.world.feature.GiantPillar;
import com.personthecat.cavegenerator.world.feature.LargeStalactite;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.*;
import java.util.*;

import static com.personthecat.cavegenerator.util.CommonMethods.*;
import static com.personthecat.cavegenerator.util.HjsonTools.*;

/**
 *   Contains all of the tools and processes used for parsing .json, .hjson,
 * and .cave files into GeneratorSettings objects. The basic design
 * philosophy is that all fields should be optional and have default values,
 * unless completely necessary. I've tried to reduce the syntax for parsing
 * fields to be as small as possible, but there is still some room for
 * improvement. Essentially, the current syntax works like this:
 * ```
 *   // Defaults are stored inside of the constructor. You only need
 *   // to call the default initializer when no JsonObject is found.
 *   Object parsed = getObject(fromJson, namedThis)
 *       .ifPresent(foundObject -> new EquivalentObject(
 *           getField(foundObject, namedThis),
 *           getField(foundObject, namedThis,
 *           ... ))
 *       .ifNotPresent(new EquivalentObject.default());
 * ```
 *   I'm not exactly sure how readable this is. On one hand, you can see where
 * each field retrieved corresponds to a certain type and name. On the other,
 * everything is condensed in a way that I'm sure is not idiomatic for Java. A
 * much prettier setup would be to use the standard syntax of having mutable
 * fields with getters and setters. This would allow me to instantiate an object
 * with default values and then use setters to handle modifying the values,
 * which would look like this:
 * ```
 *   // Construct the new base object, then modify it.
 *   Object parsed = new ObjectToParse();
 *   getType(fromJson, namedThis, parsed::callSetter);
 *   getType(fromJson, namedThis, parsed::callSetter);
 * ```
 *   I feel that this syntax is clearly the more readable of the two, however,
 * it does not allow the fields to be initialized into a final state, which is
 * preferred for the following reasons:
 *   * Good measure.
 *   * Clearly indicates that field values are not supposed to change.
 *   * Safer and faster use of multithreading later on.
 *   As such, it is not my preferred solution for handling the serializers. It is
 * worth noting, however, that a similar syntax can be achieved using builders.
 * Unfortunately, I worry that having two nearly-identical classes for every
 * data structure contained within this program will be a little too redundant
 * for my preferences.
 *   It is also worth noting that I have looked into the dependency known as
 * "Immutables," which I believe could help me achieve my goals in a much more
 * readable form. Unfortunately, due to the nature in which this program is used,
 * declaring dependencies such as this is not really an option, as they would have
 * to be imported directly into the jar file in order for users to have them in-
 * game through Forge. I have already done this with Hjson-Java and will most
 * likely also do it with FastNoise, and would really like to avoid over-doing it.
 *   As always, any suggestions for improvement can be submitted in the form of an
 * issue or pull request on GitHub. -PersonTheCat
 *
 * To-do: Test objects for potential errors and improvements and report them to
 * the user.
 */
public class PresetReader {
    /**
     * The basic function used for retrieving a GeneratorSettings object from
     * the input File. Will throw a ParseException if the file cannot be
     * parsed.
     */
    public static GeneratorSettings getPreset(File file) {
        JsonObject json = loadJson(file).asObject();
        final boolean b = blankSlateMode(json);
        return new GeneratorSettings(
            getSpawnSettings(json),
            getTunnelSettings(json, b),
            getRavineSettings(json, b),
            getRoomSettings(json, b),
            getCavernSettings(json),
            getStructureSettings(json),
            getDecoratorSettings(json, b),
            getReplaceableBlocks(json),
            getReplaceDecorators(json),
            json
        );
    }

    /**
     * Returns a JsonObject from the input file. Ensures that an error is handled
     * by any external callers.
     */
    public static Optional<JsonObject> getPresetJson(File file) {
        try {
            return full(loadJson(file).asObject());
        } catch (RuntimeException e) {
            return empty();
        }
    }

    /** Parses the contents of @param file into a generic JsonValue. */
    private static JsonValue loadJson(File file) {
        try {
            Reader reader = new FileReader(file);
            String extension = extension(file);
            if (extension.equals("json")) {
                return JsonObject.readJSON(reader);
            } else {
                return JsonObject.readHjson(reader);
            }
        } catch (IOException e) {
            throw runExF("Unable to load preset file %s.", file.getName());
        }
    }

    /** Determines whether the input object is in blank slate mode. */
    private static boolean blankSlateMode(JsonObject json) {
        return getBoolOr(json, "blankSlate", true);
    }

    /** Parses various fields in the root of the input json related to spawn conditions. */
    private static SpawnSettings getSpawnSettings(JsonObject json) {
        return new SpawnSettings(json);
    }

    /** Retrieves a list of replaceable blocks from the input object. */
    private static IBlockState[] getReplaceableBlocks(JsonObject json) {
        return getBlocksOr(json, "replaceableBlocks",
            Blocks.STONE.getDefaultState(),
            Blocks.DIRT.getDefaultState()
        );
    }

    /** Retrieves a single boolean from the input object. Looks nicer this way. */
    private static boolean getReplaceDecorators(JsonObject json) {
        return getBoolOr(json, "replaceDecorators", true);
    }

    /** Parses the field "tunnels" from this json into a TunnelSettings object. */
    private static TunnelSettings getTunnelSettings(JsonObject json, boolean blankSlate) {
        return getObject(json, "tunnels")
            .map(TunnelSettings::new) // The user defined a field.
            .orElse(new TunnelSettings(blankSlate)); // The user did not define a field.
    }

    /**
     * This seems fairly redundant, but isn't. The fields belong
     * to a separate object with mostly similar properties.
     */
    private static RavineSettings getRavineSettings(JsonObject json, boolean blankSlate) {
        return getObject(json, "ravines")
            .map(RavineSettings::new) // The user defined a field.
            .orElse(new RavineSettings(blankSlate)); // The user did not define a field.
    }

    /** Parses the field "rooms" from this json into a RoomSettings object. */
    private static RoomSettings getRoomSettings(JsonObject json, boolean blankSlate) {
        return getObject(json, "rooms")
            .map(RoomSettings::new)
            .orElse(new RoomSettings(blankSlate));
    }

    /** Parses the field "caverns" from this json into a CavernSettings object. */
    private static CavernSettings getCavernSettings(JsonObject json) {
        return getObject(json, "caverns")
            .map(CavernSettings::new)
            .orElse(new CavernSettings());
    }

    /** Parses the field "structures" from this json into an array of StructureSettings objects. */
    private static StructureSettings[] getStructureSettings(JsonObject json) {
        List<StructureSettings> structures = new ArrayList<>();
        for (JsonObject structure : getObjectArray(json, "structures")) {
            // Construct and add the final structure settings.
            structures.add(new StructureSettings(structure));
        }
        return toArray(structures, StructureSettings.class);
    }

    /** Parses various decorator objects from this json.. */
    private static DecoratorSettings getDecoratorSettings(JsonObject json, boolean blankSlate) {
        return new DecoratorSettings(
            getStoneClusters(json),
            getStoneLayers(json),
            getCaveBlocks(json),
            getWallDecorators(json),
            getLargeStalactites(json),
            getGiantPillars(json),
            blankSlate
        );
    }

    /** Parses the field "stoneClusters" from this json into an array of StoneCluster objects. */
    private static StoneCluster[] getStoneClusters(JsonObject json) {
        List<StoneCluster> stoneClusters = new ArrayList<>();
        for (JsonObject cluster : getObjectArray(json, "stoneClusters")) {
            // Start with the states. This value must exist.
            IBlockState[] states = getGuranteedStates(cluster, "StoneCluster");

            // Create a stone cluster for each state in the array.
            for (IBlockState state : states) {
                stoneClusters.add(new StoneCluster(state, cluster));
            }
        }
        return toArray(stoneClusters, StoneCluster.class);
    }

    /** Parses the field "stoneLayers" from this json into an array of StoneLayer objects. */
    private static StoneLayer[] getStoneLayers(JsonObject json) {
        List<StoneLayer> stoneLayers = new ArrayList<>();
        for (JsonObject layer : getObjectArray(json, "stoneLayers")) {
            stoneLayers.add(new StoneLayer(layer));
        }
        return toArray(stoneLayers, StoneLayer.class);
    }

    /** Parses the field "caveblocks" from this json into an array of CaveBlock objects. */
    private static Optional<CaveBlock[]> getCaveBlocks(JsonObject json) {
        // Manually verify whether the field exists.
        // To-do: switch to Optional, instead.
        if (json.get("caveBlocks") == null) {
            return empty();
        }
        // Create an empty list.
        List<CaveBlock> caveBlocks = new ArrayList<>();
        for (JsonObject caveBlock : getObjectArray(json, "caveBlocks")) {
            // Start with the states. This field must exist.
            IBlockState[] states = getGuranteedStates(caveBlock, "CaveBlock");

            // Create a CaveBlock object for each state in the array.
            for (IBlockState state : states) {
                // Create and push the CaveBlock object into the array.
                caveBlocks.add(new CaveBlock(state, caveBlock));
            }
        }
        return full(toArray(caveBlocks, CaveBlock.class));
    }

    /** Parses the field "wallDecorators" from this json into an array of WallDecorator objects. */
    private static WallDecorator[] getWallDecorators(JsonObject json) {
        List<WallDecorator> decorators = new ArrayList<>();
        for (JsonObject decorator : getObjectArray(json, "wallDecorators")) {
            // Start with the states. This field must exist.
            IBlockState[] states = getGuranteedStates(decorator, "WallDecorator");

            // Construct an object for each state in the list.
            for (IBlockState state : states) {
                decorators.add(new WallDecorator(state, decorator));
            }
        }
        return toArray(decorators, WallDecorator.class);
    }

    /** Parses the large stalactites from this json into an array of LargeStalactite objects.  */
    private static LargeStalactite[] getLargeStalactites(JsonObject json) {
        List<LargeStalactite> stalactites = new ArrayList<>();
        for (JsonObject stalactite : getObjectArray(json, "largeStalactites")) {
            stalactites.add(getStalactite(stalactite, LargeStalactite.Type.STALACTITE));
        }
        for (JsonObject stalagmite : getObjectArray(json, "largeStalagmites")) {
            stalactites.add(getStalactite(stalagmite, LargeStalactite.Type.STALAGMITE));
        }
        return toArray(stalactites, LargeStalactite.class);
    }

    /** Gets a single LargeStalactite of type @param type. */
    private static LargeStalactite getStalactite(JsonObject json, LargeStalactite.Type type) {
        return new LargeStalactite(type, json);
    }

    private static GiantPillar[] getGiantPillars(JsonObject json) {
        List<GiantPillar> pillars = new ArrayList<>();
        for (JsonObject pillar : getObjectArray(json, "giantPillars")) {
            // Start with the state. This value must exist.
            pillars.add(new GiantPillar(pillar));
        }
        return toArray(pillars, GiantPillar.class);
    }
}