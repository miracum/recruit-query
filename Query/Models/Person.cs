using System;

namespace Query.Models.DB
{
#pragma warning disable CS1591
    public partial class Person
    {
        public int PersonId { get; set; }

        public int GenderConceptId { get; set; }

        public int YearOfBirth { get; set; }

        public int? MonthOfBirth { get; set; }

        public int? DayOfBirth { get; set; }

        public DateTime? BirthDatetime { get; set; }

        public int RaceConceptId { get; set; }

        public int EthnicityConceptId { get; set; }

        public int? LocationId { get; set; }

        public int? ProviderId { get; set; }

        public int? CareSiteId { get; set; }

        public string PersonSourceValue { get; set; }

        public string GenderSourceValue { get; set; }

        public int? GenderSourceConceptId { get; set; }

        public string RaceSourceValue { get; set; }

        public int? RaceSourceConceptId { get; set; }

        public string EthnicitySourceValue { get; set; }

        public int? EthnicitySourceConceptId { get; set; }
    }
}
