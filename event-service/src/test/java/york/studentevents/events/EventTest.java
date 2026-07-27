package york.studentevents.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventTest {

  private Event event;

  @BeforeEach
  void setUp() {
    event = new Event("Test Event", 100, EventCategory.MUSIC);
  }

  // --- Constructor with capacity ---

  @Test
  void constructor_withValidArgs_assignsFieldsAndGeneratesId() {
    Event e = new Event("York Social", 50, EventCategory.SPORTS);

    assertEquals("York Social", e.getTitle());
    assertEquals(50, e.getCapacity());
    assertEquals(EventCategory.SPORTS, e.getCategory());
    assertNotNull(e.getId());
  }

  @Test
  void constructor_withNullTitle_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Event(null, 50, EventCategory.SPORTS));
  }

  @Test
  void constructor_withBlankTitle_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Event("   ", 50, EventCategory.SPORTS));
  }

  @Test
  void constructor_withEmptyTitle_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Event("", 50, EventCategory.SPORTS));
  }

  @Test
  void constructor_withNullCategory_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Event("York Social", 50, null));
  }

  @Test
  void constructor_withZeroCapacity_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Event("York Social", 0,
        EventCategory.SPORTS));
  }

  @Test
  void constructor_withNegativeCapacity_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Event("York Social", -1,
        EventCategory.SPORTS));
  }

  @Test
  void constructor_withCapacityOfOne_succeeds() {
    Event e = new Event("York Social", 1, EventCategory.SPORTS);
    assertEquals(1, e.getCapacity());
  }

  // --- Constructor without capacity ---

  @Test
  void constructor_noCapacity_withValidArgs_assignsFieldsAndCapacityIsNull() {
    Event e = new Event("York Social", EventCategory.SPORTS);

    assertEquals("York Social", e.getTitle());
    assertEquals(EventCategory.SPORTS, e.getCategory());
    assertNull(e.getCapacity());
    assertNotNull(e.getId());
  }

  @Test
  void constructor_noCapacity_withNullTitle_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Event(null,
        EventCategory.SPORTS));
  }

  @Test
  void constructor_noCapacity_withBlankTitle_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Event("   ",
        EventCategory.SPORTS));
  }

  @Test
  void constructor_noCapacity_withEmptyTitle_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Event("",
        EventCategory.SPORTS));
  }

  @Test
  void constructor_noCapacity_withNullCategory_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Event("York Social",
        (EventCategory) null));
  }

  // --- Initial state ---

  @Test
  void getDescription_initiallyNull() {
    assertNull(event.getDescription());
  }

  @Test
  void getStartDateTime_initiallyNull() {
    assertNull(event.getStartDateTime());
  }

  @Test
  void getEndDateTime_initiallyNull() {
    assertNull(event.getEndDateTime());
  }

  @Test
  void getVenue_initiallyNull() {
    assertNull(event.getVenue());
  }

  // --- UUID uniqueness ---

  @Test
  void getId_twoDistinctEvents_haveUniqueIds() {
    Event other = new Event("Another Event", 50, EventCategory.SPORTS);
    assertNotEquals(event.getId(), other.getId());
  }

  // --- setTitle ---

  @Test
  void setTitle_withValidTitle_updatesTitle() {
    event.setTitle("New Title");
    assertEquals("New Title", event.getTitle());
  }

  @Test
  void setTitle_withNull_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> event.setTitle(null));
  }

  @Test
  void setTitle_withBlankTitle_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> event.setTitle("   "));
  }

  @Test
  void setTitle_withEmptyTitle_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> event.setTitle(""));
  }

  // --- setDescription ---

  @Test
  void setDescription_withValidDescription_updatesDescription() {
    event.setDescription("A fun evening event.");
    assertEquals("A fun evening event.", event.getDescription());
  }

  @Test
  void setDescription_withNull_setsNull() {
    event.setDescription("Initial description");
    event.setDescription(null);
    assertNull(event.getDescription());
  }

  // --- setVenue ---

  @Test
  void setVenue_withValidLocation_updatesLocation() {
    event.setVenue(UUID.fromString("632b4764-69e7-4ef6-9645-2789919c29ac"));
    assertEquals("632b4764-69e7-4ef6-9645-2789919c29ac", event.getVenue());
  }

  @Test
  void setVenue_withNull_whenNoDateTimeSet_setsNull() {
    event.setVenue(UUID.fromString("632b4764-69e7-4ef6-9645-2789919c29ac"));
    event.setVenue(null);
    assertNull(event.getVenue());
  }

  @Test
  void setVenue_withNull_whenDateTimeAlreadySet_throwsIllegalStateException() {
    event.setVenue(UUID.fromString("632b4764-69e7-4ef6-9645-2789919c29ac"));
    event.setDateTime(
        LocalDateTime.of(2026, 8, 1, 18, 0),
        LocalDateTime.of(2026, 8, 1, 22, 0));
    assertThrows(IllegalStateException.class, () -> event.setVenue(null));
  }

  // --- setDateTime ---

  @Test
  void setDateTime_whenVenueSet_updatesDateTimes() {
    LocalDateTime start = LocalDateTime.of(2026, 8, 1, 18, 0);
    LocalDateTime end = LocalDateTime.of(2026, 8, 1, 22, 0);
    event.setVenue(UUID.fromString("632b4764-69e7-4ef6-9645-2789919c29ac"));

    event.setDateTime(start, end);

    assertEquals(start, event.getStartDateTime());
    assertEquals(end, event.getEndDateTime());
  }

  @Test
  void setDateTime_whenNoLocationSet_throwsIllegalStateException() {
    LocalDateTime start = LocalDateTime.of(2026, 8, 1, 18, 0);
    LocalDateTime end = LocalDateTime.of(2026, 8, 1, 22, 0);
    assertThrows(IllegalStateException.class, () -> event.setDateTime(start, end));
  }

  @Test
  void setDateTime_withStartAfterEnd_throwsIllegalArgumentException() {
    event.setVenue(UUID.fromString("632b4764-69e7-4ef6-9645-2789919c29ac"));
    LocalDateTime start = LocalDateTime.of(2026, 8, 1, 22, 0);
    LocalDateTime end = LocalDateTime.of(2026, 8, 1, 18, 0);
    assertThrows(IllegalArgumentException.class, () -> event.setDateTime(start, end));
  }

  @Test
  void setDateTime_withStartEqualToEnd_throwsIllegalArgumentException() {
    event.setVenue(UUID.fromString("632b4764-69e7-4ef6-9645-2789919c29ac"));
    LocalDateTime same = LocalDateTime.of(2026, 8, 1, 18, 0);
    assertThrows(IllegalArgumentException.class, () -> event.setDateTime(same, same));
  }

  @Test
  void setDateTime_withStartInPast_throwsIllegalArgumentException() {
    event.setVenue(UUID.fromString("632b4764-69e7-4ef6-9645-2789919c29ac"));
    LocalDateTime past = LocalDateTime.of(2020, 1, 1, 12, 0);
    LocalDateTime end = LocalDateTime.of(2020, 1, 1, 14, 0);
    assertThrows(IllegalArgumentException.class, () -> event.setDateTime(past, end));
  }

  // --- setCapacity ---

  @Test
  void setCapacity_withValidValue_updatesCapacity() {
    event.setCapacity(200);
    assertEquals(200, event.getCapacity());
  }

  @Test
  void setCapacity_withNull_removesCapacityLimit() {
    event.setCapacity(null);
    assertNull(event.getCapacity());
  }

  // --- setCategory ---

  @Test
  void setCategory_withValidCategory_updatesCategory() {
    event.setCategory(EventCategory.NIGHTLIFE);
    assertEquals(EventCategory.NIGHTLIFE, event.getCategory());
  }

  // --- toString ---

  @Test
  void toString_containsExpectedFields() {
    String result = event.toString();
    assertTrue(result.contains("Test Event"));
    assertTrue(result.contains("MUSIC"));
    assertTrue(result.contains(event.getId().toString()));
  }
}
