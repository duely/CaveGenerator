package com.personthecat.cavegenerator.util;

import com.personthecat.cavegenerator.world.GeneratorSettings;

import java.util.Optional;

/**
 * Contains settings for how a particular floating point value
 * is intended to change throughout its lifetime.
 */
public class ScalableFloat {
    public final float exponent;
    public final float factor;
    public final float randFactor;
    public final float startVal;
    public final float startValRandFactor;

    public ScalableFloat(
        float startVal,
        float startValRandFactor,
        float factor,
        float randFactor,
        float exponent
    ) {
        this.startVal = startVal;
        this.startValRandFactor = startValRandFactor;
        this.factor = factor;
        this.randFactor = randFactor;
        this.exponent = exponent;
    }

    /**
     * Constructs a new instance of this object using optional
     * values.
     * @param defaults The container of default properties to
     *                 refer to in the event any value is not
     *                 present.
     * @see GeneratorSettings.SpawnSettings#SpawnSettings for
     * information on why Optional types are used here.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static ScalableFloat fromDefaults(
        ScalableFloat defaults,
        Optional<Float> startVal,
        Optional<Float> startValRandFactor,
        Optional<Float> factor,
        Optional<Float> randFactor,
        Optional<Float> exponent
    ) {
        return new ScalableFloat(
            startVal.orElse(defaults.startVal),
            startValRandFactor.orElse(defaults.startValRandFactor),
            factor.orElse(defaults.factor),
            randFactor.orElse(defaults.randFactor),
            exponent.orElse(defaults.exponent)
        );
    }
}