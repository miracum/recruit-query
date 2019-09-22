using System;

namespace Query.Models.Omop
{
#pragma warning disable CS1591
    public partial class Cohort
    {
        public int CohortDefinitionId { get; set; }

        public long SubjectId { get; set; }

        public DateTime CohortStartDate { get; set; }

        public DateTime CohortEndDate { get; set; }
    }
}
