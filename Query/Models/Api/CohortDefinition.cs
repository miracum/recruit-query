using System;

namespace Query.Models.Api
{
#pragma warning disable CS1591
    public class CohortDefinition
    {
        public int Id { get; set; }
        public string Name { get; set; }
        public string Description { get; set; }
        public string ExpressionType { get; set; }
        public string CreatedBy { get; set; }
        public DateTimeOffset? CreatedDate { get; set; }
        public string ModifiedBy { get; set; }
        public DateTimeOffset? ModifiedDate { get; set; }
        public string Expression { get; set; }
    }
}