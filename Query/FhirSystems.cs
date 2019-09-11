namespace Query
{
    /// <summary>
    /// FHIR URL systems.
    /// </summary>
    public static class FhirSystems
    {
        /// <summary>
        /// System for patient identifiers retrieved from the OMOP database.
        /// </summary>
        public static readonly string OmopSubjectIdentifier = "http://ohdsi.org/omop/fhir/patient-identifier";

        /// <summary>
        /// System for the cohort identifier as part of the screening list.
        /// </summary>
        public static readonly string ScreeningListCohortIdentifier = "http://miracum.org/fhir/screening-list-cohort-identifier";

        /// <summary>
        /// System for coding a List resource as a screening list.
        /// </summary>
        public static readonly string ScreeningListCodingSystem = "http://miracum.org/fhir/CodeSystem/screening-list";
    }
}
