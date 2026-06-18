package york.studentevents.cohorts;

import java.util.List;
import java.util.UUID;

/**
 * Represents a cohort of University of York students.
 *
 * <p>A cohort groups students who share an academic year, enabling targeted event discovery and
 * social features. Implementations are responsible for maintaining cohort identity and membership.
 */
public interface ICohort {

  /** Returns the unique identifier for this cohort. */
  UUID getId();

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
   * <p> First year students are year 1; second year represented by 2.
   * 
   * <p> Foundation year cohorts are represented as 'year 0'.
   * 
   * <p> Placement years are represented by a year of 3; third years are represented by 4. Note that
   * this means that any students who are not following a placement year route seemingly jump
   * straight from year 2 into year 4.
   * 
   * <p> The masters stage is represented by 5.
   * 
   * @return the stage to which the cohort belongs
   */
  int getYearGroup();

  /**
   * Returns the IDs of all members belonging to this cohort.
   *
   * @return a list of member user IDs; never {@code null}
   */
  List<UUID> getMembers();

  /** Adds a member by UUID to this cohort.
   * <p>
   * @param memberId the user ID of the member to add*/
  void addMember(UUID memberId);

  /** Removes a member by UUID from this cohort.
   * <p>
   * @param memberId the user ID of the member to remove; must be a member of this cohort.
   * <p>
   * @throws IllegalArgumentException if the member is not a member of this cohort*/
  void removeMember(UUID memberId) throws IllegalArgumentException;
  
}
