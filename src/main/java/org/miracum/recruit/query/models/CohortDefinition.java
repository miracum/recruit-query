package org.miracum.recruit.query.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

public class CohortDefinition {

  private Long id;
  private String name;
  private String description;
  private String expressionType;
  private String createdBy;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+2")
  private Date createdDate;

  private String modifiedBy;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+2")
  private Date modifiedDate;

  private String expression;

  public String getCreatedBy() {
    return createdBy;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public String getDescription() {
    return description;
  }

  public String getExpression() {
    return expression;
  }

  public String getExpressionType() {
    return expressionType;
  }

  public Long getId() {
    return id;
  }

  public String getModifiedBy() {
    return modifiedBy;
  }

  public Date getModifiedDate() {
    return modifiedDate;
  }

  public String getName() {
    return name;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public void setExpressionType(String expressionType) {
    this.expressionType = expressionType;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  public void setModifiedDate(Date modifiedDate) {
    this.modifiedDate = modifiedDate;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "CohortDefinition{"
        + "id="
        + id
        + ", name='"
        + name
        + '\''
        + ", description='"
        + description
        + '\''
        + ", expressionType='"
        + expressionType
        + '\''
        + ", createdBy='"
        + createdBy
        + '\''
        + ", createdDate="
        + createdDate
        + ", modifiedBy='"
        + modifiedBy
        + '\''
        + ", modifiedDate="
        + modifiedDate
        + ", expression='"
        + expression
        + '\''
        + '}';
  }
}
