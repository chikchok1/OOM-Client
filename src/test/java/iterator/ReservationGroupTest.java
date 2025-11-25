/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package iterator;

/**
 *
 * @author jms5310
 */
import Service.ReservationService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReservationGroupTest {

    @Test
    void testIteratorTraversesAllItems() {
        // 1. 준비 (Arrange): 그룹 생성 및 데이터 추가
        ReservationGroup group = new ReservationGroup();
        
        // 테스트용 더미 데이터 생성 (생성자 파라미터는 DTO 정의에 맞춰 넣으세요)
        // 여기서는 테스트를 위해 필요한 최소한의 값만 넣거나, DTO 생성자가 복잡하면 null로 채워도 됩니다.
        // 핵심은 Iterator가 '객체'를 잘 반환하느냐입니다.
        Service.ReservationService.ReservationDTO data1 = createDummyDTO("홍길동", "908호");
        Service.ReservationService.ReservationDTO data2 = createDummyDTO("이순신", "912호");
        
        group.addReservation(data1);
        group.addReservation(data2);

        // 2. 실행 (Act): 이터레이터 생성
        Iterator iterator = group.createIterator();

        // 3. 검증 (Assert)
        
        // 첫 번째 아이템 확인
        assertTrue(iterator.hasNext(), "첫 번째 요소가 있어야 합니다.");
        Object item1 = iterator.next();
        assertEquals(data1, item1, "첫 번째로 반환된 객체는 data1이어야 합니다.");

        // 두 번째 아이템 확인
        assertTrue(iterator.hasNext(), "두 번째 요소가 있어야 합니다.");
        Object item2 = iterator.next();
        assertEquals(data2, item2, "두 번째로 반환된 객체는 data2이어야 합니다.");

        // 끝 확인
        assertFalse(iterator.hasNext(), "모든 요소를 순회했으므로 false여야 합니다.");
        assertNull(iterator.next(), "더 이상 요소가 없으면 null을 반환해야 합니다.");
    }

    // 테스트용 DTO 생성 헬퍼 메소드 (DTO 생성자가 복잡할 경우를 대비해 분리)
    private Service.ReservationService.ReservationDTO createDummyDTO(String name, String room) {
        // 실제 DTO 생성자에 맞춰서 수정해서 쓰세요. 
        // 아래는 예시입니다. (String fileType, String userId, String name ...)
        return new Service.ReservationService.ReservationDTO(
            "file", "id", name, room, "2025-11-27", "월", "1교시", "수업", "학생", 1
        );
    }
}