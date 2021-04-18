package org.miracum.recruit.query.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareSite {
  @Id private Long careSiteId;
  private String careSiteName;
  private Integer placeOfServiceConceptId;
  private Long locationId;
  private String careSiteSourceValue;
  private String placeOfServiceSourceValue;
}
