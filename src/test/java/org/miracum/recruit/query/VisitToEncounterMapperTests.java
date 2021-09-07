package org.miracum.recruit.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miracum.recruit.query.models.CareSite;
import org.miracum.recruit.query.models.VisitDetail;
import org.miracum.recruit.query.models.VisitOccurrence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {FhirSystems.class})
@EnableConfigurationProperties(value = {FhirSystems.class})
class VisitToEncounterMapperTests {

  private VisitToEncounterMapper mapper;
  private Reference patientReference;

  @Autowired private FhirSystems fhirSystems;

  private static List<Encounter> getEncountersFromListOfBundleEntries(
      Collection<BundleEntryComponent> entries) {
    return entries.stream().map(e -> (Encounter) e.getResource()).collect(Collectors.toList());
  }

  @BeforeEach
  void setUp() {
    this.mapper = new VisitToEncounterMapper(fhirSystems);
    this.patientReference = new Reference();
  }

  @Test
  void map_withEmptyVisitOccurrenceList_shouldReturnEmptyList() {
    var result = mapper.map(Collections.emptyList(), patientReference);

    assertThat(result).isEmpty();
  }

  @Test
  void map_withNullVisitOccurrenceList_shouldReturnEmptyList() {
    var result = mapper.map(null, patientReference);

    assertThat(result).isEmpty();
  }

  @Test
  void map_withVisitTypeSetToStillPatient_shouldNotSetEndDateOfAnyEncounter() {
    // Still patient = 32220
    var vd =
        VisitDetail.builder()
            .visitDetailStartDate(LocalDate.now())
            .visitDetailEndDate(LocalDate.now())
            .visitDetailTypeConceptId(32220)
            .build();
    var vo =
        VisitOccurrence.builder()
            .visitSourceValue("1")
            .visitStartDate(LocalDate.now())
            .visitEndDate(LocalDate.now())
            .visitTypeConceptId(32220)
            .visitDetails(Set.of(vd))
            .build();

    var result = mapper.map(List.of(vo), patientReference);

    var encounters = getEncountersFromListOfBundleEntries(result);

    assertThat(encounters).hasSize(2);

    for (var encounter : encounters) {
      assertThat(encounter.getPeriod().hasStart()).isTrue();
      assertThat(encounter.getPeriod().hasEnd()).isFalse();
    }
  }

  @Test
  void map_withEmptyVisitData_shouldReturnEmptyList() {
    var vd = VisitDetail.builder().build();
    var vo = VisitOccurrence.builder().visitDetails(Set.of(vd)).build();

    var result = mapper.map(List.of(vo), patientReference);

    assertThat(result).isEmpty();
  }

  @Test
  void map_withOutPatientVisitType_shouldSetEncounterClassToAmbulatory() {
    var vd = VisitDetail.builder().visitDetailTypeConceptId(9202).build();
    var vo =
        VisitOccurrence.builder()
            .visitSourceValue("1")
            .visitDetails(Set.of(vd))
            .visitConceptId(9202)
            .build();

    var result = mapper.map(List.of(vo), patientReference);
    var encounters = getEncountersFromListOfBundleEntries(result);

    for (var encounter : encounters) {
      assertThat(encounter.getClass_().getCode()).isEqualTo("AMB");
    }
  }

  @Test
  void map_withGivenCareSites_shouldSetEncounterLocationDisplayToCareSiteName() {
    var careSite1 = CareSite.builder().careSiteName("Test 1").build();
    var careSite2 = CareSite.builder().careSiteName("Test 2").build();
    var vd = VisitDetail.builder().careSite(careSite2).build();
    var vo =
        VisitOccurrence.builder()
            .careSite(careSite1)
            .visitSourceValue("1")
            .visitDetails(Set.of(vd))
            .build();

    var result = mapper.map(List.of(vo), patientReference);
    var encounters = getEncountersFromListOfBundleEntries(result);

    // depending on a specific ordering for the encounters is not great
    assertThat(encounters.get(0).getLocationFirstRep().getLocation().getDisplay())
        .isEqualTo(careSite1.getCareSiteName());
    assertThat(encounters.get(1).getLocationFirstRep().getLocation().getDisplay())
        .isEqualTo(careSite2.getCareSiteName());
  }

  @Test
  void map_withoutCareSite_shouldSetEncounterLocationDisplayToVisitDetailSourceValue() {
    var vd = VisitDetail.builder().visitDetailSourceValue("Test").build();
    var vo = VisitOccurrence.builder().visitSourceValue("1").visitDetails(Set.of(vd)).build();

    var result = mapper.map(List.of(vo), patientReference);
    var encounters = getEncountersFromListOfBundleEntries(result);

    assertThat(encounters.get(0).hasLocation()).isFalse();
    assertThat(encounters.get(1).getLocationFirstRep().getLocation().getDisplay())
        .isEqualTo(vd.getVisitDetailSourceValue());
  }

  @Test
  void
      map_withVisitDetailCareSiteAndSourceValue_shouldSetLocationReferenceDisplayToConcatenationOfBoth() {
    var careSite = CareSite.builder().careSiteName("Care Site").build();
    var vd = VisitDetail.builder().careSite(careSite).visitDetailSourceValue("CS").build();
    var vo = VisitOccurrence.builder().visitSourceValue("1").visitDetails(Set.of(vd)).build();

    var result = new ArrayList<>(mapper.map(List.of(vo), patientReference));
    var encounters = getEncountersFromListOfBundleEntries(result);

    var subEncounter = encounters.get(1);
    assertThat(subEncounter.getLocationFirstRep().getLocation().getDisplay())
        .isEqualTo("Care Site (CS)");
  }

  @Test
  void map_withVisitOccurrenceAndDetail_shouldSetSubEncounterPartOfToReferenceMainEncounter() {
    var vd = VisitDetail.builder().visitDetailSourceValue("Test").build();
    var vo = VisitOccurrence.builder().visitSourceValue("1").visitDetails(Set.of(vd)).build();

    var result = new ArrayList<>(mapper.map(List.of(vo), patientReference));

    var mainEncounterFullUrl = result.get(0).getFullUrl();
    var subEncounter = (Encounter) result.get(1).getResource();
    assertThat(subEncounter.getPartOf().getReference()).isEqualTo(mainEncounterFullUrl);
  }
}
