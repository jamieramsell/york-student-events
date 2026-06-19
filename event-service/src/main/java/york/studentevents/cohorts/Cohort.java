package york.studentevents.cohorts;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Cohort implements ICohort {
  private final UUID id;
  private String name;
  private String department;
  private int academicYear;
  private int yearGroup;
  private final List<UUID> members;

  /**
   * Creates a {@code Cohort} with the given details.
   *
   * @param name the name of the cohort; must not be {@code null} or blank
   * @param department the department of the cohort; must not be {@code null} or blank
   * @param academicYear the academic year of the cohort; must be greater than 0
   * @param yearGroup the year group of the cohort; must be within the range of 0 to 5.
   *     <ul>
   *     <li>Foundation year cohorts are represented as 'year 0'.</li>
   *     <li>First year students are year 1; second year represented by 2.</li>
   *     <li>Placement years are represented by a year of 3; third years are represented by 4. Note
   *         that this means that any students who are not following a placement year route
   *         seemingly jump straight from year 2 into year 4.</li>
   *     <li>The masters stage is represented by 5.</li>
   *     </ul>
   * @throws IllegalArgumentException if the name, department, academic year, or year group is
   *     invalid
   */
  public Cohort(String name, String department, int academicYear, int yearGroup) {

    // Validation
    if (name == null || name.isBlank() || name.isEmpty()) {
      throw new IllegalArgumentException("Cohort name cannot be null, blank, or empty.");
    } else if (department == null || department.isBlank() || department.isEmpty()) {
      throw new IllegalArgumentException("Cohort department cannot be null, blank, or empty.");
    } else if (academicYear < 0) {
      throw new IllegalArgumentException("Academic year must be >= 0");
    } else if (yearGroup < 0) {
      throw new IllegalArgumentException("Year group must be >= 0");
    } else if (yearGroup > 5) {
      throw new IllegalArgumentException("Year group must be @code <= 5");
    }

    this.name = name;
    this.department = department;
    this.academicYear = academicYear;
    this.yearGroup = yearGroup;

    this.id = UUID.randomUUID();
    this.members = new ArrayList<>();
  }

  /** Returns the unique identifier for this cohort. */
  @Override
  public UUID getId() {
    return id;
  }

  /**
   * Returns the display name of this cohort.
   *
   * @return the cohort name; never {@code null}
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Returns the name of the department which this cohort belongs to (e.g. Computer Science)
   *
   * @return the display name of the department; never {@code null}
   */
  @Override
  public String getDepartment() {
    return department;
  }

  /**
   * Returns the academic year associated with this cohort.
   *
   * @return the academic year (e.g. {@code 2025} for the 2025/26 cohort)
   */
  @Override
  public int getAcademicYear() {
    return academicYear;
  }

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
  @Override
  public int getYearGroup() {
    return yearGroup;
  }

  /**
   * Returns the IDs of all members belonging to this cohort.
   *
   * @return a list of member user IDs; never {@code null}
   */
  @Override
  public List<UUID> getMembers() {
    return members;
  }

  /**
   * Adds a member by UUID to this cohort.
   *
   * @param memberId the user ID of the member to add
   */
  @Override
  public void addMember(UUID memberId) {
    members.add(memberId);
  }

  /**
   * Removes a member by UUID from this cohort.
   *
   * @param memberId the user ID of the member to remove; must be a member of this cohort.
   *
   * @throws IllegalArgumentException if the member is not a member of this cohort
   */
  @Override
  public void removeMember(UUID memberId) {
    if (!members.contains(memberId)) {
      throw new IllegalArgumentException("Member is not a member of this cohort.");
    }
    members.remove(memberId);
  }

  /** Returns a string representation for debugging purposes. */
  @Override
  public String toString() {
    return String.format("Cohort[id=%s, name='%s', department='%s', academicYear=%d, yearGroup=%d,"
        + " members=%s]", id, name, department, academicYear, yearGroup, members);
  }
}
