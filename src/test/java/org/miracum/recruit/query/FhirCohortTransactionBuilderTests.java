package org.miracum.recruit.query;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.miracum.recruit.query.models.CohortDefinition;
import org.miracum.recruit.query.models.OmopPerson;

import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FhirCohortTransactionBuilderTests {

    private final FhirSystems systems = new FhirSystems();
    private final FhirContext fhirContext = FhirContext.forR4();
    private final int MAXSIZE = 10;
    private FhirCohortTransactionBuilder sut = new FhirCohortTransactionBuilder(systems, MAXSIZE);

    public FhirCohortTransactionBuilderTests() {
        systems.setResearchStudyAcronym("https://fhir.miracum.org/uc1/StructureDefinition/studyAcronym");
        systems.setStudySource("https://fhir.miracum.org/uc1/recruit#generatedByQueryModule");
    }

    @Test
    public void buildFromOmopCohort_withGivenPersons_shouldCreateExpectedNumberOfResourcesInTransaction() {
        var cohort = new CohortDefinition();
        cohort.setId(4L);
        cohort.setName("Testcohort");

        var pers1 = new OmopPerson()
                .setPersonId(1)
                .setYearOfBirth(Year.of(1993))
                .setMonthOfBirth(Month.AUGUST)
                .setDayOfBirth(10);
        var pers2 = new OmopPerson()
                .setPersonId(2)
                .setYearOfBirth(Year.of(1976));

        var persons = List.of(pers1, pers2);

        var fhirTrx = sut.buildFromOmopCohort(cohort, persons);

        // one Patient resource for each OmopPerson
        var patients = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, Patient.class);
        assertThat(patients).hasSize(2);

        // one ResearchSubject resource for each OmopPerson
        var researchSubjects = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ResearchSubject.class);
        assertThat(researchSubjects).hasSize(2);

        // one ResearchStudy, the "Testcohort"
        var studies = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ResearchStudy.class);
        assertThat(studies).hasSize(1);

        // one List
        var lists = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ListResource.class);
        assertThat(lists).hasSize(1);
    }

    @Test
    public void buildFromOmopCohort_withPersonWithBirthdate_shouldCreatePatientWithSameBirthDate() {
        var cohort = new CohortDefinition();
        cohort.setId(4L);
        cohort.setName("Testcohort");
        var person = new OmopPerson()
                .setPersonId(2)
                .setYearOfBirth(Year.of(1976))
                .setMonthOfBirth(Month.FEBRUARY)
                .setDayOfBirth(12)
                .setGender("Female");
        var persons = List.of(person);

        var fhirTrx = sut.buildFromOmopCohort(cohort, persons);

        var patients = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, Patient.class);

        var patient = patients.get(0);

        assertThat(patient.getBirthDateElement().getYear()).isEqualTo(1976);
        assertThat(patient.getBirthDateElement().getMonth()).isEqualTo(1); // the month is 0-based...
        assertThat(patient.getBirthDateElement().getDay()).isEqualTo(12);
    }

    @Test
    public void buildFromOmopCohort_withPersonWithJustTheBirthYear_shouldCreatePatientWithJustTheBirthYear() {
        var cohort = new CohortDefinition();
        cohort.setId(4L);
        cohort.setName("Testcohort");
        var person = new OmopPerson()
                .setPersonId(2)
                .setYearOfBirth(Year.of(1976));
        var persons = List.of(person);

        var fhirTrx = sut.buildFromOmopCohort(cohort, persons);

        var patients = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, Patient.class);

        var patient = patients.get(0);

        // 1975-12-31T00:00:00
        assertThat(patient.getBirthDateElement().getYear()).isEqualTo(1975);
        assertThat(patient.getBirthDateElement().getMonth()).isEqualTo(11);
        assertThat(patient.getBirthDateElement().getDay()).isEqualTo(31);
        assertThat(patient.getBirthDateElement().getHour()).isEqualTo(0);
        assertThat(patient.getBirthDateElement().getMinute()).isEqualTo(0);
    }

    @Test
    public void buildFromOmopCohort_withCohortDefinitionWithName_shouldSetStudyAcronymToCohortDefinitionName() {
        var cohort = new CohortDefinition();
        cohort.setId(4L);
        cohort.setName("Testcohort");

        var person = new OmopPerson()
        		.setPersonId(2);
        var fhirTrx = sut.buildFromOmopCohort(cohort, List.of(person));
        var studies = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ResearchStudy.class);
        assertThat(studies).hasSize(1);

        var study = studies.get(0);
        assertThat(study.hasExtension(systems.getResearchStudyAcronym())).isTrue();

        var acronym = (StringType) study.getExtensionByUrl(systems.getResearchStudyAcronym()).getValue();
        assertThat(acronym.getValue()).isEqualTo(cohort.getName());
    }

    @Test
    public void buildFromOmopCohort_withGivenPersons_shouldHaveMetaSource() {
        var cohort = new CohortDefinition();
        cohort.setId(4L);
        cohort.setName("Testcohort");

        var fhirTrx = sut.buildFromOmopCohort(cohort, List.of());
        var studies = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ResearchStudy.class);
        var study = studies.get(0);
        assertThat(study.getMeta().getSource()).isEqualTo(systems.getStudySource());
    }

    @Test
    public void buildFromOmopCohort_withCohortDefinitionWithLabels_shouldStripLabelsInStudyTitleAndDescription() {
        var cohort = new CohortDefinition();
        cohort.setId(1L);
        cohort.setName("[Any Label] Testcohort");
        cohort.setDescription("[UC1] [two labels] A Description");

        var fhirTrx = sut.buildFromOmopCohort(cohort, List.of());
        var studies = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ResearchStudy.class);
        var study = studies.get(0);
        assertThat(study.getTitle()).isEqualTo("Testcohort");
        assertThat(study.getDescription()).isEqualTo("A Description");
    }

    public void buildFromOmopCohort_withCohortSizeThreshold_shouldAddNoteToTransaction() {
    	//this.sut.setMaxListSize(10);
    	var cohort = new CohortDefinition();
        cohort.setId(4L);
        cohort.setName("Testcohort");
        var persons = new ArrayList<OmopPerson>();

        for (int i = 0; i<15; i++) {
            var person = new OmopPerson()
                    .setPersonId(i+1)
                    .setYearOfBirth(Year.of(1976 + i))
                    .setMonthOfBirth(Month.FEBRUARY)
                    .setDayOfBirth(12 + i)
                    .setGender("Female");
            persons.add(person);
        }

        var fhirTrx = sut.buildFromOmopCohort(cohort, persons);

        var lists = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, ListResource.class);
        assertThat(lists).hasSize(1);

        var list = lists.get(0);
        //assertThat(list.hasNote());
        assertThat(list).hasFieldOrProperty("note");
        assertThat(list.getNoteFirstRep().getText()).contains(""+MAXSIZE);
        assertThat(list.getNoteFirstRep().getText()).contains("15");
        assertThat(list.getEntry()).hasSize(MAXSIZE);
    }

}
