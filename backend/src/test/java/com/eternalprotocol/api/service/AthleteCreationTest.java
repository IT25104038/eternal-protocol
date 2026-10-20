package com.eternalprotocol.api.service;

import com.eternalprotocol.api.cache.AthleteCodeCache;
import com.eternalprotocol.api.dto.AthleteCodeValidationDto;
import com.eternalprotocol.api.dto.AthleteDto;
import com.eternalprotocol.api.dto.CreateAthleteRequestDto;
import com.eternalprotocol.api.entity.Athlete;
import com.eternalprotocol.api.exception.EmailAlreadyInUseException;
import com.eternalprotocol.api.repository.AthleteRepository;
import com.eternalprotocol.api.repository.CommissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for an admin creating an athlete with an automatically generated
 * athlete code or one typed in by hand
 */
@ExtendWith(MockitoExtension.class)
class AthleteCreationTest {

    @Mock
    private AthleteRepository athleteRepository;
    @Mock
    private CommissionRepository commissionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AthleteCodeGenerator athleteCodeGenerator;

    @InjectMocks
    private AthleteService athleteService;

    @BeforeEach
    void setUp() {

        AthleteCodeCache.getInstance().clear();
    }

    private CreateAthleteRequestDto request(String athleteCode) {
        return new CreateAthleteRequestDto(
                "Usain Bolt", "usain@test.com", "secret123", athleteCode, new BigDecimal("10.00"));
    }

    @Test
    void createAthlete_withoutACode_generatesOneFromTheName() {
        // no code typed in
        when(athleteRepository.existsByEmail("usain@test.com")).thenReturn(false);
        when(athleteCodeGenerator.generate("Usain Bolt")).thenReturn("USA472");
        when(passwordEncoder.encode("secret123")).thenReturn("HASHED");
        when(athleteRepository.save(any(Athlete.class))).thenAnswer(call -> call.getArgument(0));

        AthleteDto result = athleteService.createAthlete(request(null));

        assertEquals("USA472", result.athleteCode());
        ArgumentCaptor<Athlete> saved = ArgumentCaptor.forClass(Athlete.class);
        verify(athleteRepository).save(saved.capture());
        assertEquals("USA472", saved.getValue().getAthleteCode());
    }

    @Test
    void createAthlete_withABlankCode_alsoGeneratesOne() {
        // the admin left the field empty 
        when(athleteRepository.existsByEmail("usain@test.com")).thenReturn(false);
        when(athleteCodeGenerator.generate("Usain Bolt")).thenReturn("USA472");
        when(passwordEncoder.encode("secret123")).thenReturn("HASHED");
        when(athleteRepository.save(any(Athlete.class))).thenAnswer(call -> call.getArgument(0));

        AthleteDto result = athleteService.createAthlete(request("   "));

        assertEquals("USA472", result.athleteCode());
    }

    @Test
    void createAthlete_withATypedCode_usesItAndDoesNotGenerateOne() {
        // the admin typed their own code
        when(athleteRepository.existsByEmail("usain@test.com")).thenReturn(false);
        when(athleteRepository.existsByAthleteCodeIgnoreCase("LIGHTNING")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("HASHED");
        when(athleteRepository.save(any(Athlete.class))).thenAnswer(call -> call.getArgument(0));

        AthleteDto result = athleteService.createAthlete(request("  LIGHTNING "));

        assertEquals("LIGHTNING", result.athleteCode());
        verifyNoInteractions(athleteCodeGenerator);
    }

    @Test
    void createAthlete_withATypedCodeThatIsTaken_throwsAndSavesNothing() {
        when(athleteRepository.existsByEmail("usain@test.com")).thenReturn(false);
        when(athleteRepository.existsByAthleteCodeIgnoreCase("LIGHTNING")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> athleteService.createAthlete(request("LIGHTNING")));
        verify(athleteRepository, never()).save(any(Athlete.class));
        verifyNoInteractions(athleteCodeGenerator);
    }

    @Test
    void createAthlete_whenEmailIsAlreadyRegistered_throwsBeforeGeneratingAnything() {
        
        when(athleteRepository.existsByEmail("usain@test.com")).thenReturn(true);

        assertThrows(EmailAlreadyInUseException.class, () -> athleteService.createAthlete(request(null)));
        verifyNoInteractions(athleteCodeGenerator);
        verify(athleteRepository, never()).save(any(Athlete.class));
    }

    @Test
    void createAthlete_clearsAStaleNotFoundAnswerForTheNewCode() {
        // someone already tried the code at checkout before it existed
        AthleteCodeCache.getInstance().put("USA472", AthleteCodeValidationDto.invalid("USA472"));
        when(athleteRepository.existsByEmail("usain@test.com")).thenReturn(false);
        when(athleteCodeGenerator.generate("Usain Bolt")).thenReturn("USA472");
        when(passwordEncoder.encode("secret123")).thenReturn("HASHED");
        when(athleteRepository.save(any(Athlete.class))).thenAnswer(call -> call.getArgument(0));

        athleteService.createAthlete(request(null));

        assertTrue(AthleteCodeCache.getInstance().get("USA472").isEmpty());
    }
}
