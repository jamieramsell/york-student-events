package york.studentevents.cohorts;

import java.util.Set;
import java.util.UUID;
import york.studentevents.repository.IEntity;

/**
 * Represents a cohort of University of York students.
 *
 * <p>A cohort groups students who share an academic year, enabling targeted event discovery and
 * social features. Implementations are responsible for maintaining cohort identity and membership.
 */
public interface ICohort extends IEntity {

  /**
   * Returns the display name of this cohort.
   *
   * @return the cohort name; never {@code null}
   */
  String getName();

  /**
   * Returns the name of the department which this cohort belongs to (e.g. Computer Science)
   *
   * @return the display name of the department; never {@code null}
   */
  String getDepartment();

  /**
   * Returns the academic year associated with this cohort.
   *
   * @return the academic year (e.g. {@code 2025} for the 2025/26 cohort)
   */
  int getAcademicYear();

  /**
   * Returns the year group / stage of the cohort.
   *
   * <p>First year students are year 1; second year represented by 2.
   *
   * <p>Foundation year cohorts are represented as 'year 0'.
   *
   * <p>Placement years are represented by a year of 3; third years are represented by 4. Note that
   * this means that any students who are not following a placement year route seemingly jump
   * straight from year 2 into year 4.
   *
   * <p>The masters stage is represented by 5.
   *
   * @return the stage to which the cohort belongs
   */
  int getYearGroup();

  /**
   * Returns the IDs of all Students belonging to this cohort.
   *
   * @return a set of member user IDs; never {@code null}
   */
  Set<UUID> getMembers();

  /** Adds a Student by UUID to this cohort.
   *
   * @param memberId the user ID of the member to add
   * @throws IllegalArgumentException if the member is already a member of this cohort
   */
  void addMember(UUID memberId);

  /** Removes a Student by UUID from this cohort.
   *
   * @param memberId the user ID of the member to remove; must be a member of this cohort.
   * @throws IllegalArgumentException if the member is not a member of this cohort
   */
  void removeMember(UUID memberId);

  /**
   * Set the display name of this cohort.
   *
   * @param name the cohort name; never {@code null}
   */
  void setName(String name);

  /**
   * Set the name of the department which this cohort belongs to (e.g. Computer Science)
   *
   * @param departmentName the display name of the department; never {@code null}
   */
  void setDepartment(String departmentName);

  /**
   * Set the academic year associated with this cohort.
   *
   * @param academicYear the academic year (e.g. {@code 2025} for the 2025/26 cohort); must be > 0.
   */
  void setAcademicYear(int academicYear);

  /**
   * Set the year group / stage of the cohort.
   *
   * <p>First year students are represented by stage 1; second years are stage 2.
   *
   * <p>Foundation year cohorts are represented as 'stage 0'.
   *
   * <p>Placement years are represented by a stage of 3; third years are represented by 4. Note that
   * this means that any students who are not following a placement year route seemingly jump
   * straight from stage 2 into stage 4.
   *
   * <p>The masters stage is represented by 5.
   *
   * @param stage the stage to which the cohort belongs
   * 
   * @throws IllegalArgumentException if stage is less than 0, or greater than 5.
   */
  void setYearGroup(int stage);
  
}
