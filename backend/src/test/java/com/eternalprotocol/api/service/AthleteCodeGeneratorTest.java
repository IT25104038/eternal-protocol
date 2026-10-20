package com.eternalprotocol.api.service;

import com.eternalprotocol.api.repository.AthleteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for turning an athlete's name into a referral code 
 */

@ExtendWith(MockitoExtension.class)
class AthleteCodeGeneratorTest {

    @Mock
    private AthleteRepository athleteRepository;

    private AthleteCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new AthleteCodeGenerator(athleteRepository, new ZeroRandom());
    }

    private static class ZeroRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }

    @Test
    void prefixFrom_usesTheFirstThreeLettersInUpperCase() {
        assertEquals("USA", AthleteCodeGenerator.prefixFrom("Usain Bolt"));
    }

    @Test
    void prefixFrom_ignoresSpacesPunctuationAndNumbers() {
        assertEquals("ALB", AthleteCodeGenerator.prefixFrom("  al b-o 99"));
    }

    @Test
    void prefixFrom_removesAccents() {
        assertEquals("ZOE", AthleteCodeGenerator.prefixFrom("Zo\u00eb Smith"));
    }

    @Test
    void prefixFrom_padsShortNamesWithX() {
        assertEquals("LIX", AthleteCodeGenerator.prefixFrom("Li"));
    }

    @Test
    void prefixFrom_nameWithNoLatinLettersOrNoNameAtAll_becomesXXX() {
        assertEquals("XXX", AthleteCodeGenerator.prefixFrom("\u0DC3\u0DD2\u0D82\u0DC4\u0DBD"));
        assertEquals("XXX", AthleteCodeGenerator.prefixFrom("   "));
        assertEquals("XXX", AthleteCodeGenerator.prefixFrom(null));
    }

    @Test
    void generate_returnsPrefixPlusThreeDigitNumber() {

        when(athleteRepository.existsByAthleteCodeIgnoreCase(anyString())).thenReturn(false);

        String code = generator.generate("Usain Bolt");

        assertEquals("USA100", code);
    }

    @Test
    void generate_withRealRandomness_alwaysLooksLikeThreeLettersAndThreeDigits() {
        
        AthleteCodeGenerator realGenerator = new AthleteCodeGenerator(athleteRepository);
        when(athleteRepository.existsByAthleteCodeIgnoreCase(anyString())).thenReturn(false);

        for (int i = 0; i < 200; i++) {
            String code = realGenerator.generate("Usain Bolt");
            assertTrue(code.matches("USA[1-9][0-9]{2}"), "unexpected code: " + code);
        }
    }

    @Test
    void generate_whenTheCodeIsTaken_triesAgainUntilItFindsAFreeOne() {
        
        when(athleteRepository.existsByAthleteCodeIgnoreCase("USA100")).thenReturn(true, true, false);

        String code = generator.generate("Usain Bolt");

        assertEquals("USA100", code);
        verify(athleteRepository, times(3)).existsByAthleteCodeIgnoreCase("USA100");
    }

    @Test
    void generate_whenEveryThreeDigitCodeKeepsClashing_movesOnToFourDigits() {
        when(athleteRepository.existsByAthleteCodeIgnoreCase("USA100")).thenReturn(true);

        when(athleteRepository.existsByAthleteCodeIgnoreCase("USA1000")).thenReturn(false);

        String code = generator.generate("Usain Bolt");

        assertEquals("USA1000", code);
    }

    @Test
    void generate_whenNothingIsFree_givesUpWithAClearError() {
        when(athleteRepository.existsByAthleteCodeIgnoreCase(anyString())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> generator.generate("Usain Bolt"));
    }
}
