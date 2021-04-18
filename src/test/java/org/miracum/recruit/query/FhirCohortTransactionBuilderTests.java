package org.miracum.recruit.query;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResearchStudy;
import org.hl7.fhir.r4.model.ResearchSubject;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.miracum.recruit.query.models.CohortDefinition;
import org.miracum.recruit.query.models.Person;

class FhirCohortTransactionBuilderTests {

  private final FhirSystems systems = new FhirSystems();
  private final FhirContext fhirContext = FhirContext.forR4();
  private final int MAXSIZE = 10;
  private final FhirCohortTransactionBuilder sut =
      new FhirCohortTransactionBuilder(
          systems, MAXSIZE, false, new VisitToEncounterMapper(systems));

  // TODO: refactor to auto-wire dependencies
  public FhirCohortTransactionBuilderTests() {
    systems.setResearchStudyAcronym(
        "https://fhir.miracum.org/uc1/StructureDefinition/studyAcronym");
    systems.setStudySource("https://fhir.miracum.org/uc1/recruit#generatedByQueryModule");
  }

  @Test
  void buildFromOmopCohort_withAcronymTagInName_shouldSetStudyAcronymFromName() {
    var cohort = new CohortDefinition();
    cohort.setId(4L);
    cohort.setName("Testcohort [acronym=testacronym]");
    cohort.setDescription("This is a description");

    var person = Person.builder().personId(2L).build();
    var fhirTrx = sut.buildFromOmopCohort(cohort, List.of(person), 100);
    var studies = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ResearchStudy.class);
    assertThat(studies).hasSize(1);

    var study = studies.get(0);
    assertThat(study.hasExtension(systems.getResearchStudyAcronym())).isTrue();

    var acronym =
        (StringType) study.getExtensionByUrl(systems.getResearchStudyAcronym()).getValue();
    assertThat(acronym.getValue()).isEqualTo("testacronym");
  }

  @Test
  void
      buildFromOmopCohort_withCohortDefinitionWithLabels_shouldStripLabelsInStudyTitleAndDescription() {
    var cohort = new CohortDefinition();
    cohort.setId(1L);
    cohort.setName("[Any Label] Testcohort");
    cohort.setDescription("[UC1] [two labels] A Description");

    var fhirTrx = sut.buildFromOmopCohort(cohort, List.of(), 100);
    var studies = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ResearchStudy.class);
    var study = studies.get(0);
    assertThat(study.getTitle()).isEqualTo("Testcohort");
    assertThat(study.getDescription()).isEqualTo("A Description");
  }

  @Test
  void buildFromOmopCohort_withCohortDefinitionWithName_shouldSetStudyAcronymFromDescription() {
    var cohort = new CohortDefinition();
    cohort.setId(4L);
    cohort.setName("Testcohort");
    cohort.setDescription("[acronym=testacronym]");

    var person = Person.builder().personId(2L).build();
    var fhirTrx = sut.buildFromOmopCohort(cohort, List.of(person), 100);
    var studies = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ResearchStudy.class);
    assertThat(studies).hasSize(1);

    var study = studies.get(0);
    assertThat(study.hasExtension(systems.getResearchStudyAcronym())).isTrue();

    var acronym =
        (StringType) study.getExtensionByUrl(systems.getResearchStudyAcronym()).getValue();
    assertThat(acronym.getValue()).isEqualTo("testacronym");
  }

  void buildFromOmopCohort_withCohortSizeThreshold_shouldAddNoteToTransaction() {
    // this.sut.setMaxListSize(10);
    var cohort = new CohortDefinition();
    cohort.setId(4L);
    cohort.setName("Testcohort");
    var persons = new ArrayList<Person>();

    for (int i = 0; i < 15; i++) {
      var person =
          Person.builder()
              .personId(i + 1L)
              .yearOfBirth(Year.of(1976 + i))
              .monthOfBirth(Month.FEBRUARY)
              .dayOfBirth(12 + i)
              .gender("Female")
              .build();
      persons.add(person);
    }

    var fhirTrx = sut.buildFromOmopCohort(cohort, persons, 100);

    var lists = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ListResource.class);
    assertThat(lists).hasSize(1);

    var list = lists.get(0);
    // assertThat(list.hasNote());
    assertThat(list).hasFieldOrProperty("note");
    assertThat(list.getNoteFirstRep().getText()).contains("" + 100);
    assertThat(list.getNoteFirstRep().getText()).contains("15");
    assertThat(list.getEntry()).hasSize(15);
  }

  @Test
  void buildFromOmopCohort_withGivenPersons_shouldCreateExpectedNumberOfResourcesInTransaction() {
    var cohort = new CohortDefinition();
    cohort.setId(4L);
    cohort.setName("Testcohort");

    var pers1 =
        Person.builder()
            .personId(1L)
            .yearOfBirth(Year.of(1993))
            .monthOfBirth(Month.AUGUST)
            .dayOfBirth(10)
            .build();
    var pers2 = Person.builder().personId(2L).yearOfBirth(Year.of(1976)).build();

    var persons = List.of(pers1, pers2);

    var fhirTrx = sut.buildFromOmopCohort(cohort, persons, 100);

    // one Patient resource for each OmopPerson
    var patients = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, Patient.class);
    assertThat(patients).hasSize(2);

    // one ResearchSubject resource for each OmopPerson
    var researchSubjects =
        BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ResearchSubject.class);
    assertThat(researchSubjects).hasSize(2);

    // one ResearchStudy, the "Testcohort"
    var studies = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ResearchStudy.class);
    assertThat(studies).hasSize(1);

    // one List
    var lists = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ListResource.class);
    assertThat(lists).hasSize(1);
  }

  @Test
  void buildFromOmopCohort_withGivenPersons_shouldHaveMetaSource() {
    var cohort = new CohortDefinition();
    cohort.setId(4L);
    cohort.setName("Testcohort");

    var fhirTrx = sut.buildFromOmopCohort(cohort, List.of(), 100);
    var studies = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ResearchStudy.class);
    var study = studies.get(0);
    assertThat(study.getMeta().getSource()).isEqualTo(systems.getStudySource());
  }

  @Test
  void buildFromOmopCohort_withoutAcronymTag_shouldNotSetAcronym() {
    var cohort = new CohortDefinition();
    cohort.setId(4L);
    cohort.setName("Testcohorte");
    cohort.setDescription("This is a description");

    var person = Person.builder().personId(2L).build();
    var fhirTrx = sut.buildFromOmopCohort(cohort, List.of(person), 100);
    var studies = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ResearchStudy.class);
    assertThat(studies).hasSize(1);

    var study = studies.get(0);
    assertThat(study.hasExtension(systems.getResearchStudyAcronym())).isFalse();
  }

  @Test
  void buildFromOmopCohort_withPersonsWithoutSourceId_shouldntCreateAsIdentifier() {
    var cohort = new CohortDefinition();
    cohort.setId(5L);
    cohort.setName("Testkohorte");
    var person =
        Person.builder()
            .personId(2L)
            .yearOfBirth(Year.of(1976))
            .monthOfBirth(Month.FEBRUARY)
            .dayOfBirth(12)
            .gender("Female")
            .build();
    var fhirTrx = sut.buildFromOmopCohort(cohort, List.of(person), 100);
    var ids =
        BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, Patient.class)
            .get(0)
            .getIdentifier();
    assertThat(ids).hasSizeLessThan(2);
  }

  @Test
  void buildFromOmopCohort_withPersonsWithSourceId_shouldCreateAsIdentifier() {
    var cohort = new CohortDefinition();
    cohort.setId(5L);
    cohort.setName("Testkohorte");
    var person =
        Person.builder()
            .personId(2L)
            .yearOfBirth(Year.of(1976))
            .monthOfBirth(Month.FEBRUARY)
            .dayOfBirth(12)
            .gender("Female")
            .sourceId("1")
            .build();
    var fhirTrx = sut.buildFromOmopCohort(cohort, List.of(person), 100);
    var ids =
        BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, Patient.class)
            .get(0)
            .getIdentifier();
    assertThat(ids).hasSizeGreaterThan(1);
    assertThat(ids.get(0).getValue()).isEqualTo("1");
  }

  @Test
  void buildFromOmopCohort_withPersonWithBirthdate_shouldCreatePatientWithSameBirthDate() {
    var cohort = new CohortDefinition();
    cohort.setId(4L);
    cohort.setName("Testcohort");
    var person =
        Person.builder()
            .personId(2L)
            .yearOfBirth(Year.of(1976))
            .monthOfBirth(Month.FEBRUARY)
            .dayOfBirth(12)
            .gender("Female")
            .build();
    var persons = List.of(person);

    var fhirTrx = sut.buildFromOmopCohort(cohort, persons, 100);

    var patients = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, Patient.class);

    var patient = patients.get(0);

    assertThat(patient.getBirthDateElement().getYear()).isEqualTo(1976);
    assertThat(patient.getBirthDateElement().getMonth()).isEqualTo(1); // the month is 0-based...
    assertThat(patient.getBirthDateElement().getDay()).isEqualTo(12);
  }

  @Test
  void
      buildFromOmopCohort_withPersonWithJustTheBirthYear_shouldCreatePatientWithJustTheBirthYear() {
    var cohort = new CohortDefinition();
    cohort.setId(4L);
    cohort.setName("Testcohort");
    var person = Person.builder().personId(2L).yearOfBirth(Year.of(1976)).build();
    var persons = List.of(person);

    var fhirTrx = sut.buildFromOmopCohort(cohort, persons, 100);

    var patients = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, Patient.class);

    var patient = patients.get(0);

    // 1975-12-31T00:00:00
    assertThat(patient.getBirthDateElement().getYear()).isEqualTo(1975);
    assertThat(patient.getBirthDateElement().getMonth()).isEqualTo(11);
    assertThat(patient.getBirthDateElement().getDay()).isEqualTo(31);
    assertThat(patient.getBirthDateElement().getHour()).isEqualTo(0);
    assertThat(patient.getBirthDateElement().getMinute()).isEqualTo(0);
  }
}
