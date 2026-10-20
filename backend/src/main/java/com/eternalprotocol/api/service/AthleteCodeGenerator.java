package com.eternalprotocol.api.service;

import com.eternalprotocol.api.repository.AthleteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Random;

/**
 * Creates a short, unique referral code for a new athlete from their name.
 * <p>
 * The code is the first {@value #PREFIX_LENGTH} letters of the name (accents
 * and anything that isn't a letter are ignored, and the result is upper case)
 * followed by a random number. For example "Usain Bolt" becomes something like
 * {@code USA472}. A name with fewer than {@value #PREFIX_LENGTH} usable letters
 * is padded with {@code X}, so "Li" becomes {@code LIX} and a name with no
 * Latin letters at all becomes {@code XXX}.
 * <p>
 * Every candidate is checked against the database (ignoring case) and a new
 * random number is tried if it is taken. Three-digit numbers are tried first
 * and four-digit numbers are used only if those keep colliding.
 */
@Component
public class AthleteCodeGenerator {

    /** How many letters of the name go at the front of the code */
    static final int PREFIX_LENGTH = 3;

    /** Letter used to fill the prefix when the name has too few letters */
    private static final char PADDING = 'X';

    /** How many random numbers are tried at each length before moving to a longer one */
    private static final int ATTEMPTS_PER_LENGTH = 10;

    private static final int MIN_DIGITS = 3;
    private static final int MAX_DIGITS = 4;

    private final AthleteRepository athleteRepository;
    private final Random random;

    /**
     * @param athleteRepository used to check that a generated code is not already taken
     */

    @Autowired
    public AthleteCodeGenerator(AthleteRepository athleteRepository) {
        this(athleteRepository, new SecureRandom());
    }

    /**
     *
     * @param athleteRepository used to check that a generated code is not already taken
     * @param random            source of the random number added to the end of the code
     */
    AthleteCodeGenerator(AthleteRepository athleteRepository, Random random) {
        this.athleteRepository = athleteRepository;
        this.random = random;
    }

    /**
     * Generates an athlete code that no other athlete is using
     *
     * @param name the athlete's full name
     * @return a unique code such as {@code USA472}
     * @throws IllegalStateException if no free code could be found 
     */
    public String generate(String name) {
        String prefix = prefixFrom(name);

        for (int digits = MIN_DIGITS; digits <= MAX_DIGITS; digits++) {
            for (int attempt = 0; attempt < ATTEMPTS_PER_LENGTH; attempt++) {
                String candidate = prefix + randomNumber(digits);
                if (!athleteRepository.existsByAthleteCodeIgnoreCase(candidate)) {
                    return candidate;
                }
            }
        }
        throw new IllegalStateException("Could not generate a unique athlete code for \"" + name + "\"");
    }

    /**
     * Turns a name into the 3 letter part of a code
     *
     * @param name the athlete's full name
     * @return exactly {@value #PREFIX_LENGTH} upper case letters
     */
    static String prefixFrom(String name) {
        String letters = "";
        if (name != null) {
            letters = Normalizer.normalize(name, Normalizer.Form.NFD)
                    .replaceAll("[^A-Za-z]", "")
                    .toUpperCase(Locale.ROOT);
        }

        StringBuilder prefix = new StringBuilder(letters.length() > PREFIX_LENGTH
                ? letters.substring(0, PREFIX_LENGTH)
                : letters);
        while (prefix.length() < PREFIX_LENGTH) {
            prefix.append(PADDING);
        }
        return prefix.toString();
    }

    private int randomNumber(int digits) {
        int min = (int) Math.pow(10, digits - 1);
        return min + random.nextInt(9 * min);
    }
}
