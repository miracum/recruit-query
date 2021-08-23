package org.miracum.recruit.query.repositories;

import java.util.Set;
import org.miracum.recruit.query.models.VisitDetail;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface VisitDetailRepository extends PagingAndSortingRepository<VisitDetail, Long> {
  @Query(
      "SELECT visit_detail.*, care_site.* "
          + "FROM visit_detail "
          + "         JOIN care_site ON visit_detail.care_site_id = care_site.care_site_id "
          + "WHERE visit_detail.visit_occurrence_id = :visitOccurrenceId "
          + "ORDER BY visit_detail_start_date DESC "
          + "LIMIT 5")
  Set<VisitDetail> findTop5ByVisitOccurrenceIdOrderByVisitDetailStartDateDesc(
      Long visitOccurrenceId);
}