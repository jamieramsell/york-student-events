package york.studentevents.cohorts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CohortTest {

  private ICohort cohort;

  @BeforeEach
  void setUp() {
    cohort = new Cohort("Test Cohort", "Test Department", 2026, 1);
  }

  // Constructor

  @Test
  void constructor_withValidArgs_assignsFieldsAndGeneratesId() {
    Cohort c = new Cohort("Computer Science 2025", "Computer Science", 2025, 2);

    assertEquals("Computer Science 2025", c.getName());
    assertEquals("Computer Science", c.getDepartment());
    assertEquals(2025, c.getAcademicYear());
    assertEquals(2, c.getYearGroup());
    assertNotNull(c.getId());
  }

  @Test
  void constructor_initialisesWithNoMembers() {
    assertTrue(cohort.getMembers().isEmpty());
  }

  @Test
  void constructor_twoDistinctCohorts_haveUniqueIds() {
    Cohort other = new Cohort("Test Cohort", "Test Department", 2026, 1);
    assertNotEquals(cohort.getId(), other.getId());
  }

  @Test
  void constructor_withNullName_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new Cohort(null, "Test Department", 2026, 1));
  }

  @Test
  void constructor_withBlankName_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new Cohort("   ", "Test Department", 2026, 1));
  }

  @Test
  void constructor_withEmptyName_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new Cohort("", "Test Department", 2026, 1));
  }

  @Test
  void constructor_withNullDepartment_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new Cohort("Test Cohort", null, 2026, 1));
  }

  @Test
  void constructor_withBlankDepartment_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new Cohort("Test Cohort", "   ", 2026, 1));
  }

  @Test
  void constructor_withNegativeAcademicYear_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new Cohort("Test Cohort", "Test Department", -1, 1));
  }

  @Test
  void constructor_withNegativeStage_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new Cohort("Test Cohort", "Test Department", 2026, -1));
  }

  @Test
  void constructor_withStageAboveFive_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new Cohort("Test Cohort", "Test Department", 2026, 6));
  }

  @Test
  void constructor_withNullId_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new Cohort((UUID) null, "Test Cohort", "Test Department", 2026, 1));
  }

  // setName

  @Test
  void setName_withValidName_updatesName() {
    cohort.setName("Updated Cohort");
    assertEquals("Updated Cohort", cohort.getName());
  }

  @Test
  void setName_rejectsEmptyString() {
    assertThrows(IllegalArgumentException.class, () -> cohort.setName(""));
  }

  @Test
  void setName_rejectsBlankString() {
    assertThrows(IllegalArgumentException.class, () -> cohort.setName("   "));
  }

  @Test
  void setName_rejectsNullString() {
    assertThrows(IllegalArgumentException.class, () -> cohort.setName(null));
  }

  // setDepartment

  @Test
  void setDepartment_withValidDepartment_updatesDepartment() {
    cohort.setDepartment("Mathematics");
    assertEquals("Mathematics", cohort.getDepartment());
  }

  @Test
  void setDepartment_rejectsEmptyString() {
    assertThrows(IllegalArgumentException.class, () -> cohort.setDepartment(""));
  }

  @Test
  void setDepartment_rejectsBlankString() {
    assertThrows(IllegalArgumentException.class, () -> cohort.setDepartment("   "));
  }

  @Test
  void setDepartment_rejectsNullString() {
    assertThrows(IllegalArgumentException.class, () -> cohort.setDepartment(null));
  }

  // setAcademicYear

  @Test
  void setAcademicYear_withValidYear_updatesAcademicYear() {
    cohort.setAcademicYear(2030);
    assertEquals(2030, cohort.getAcademicYear());
  }

  @Test
  void setAcademicYear_withOne_updatesAcademicYear() {
    cohort.setAcademicYear(1);
    assertEquals(1, cohort.getAcademicYear());
  }

  @Test
  void setAcademicYear_withYearZero_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> cohort.setAcademicYear(0));
  }

  @Test
  void setAcademicYear_withNegativeYear_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> cohort.setAcademicYear(-1));
  }

  // setYearGroup

  @Test
  void setYearGroup_withValidStage_updatesYearGroup() {
    cohort.setYearGroup(3);
    assertEquals(3, cohort.getYearGroup());
  }

  @Test
  void setYearGroup_withFoundationStage_updatesYearGroup() {
    cohort.setYearGroup(0);
    assertEquals(0, cohort.getYearGroup());
  }

  @Test
  void setYearGroup_withMastersStage_updatesYearGroup() {
    cohort.setYearGroup(5);
    assertEquals(5, cohort.getYearGroup());
  }

  @Test
  void setYearGroup_withNegativeStage_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> cohort.setYearGroup(-1));
  }

  @Test
  void setYearGroup_withStageAboveFive_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> cohort.setYearGroup(6));
  }

  // addMember / removeMember

  @Test
  void addMember_withNewMember_addsMember() {
    UUID memberId = UUID.randomUUID();
    cohort.addMember(memberId);
    assertTrue(cohort.getMembers().contains(memberId));
  }

  @Test
  void addMember_withExistingMember_throwsIllegalArgumentException() {
    UUID memberId = UUID.randomUUID();
    cohort.addMember(memberId);
    assertThrows(IllegalArgumentException.class, () -> cohort.addMember(memberId));
  }

  @Test
  void removeMember_withExistingMember_removesMember() {
    UUID memberId = UUID.randomUUID();
    cohort.addMember(memberId);
    cohort.removeMember(memberId);
    assertTrue(cohort.getMembers().isEmpty());
  }

  @Test
  void removeMember_withNonMember_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> cohort.removeMember(UUID.randomUUID()));
  }

  // getMembers

  @Test
  void getMembers_returnsDefensiveCopy() {
    UUID memberId = UUID.randomUUID();
    cohort.addMember(memberId);

    cohort.getMembers().clear();

    assertTrue(cohort.getMembers().contains(memberId));
  }

  // toString

  @Test
  void toString_containsExpectedFields() {
    String result = cohort.toString();
    assertTrue(result.contains("Test Cohort"));
    assertTrue(result.contains("Test Department"));
    assertTrue(result.contains(cohort.getId().toString()));
  }
}
