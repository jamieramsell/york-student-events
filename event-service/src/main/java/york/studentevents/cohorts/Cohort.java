package york.studentevents.cohorts;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a cohort of students grouped by department, academic year, and year group.
 *
 * <p>A cohort maintains the set of member user IDs belonging to it.
 */
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
   * @param name the name of the cohort; must not be {@code null} or blank.
   * @param department the department of the cohort; must not be {@code null} or blank.
   * @param academicYear the academic year of the cohort; must be greater than 0.
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
   *      invalid
   */
  public Cohort(String name, String department, int academicYear, int yearGroup) {
    this(UUID.randomUUID(), name, department, academicYear, yearGroup);
  }

  /**
   * Creates a {@code Cohort} with the given details.
   *
   * @param id the unique identifier for the cohort; must not be {@code null}.
   * @param name the name of the cohort; must not be {@code null} or blank.
   * @param department the department of the cohort; must not be {@code null} or blank.
   * @param academicYear the academic year of the cohort; must be greater than 0.
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
   *      invalid
   */
  protected Cohort(UUID id, String name, String department, int academicYear, int yearGroup) {
    if (id == null) {
      throw new IllegalArgumentException("Cohort ID cannot be null");
    }
    setName(name);
    setDepartment(department);
    setAcademicYear(academicYear);
    setYearGroup(yearGroup);
    this.id = id;
    this.members = new ArrayList<>();
  }

  private void setName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Cohort name cannot be null, blank, or empty.");
    }
    this.name = name;
  }

  private void setDepartment(String department) {
    if (department == null || department.isBlank()) {
      throw new IllegalArgumentException("Cohort department cannot be null, blank, or empty.");
    }
    this.department = department;
  }

  private void setAcademicYear(int academicYear) {
    if (academicYear < 0) {
      throw new IllegalArgumentException("Academic year must be >= 0");
    }
    this.academicYear = academicYear;
  }

  private void setYearGroup(int yearGroup) {
    if (yearGroup < 0) {
      throw new IllegalArgumentException("Year group must be >= 0");
    } else if (yearGroup > 5) {
      throw new IllegalArgumentException("Year group must be <= 5");
    }
    this.yearGroup = yearGroup;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDepartment() {
    return department;
  }

  @Override
  public int getAcademicYear() {
    return academicYear;
  }

  @Override
  public int getYearGroup() {
    return yearGroup;
  }

  @Override
  public List<UUID> getMembers() {
    return members;
  }

  @Override
  public void addMember(UUID memberId) {
    members.add(memberId);
  }

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
