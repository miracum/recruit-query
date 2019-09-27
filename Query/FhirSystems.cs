namespace Query
{
    /// <summary>
    /// FHIR URL systems.
    /// </summary>
    public static class FhirSystems
    {
        /// <summary>
        /// System for subject identifiers retrieved from the OMOP database.
        /// </summary>
        public static readonly string OmopSubjectIdentifier = "http://ohdsi.org/omop/fhir/subject-identifier";

        /// <summary>
        /// System for identifying cohorts within OMOP.
        /// </summary>
        public static readonly string OmopCohortIdentifier = "http://ohdsi.org/omop/fhir/cohort-identifier";

        /// <summary>
        /// System for the cohort identifier as part of the screening list.
        /// </summary>
        public static readonly string ScreeningListCohortIdentifier = "http://miracum.org/fhir/screening-list-cohort-identifier";

        /// <summary>
        /// System for coding a List resource as a screening list.
        /// </summary>
        public static readonly string ScreeningListCodingSystem = "http://miracum.org/fhir/CodeSystem/screening-list";

        /// <summary>
        /// Extension system for referencing a researchstudy from a screening list.
        /// </summary>
        public static readonly string ScreeningListStudyReference = "http://miracum.org/fhir/recommendation-list-study-reference";
    }
}
