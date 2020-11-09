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

  public Integer getDayOfBirth() {
    return dayOfBirth;
  }

  public String getGender() {
    return gender;
  }

  public int getLocationId() {
    return locationId;
  }

  public Month getMonthOfBirth() {
    return monthOfBirth;
  }

  public int getPersonId() {
    return personId;
  }

  public String getSourceId() {
    return sourceId;
  }

  public Year getYearOfBirth() {
    return yearOfBirth;
  }

  public OmopPerson setDayOfBirth(Integer dayOfBirth) {
    this.dayOfBirth = dayOfBirth;
    return this;
  }

  public OmopPerson setGender(String gender) {
    this.gender = gender;
    return this;
  }

  public OmopPerson setLocationId(int locationId) {
    this.locationId = locationId;
    return this;
  }

  public OmopPerson setMonthOfBirth(Month monthOfBirth) {
    this.monthOfBirth = monthOfBirth;
    return this;
  }

  public OmopPerson setPersonId(int personId) {
    this.personId = personId;
    return this;
  }

  public OmopPerson setSourceId(String sourceId) {
    this.sourceId = sourceId;
    return this;
  }

  public OmopPerson setYearOfBirth(Year yearOfBirth) {
    this.yearOfBirth = yearOfBirth;
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
}
