package org.miracum.recruit.query.models;

import java.time.Month;
import java.time.Year;

public class OmopPerson {

  private int personId = 0;
  private String gender;
  private Year yearOfBirth;
  private Month monthOfBirth;
  private Integer dayOfBirth;
  private int locationId = 0;
  private String sourceId;

  public int getPersonId() {
    return personId;
  }

  public OmopPerson setPersonId(int personId) {
    this.personId = personId;
    return this;
  }

  public String getGender() {
    return gender;
  }

  public OmopPerson setGender(String gender) {
    this.gender = gender;
    return this;
  }

  public Year getYearOfBirth() {
    return yearOfBirth;
  }

  public OmopPerson setYearOfBirth(Year yearOfBirth) {
    this.yearOfBirth = yearOfBirth;
    return this;
  }

  public Month getMonthOfBirth() {
    return monthOfBirth;
  }

  public OmopPerson setMonthOfBirth(Month monthOfBirth) {
    this.monthOfBirth = monthOfBirth;
    return this;
  }

  public Integer getDayOfBirth() {
    return dayOfBirth;
  }

  public OmopPerson setDayOfBirth(Integer dayOfBirth) {
    this.dayOfBirth = dayOfBirth;
    return this;
  }

  public int getLocationId() {
    return locationId;
  }

  public OmopPerson setLocationId(int locationId) {
    this.locationId = locationId;
    return this;
  }

  @Override
  public String toString() {
    return "OmopPerson{"
        + "personId="
        + personId
        + ", gender='"
        + gender
        + '\''
        + ", yearOfBirth="
        + yearOfBirth
        + ", monthOfBirth="
        + monthOfBirth
        + ", dayOfBirth="
        + dayOfBirth
        + ", locationId="
        + locationId
        + '}';
  }

  public String getSourceId() {
    return sourceId;
  }

  public OmopPerson setSourceId(String sourceId) {
    this.sourceId = sourceId;
    return this;
  }
}
