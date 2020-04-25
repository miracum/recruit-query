package org.miracum.recruit.query;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResearchStudy;
import org.hl7.fhir.r4.model.ResearchSubject;
import org.junit.jupiter.api.Test;
import org.miracum.recruit.query.models.CohortDefinition;
import org.miracum.recruit.query.models.OmopPerson;

import java.time.Month;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FhirCohortTransactionBuilderTests {

    private final FhirSystems systems = new FhirSystems();
    private final FhirContext fhirContext = FhirContext.forR4();

    @Test
    public void buildFromOmopCohort_withGivenPersons_shouldCreateExpectedNumberOfResourcesInTransaction() {
        var cohort = new CohortDefinition();
        cohort.setId(4);
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

        var sut = new FhirCohortTransactionBuilder(systems);

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
        cohort.setId(4);
        cohort.setName("Testcohort");
        var person = new OmopPerson()
                .setPersonId(2)
                .setYearOfBirth(Year.of(1976))
                .setMonthOfBirth(Month.FEBRUARY)
                .setDayOfBirth(12)
                .setGender("Female");
        var persons = List.of(person);

        var sut = new FhirCohortTransactionBuilder(systems);
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
        cohort.setId(4);
        cohort.setName("Testcohort");
        var person = new OmopPerson()
                .setPersonId(2)
                .setYearOfBirth(Year.of(1976));
        var persons = List.of(person);

        var sut = new FhirCohortTransactionBuilder(systems);
        var fhirTrx = sut.buildFromOmopCohort(cohort, persons);

        var patients = BundleUtil.toListOfResourcesOfType(fhirContext, fhirTrx, Patient.class);

        var patient = patients.get(0);

        // assertThat(patient.getBirthDateElement().getYear()).isEqualTo(1976);
    }
}
