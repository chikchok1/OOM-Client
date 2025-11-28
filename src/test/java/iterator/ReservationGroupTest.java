package iterator;

import Service.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

/**
 * Iterator Pattern 핵심 검증 테스트
 */
class ReservationGroupTest {

    // ============================================================
    // 1. 구조 검증
    // ============================================================

    @Test
    @DisplayName("ReservationGroup은 Aggregate 인터페이스를 구현해야 한다")
    void testReservationGroupImplementsAggregate() {
        assertTrue(Aggregate.class.isAssignableFrom(ReservationGroup.class),
            "ReservationGroup은 Aggregate를 구현해야 함");
    }

    @Test
    @DisplayName("ReservationDTOIterator는 Iterator 인터페이스를 구현해야 한다")
    void testReservationDTOIteratorImplementsIterator() {
        assertTrue(Iterator.class.isAssignableFrom(ReservationDTOIterator.class),
            "ReservationDTOIterator는 Iterator를 구현해야 함");
    }

    @Test
    @DisplayName("createIterator()는 ReservationDTOIterator를 반환해야 한다")
    void testCreateIteratorReturnsCorrectType() {
        ReservationGroup group = new ReservationGroup();
        Iterator iterator = group.createIterator();
        
        assertTrue(iterator instanceof ReservationDTOIterator,
            "createIterator()는 ReservationDTOIterator를 반환해야 함");
    }

    // ============================================================
    // 2. 기본 순회 테스트
    // ============================================================

    @Test
    @DisplayName("Iterator는 모든 요소를 순서대로 순회해야 한다")
    void testIteratorTraversesAllItems() {
        ReservationGroup group = new ReservationGroup();
        
        ReservationService.ReservationDTO data1 = createDummyDTO("홍길동", "908호");
        ReservationService.ReservationDTO data2 = createDummyDTO("이순신", "912호");
        
        group.addReservation(data1);
        group.addReservation(data2);

        Iterator iterator = group.createIterator();

        assertTrue(iterator.hasNext(), "첫 번째 요소가 있어야 함");
        assertEquals(data1, iterator.next(), "첫 번째는 data1");

        assertTrue(iterator.hasNext(), "두 번째 요소가 있어야 함");
        assertEquals(data2, iterator.next(), "두 번째는 data2");

        assertFalse(iterator.hasNext(), "모든 요소 순회 완료");
        assertNull(iterator.next(), "더 이상 요소 없으면 null 반환");
    }

    // ============================================================
    // 3. 엣지 케이스 테스트
    // ============================================================

    @Test
    @DisplayName("빈 컬렉션에서 hasNext()는 false를 반환해야 한다")
    void testEmptyCollectionHasNoNext() {
        ReservationGroup group = new ReservationGroup();
        Iterator iterator = group.createIterator();
        
        assertFalse(iterator.hasNext(), "빈 컬렉션은 hasNext() false");
        assertNull(iterator.next(), "빈 컬렉션에서 next()는 null");
    }

    @Test
    @DisplayName("단일 요소 컬렉션을 올바르게 순회해야 한다")
    void testSingleElementCollection() {
        ReservationGroup group = new ReservationGroup();
        ReservationService.ReservationDTO data = createDummyDTO("홍길동", "908호");
        group.addReservation(data);
        
        Iterator iterator = group.createIterator();
        
        assertTrue(iterator.hasNext());
        assertEquals(data, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    @DisplayName("여러 번 next()를 호출해도 안전해야 한다")
    void testMultipleNextCallsAreSafe() {
        ReservationGroup group = new ReservationGroup();
        group.addReservation(createDummyDTO("홍길동", "908호"));
        
        Iterator iterator = group.createIterator();
        iterator.next();
        
        assertNull(iterator.next(), "범위 초과 시 null");
        assertNull(iterator.next(), "계속 호출해도 null");
    }

    // ============================================================
    // 4. 내부 구현 검증
    // ============================================================

    @Test
    @DisplayName("position 필드가 올바르게 증가해야 한다")
    void testPositionIncrementsCorrectly() throws Exception {
        ReservationGroup group = new ReservationGroup();
        group.addReservation(createDummyDTO("홍길동", "908호"));
        group.addReservation(createDummyDTO("이순신", "912호"));
        
        ReservationDTOIterator iterator = 
            (ReservationDTOIterator) group.createIterator();
        
        Field field = ReservationDTOIterator.class.getDeclaredField("position");
        field.setAccessible(true);
        
        assertEquals(0, field.get(iterator), "초기 position은 0");
        
        iterator.next();
        assertEquals(1, field.get(iterator), "next() 후 position은 1");
        
        iterator.next();
        assertEquals(2, field.get(iterator), "다시 next() 후 position은 2");
    }

    @Test
    @DisplayName("reservations 필드는 private여야 한다")
    void testReservationsFieldIsPrivate() throws Exception {
        Field field = ReservationGroup.class.getDeclaredField("reservations");
        
        assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()),
            "reservations 필드는 private으로 캡슐화되어야 함");
    }

    // ============================================================
    // Helper
    // ============================================================

    private ReservationService.ReservationDTO createDummyDTO(String name, String room) {
        return new ReservationService.ReservationDTO(
            "file", "id", name, room, "2025-11-27", "월", "1교시", "수업", "학생", 1
        );
    }
}