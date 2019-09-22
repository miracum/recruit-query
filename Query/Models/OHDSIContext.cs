using Microsoft.EntityFrameworkCore;

namespace Query.Models.DB
{
    public partial class OHDSIContext : DbContext
    {
        public OHDSIContext()
        {
        }

        public OHDSIContext(DbContextOptions<OHDSIContext> options)
            : base(options)
        {
        }

        public virtual DbSet<Cohort> Cohort { get; set; }

        public virtual DbSet<Person> Person { get; set; }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            modelBuilder.Entity<Cohort>(entity =>
            {
                entity.HasNoKey();

                entity.ToTable("cohort", "synpuf_results");

                entity.Property(e => e.CohortDefinitionId).HasColumnName("cohort_definition_id");

                entity.Property(e => e.CohortEndDate)
                    .HasColumnName("cohort_end_date")
                    .HasColumnType("date");

                entity.Property(e => e.CohortStartDate)
                    .HasColumnName("cohort_start_date")
                    .HasColumnType("date");

                entity.Property(e => e.SubjectId).HasColumnName("subject_id");
            });

            modelBuilder.Entity<Person>(entity =>
            {
                entity.ToTable("person", "synpuf_cdm");

                entity.HasIndex(e => e.PersonId)
                    .HasName("idx_person_id")
                    .IsUnique();

                entity.Property(e => e.PersonId)
                    .HasColumnName("person_id")
                    .ValueGeneratedNever();

                entity.Property(e => e.BirthDatetime).HasColumnName("birth_datetime");

                entity.Property(e => e.CareSiteId).HasColumnName("care_site_id");

                entity.Property(e => e.DayOfBirth).HasColumnName("day_of_birth");

                entity.Property(e => e.EthnicityConceptId).HasColumnName("ethnicity_concept_id");

                entity.Property(e => e.EthnicitySourceConceptId).HasColumnName("ethnicity_source_concept_id");

                entity.Property(e => e.EthnicitySourceValue)
                    .HasColumnName("ethnicity_source_value")
                    .HasMaxLength(50);

                entity.Property(e => e.GenderConceptId).HasColumnName("gender_concept_id");

                entity.Property(e => e.GenderSourceConceptId).HasColumnName("gender_source_concept_id");

                entity.Property(e => e.GenderSourceValue)
                    .HasColumnName("gender_source_value")
                    .HasMaxLength(50);

                entity.Property(e => e.LocationId).HasColumnName("location_id");

                entity.Property(e => e.MonthOfBirth).HasColumnName("month_of_birth");

                entity.Property(e => e.PersonSourceValue)
                    .HasColumnName("person_source_value")
                    .HasMaxLength(50);

                entity.Property(e => e.ProviderId).HasColumnName("provider_id");

                entity.Property(e => e.RaceConceptId).HasColumnName("race_concept_id");

                entity.Property(e => e.RaceSourceConceptId).HasColumnName("race_source_concept_id");

                entity.Property(e => e.RaceSourceValue)
                    .HasColumnName("race_source_value")
                    .HasMaxLength(50);

                entity.Property(e => e.YearOfBirth).HasColumnName("year_of_birth");
            });

            OnModelCreatingPartial(modelBuilder);
        }

        partial void OnModelCreatingPartial(ModelBuilder modelBuilder);
    }
}
