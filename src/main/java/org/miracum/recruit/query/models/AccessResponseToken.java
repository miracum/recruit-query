package org.miracum.recruit.query.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessResponseToken {

  @JsonAlias("access_token")
  String accessToken;
}
