package org.miracum.recruit.query;

import ca.uhn.fhir.context.FhirContext;
import org.junit.Test;
import org.miracum.recruit.query.models.CohortDefinition;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FhirCohortTransactionBuilderTests {
    @Test
    public void buildFromOmopCohort_withGivenSubjectIds_shouldCreatePatientForEachId() {
        var systems = new FhirSystems();
        var cohort = new CohortDefinition();
        var ids = List.of(1L, 2L, 3L, 4L);

        var sut = new FhirCohortTransactionBuilder(systems);

        var fhirTrx = sut.buildFromOmopCohort(cohort, ids);

        // create a subject for each id and create one List and one ResearchStudy resource
        assertThat(fhirTrx.getEntry()).hasSize(2*ids.size() + 1 + 1);
    }
}
