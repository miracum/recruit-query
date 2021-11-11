package org.miracum.recruit.query;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResearchStudy;
import org.hl7.fhir.r4.model.ResearchSubject;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.miracum.recruit.query.models.CohortDefinition;
import org.miracum.recruit.query.models.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;

@SpringBootTest(classes = {FhirSystems.class})
@EnableConfigurationProperties(value = {FhirSystems.class})
class FhirCohortTransactionBuilderTests {

  private static final FhirContext fhirContext = FhirContext.forR4();

  private final FhirSystems systems;
  private final FhirCohortTransactionBuilder sut;

  private final CohortDefinition testCohort;

  @Autowired
  public FhirCohortTransactionBuilderTests(FhirSystems fhirSystems) {
    this.systems = fhirSystems;
    var mapper = new VisitToEncounterMapper(fhirSystems);
    sut = new FhirCohortTransactionBuilder(fhirSystems, 100, false, mapper);
    testCohort = new CohortDefinition();
    testCohort.setId(1L);
    testCohort.setName("Testcohort");
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

    var lists = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ListResource.class);
    assertThat(lists).hasSize(1);
    var list = lists.get(0);
    assertThat(list.hasExtension(systems.getScreeningListStudyReferenceExtension())).isTrue();
    var acronymFromList =
        ((Reference)
                list.getExtensionByUrl(systems.getScreeningListStudyReferenceExtension())
                    .getValue())
            .getDisplay();
    assertThat(acronymFromList).isEqualTo("testacronym");
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

  @Test
  void buildFromOmopCohort_withCohortSizeThreshold_shouldAddNoteToTransaction() {
    var persons = new ArrayList<Person>();

    for (int i = 0; i < 100; i++) {
      var person =
          Person.builder()
              .personId(i + 1L)
              .yearOfBirth(Year.of(1900 + i))
              .monthOfBirth(Month.FEBRUARY)
              .dayOfBirth(1)
              .gender("Female")
              .build();
      persons.add(person);
    }

    var fhirTrx = sut.buildFromOmopCohort(testCohort, persons, 101);

    var lists = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ListResource.class);
    assertThat(lists).hasSize(1);

    var list = lists.get(0);
    assertThat(list.hasNote()).isTrue();
    assertThat(list.getNoteFirstRep().getText()).contains("101");
    assertThat(list.getEntry()).hasSameSizeAs(persons);
  }

  @Test
  void buildFromOmopCohort_withGivenPersons_shouldCreateExpectedNumberOfResourcesInTransaction() {
    var pers1 =
        Person.builder()
            .personId(1L)
            .yearOfBirth(Year.of(1993))
            .monthOfBirth(Month.AUGUST)
            .dayOfBirth(10)
            .build();
    var pers2 = Person.builder().personId(2L).yearOfBirth(Year.of(1976)).build();

    var persons = List.of(pers1, pers2);

    var fhirTrx = sut.buildFromOmopCohort(testCohort, persons, 100);

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
    var fhirTrx = sut.buildFromOmopCohort(testCohort, List.of(), 100);
    var studies = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ResearchStudy.class);
    var study = studies.get(0);
    assertThat(study.getMeta().getSource()).isEqualTo(systems.getStudySource());
  }

  @Test
  void buildFromOmopCohort_withGivenPersons_shouldCreateDeviceResource() {
    var fhirTrx = sut.buildFromOmopCohort(testCohort, List.of(), 100);
    var studies = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, Device.class);
    assertThat(studies).hasSize(1);
    var device = studies.get(0);
    assertThat(device.getIdentifierFirstRep().getValue()).startsWith("query-");
  }

  @Test
  void buildFromOmopCohort_withoutAcronymTag_shouldSetAcronymToCohortName() {
    var cohort = new CohortDefinition();
    cohort.setId(4L);
    cohort.setName("Testcohorte");
    cohort.setDescription("This is a description");

    var person = Person.builder().personId(2L).build();
    var fhirTrx = sut.buildFromOmopCohort(cohort, List.of(person), 100);
    var studies = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ResearchStudy.class);
    assertThat(studies).hasSize(1);

    var study = studies.get(0);
    var acronym =
        (StringType) study.getExtensionByUrl(systems.getResearchStudyAcronym()).getValue();
    assertThat(acronym.getValue()).isEqualTo(cohort.getName());
  }

  @Test
  void buildFromOmopCohort_withPersonsWithoutSourceId_shouldntCreateAsIdentifier() {
    var person =
        Person.builder()
            .personId(2L)
            .yearOfBirth(Year.of(1976))
            .monthOfBirth(Month.FEBRUARY)
            .dayOfBirth(12)
            .gender("Female")
            .build();
    var fhirTrx = sut.buildFromOmopCohort(testCohort, List.of(person), 100);
    var ids =
        BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, Patient.class)
            .get(0)
            .getIdentifier();
    assertThat(ids).hasSizeLessThan(2);
  }

  @Test
  void buildFromOmopCohort_withPersonsWithSourceId_shouldCreateAsIdentifier() {
    var person =
        Person.builder()
            .personId(2L)
            .yearOfBirth(Year.of(1976))
            .monthOfBirth(Month.FEBRUARY)
            .dayOfBirth(12)
            .gender("Female")
            .sourceId("1")
            .build();
    var fhirTrx = sut.buildFromOmopCohort(testCohort, List.of(person), 100);
    var ids =
        BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, Patient.class)
            .get(0)
            .getIdentifier();
    assertThat(ids).hasSizeGreaterThan(1);
    assertThat(ids.get(0).getValue()).isEqualTo("1");
  }

  @Test
  void buildFromOmopCohort_withPersonWithBirthdate_shouldCreatePatientWithSameBirthDate() {
    var person =
        Person.builder()
            .personId(2L)
            .yearOfBirth(Year.of(1976))
            .monthOfBirth(Month.FEBRUARY)
            .dayOfBirth(12)
            .gender("Female")
            .build();
    var persons = List.of(person);

    var fhirTrx = sut.buildFromOmopCohort(testCohort, persons, 100);

    var patients = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, Patient.class);

    var patient = patients.get(0);

    assertThat(patient.getBirthDateElement().getYear()).isEqualTo(1976);
    assertThat(patient.getBirthDateElement().getMonth()).isEqualTo(1); // the month is   0-based...
    assertThat(patient.getBirthDateElement().getDay()).isEqualTo(12);
  }

  @Test
  void
      buildFromOmopCohort_withPersonWithJustTheBirthYear_shouldCreatePatientWithJustTheBirthYear() {
    var person = Person.builder().personId(2L).yearOfBirth(Year.of(1976)).build();
    var persons = List.of(person);

    var fhirTrx = sut.buildFromOmopCohort(testCohort, persons, 100);

    var patients = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, Patient.class);

    var patient = patients.get(0);

    // 1975-12-31T00:00:00
    assertThat(patient.getBirthDateElement().getYear()).isEqualTo(1975);
    assertThat(patient.getBirthDateElement().getMonth()).isEqualTo(11);
    assertThat(patient.getBirthDateElement().getDay()).isEqualTo(31);
    assertThat(patient.getBirthDateElement().getHour()).isZero();
    assertThat(patient.getBirthDateElement().getMinute()).isZero();
  }

  @Test
  void
      buildFromOmopCohort_withEmptyPreviousListOfSubjects_shouldCreateListWithJustTheNewPatients() {
    var persons =
        List.of(
            Person.builder().personId(1L).yearOfBirth(Year.of(2001)).build(),
            Person.builder().personId(2L).yearOfBirth(Year.of(2002)).build());

    var fhirTrx =
        sut.buildFromOmopCohort(testCohort, persons, 100, Pair.of(new ListResource(), List.of()));

    var lists = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ListResource.class);

    assertThat(lists).hasSize(1);

    var list = lists.get(0);

    assertThat(list.getEntry()).hasSameSizeAs(persons);
  }

  @Test
  void
      buildFromOmopCohort_withGivenPreviousScreeningListAndCohortContainingPreviousPersonAndANewPerson_shouldCreateListCotainingPreviousAndNewSubjects() {
    // previousPerson and previousPatient are the same since their identifier is identical:
    // source_value=1 is used as an identifier for the Patient.
    var previousPatient =
        new Patient()
            .setIdentifier(
                List.of(new Identifier().setSystem(systems.getPatientId()).setValue("1")));
    var previousPerson =
        Person.builder().personId(1L).sourceId("1").yearOfBirth(Year.of(2001)).build();

    var newPerson = Person.builder().personId(2L).yearOfBirth(Year.of(2002)).build();

    var persons = List.of(newPerson, previousPerson);

    var previousList = new ListResource();
    var previousEntry = new Reference("ResearchSubject/previous-persons-research-subject");
    previousList.addEntry().setItem(previousEntry);

    var fhirTrx =
        sut.buildFromOmopCohort(
            testCohort, persons, 100, Pair.of(previousList, List.of(previousPatient)));

    var lists = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ListResource.class);

    assertThat(lists).hasSize(1);

    var list = lists.get(0);

    // there should only be previousPerson + newPerson in the new list
    assertThat(list.getEntry()).hasSameSizeAs(persons);
    assertThat(list.getEntry()).anyMatch(entry -> entry.getItem().equals(previousEntry));
  }

  @Test
  void buildFromOmopCohort_withNoChangesToPreviousScreeningList_shouldNotAddTheListToTransaction() {
    var persons =
        List.of(
            Person.builder().personId(1L).sourceId("1").yearOfBirth(Year.of(2001)).build(),
            Person.builder().personId(2L).sourceId("2").yearOfBirth(Year.of(2002)).build());

    var previousPatients =
        List.of(
            new Patient()
                .setIdentifier(
                    List.of(new Identifier().setSystem(systems.getPatientId()).setValue("1"))),
            new Patient()
                .setIdentifier(
                    List.of(new Identifier().setSystem(systems.getPatientId()).setValue("2"))));

    var previousList = new ListResource();
    previousList.addEntry().setItem(new Reference("1"));
    previousList.addEntry().setItem(new Reference("2"));

    var fhirTrx =
        sut.buildFromOmopCohort(testCohort, persons, 100, Pair.of(previousList, previousPatients));

    var lists = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ListResource.class);

    assertThat(lists).isEmpty();
  }
}
