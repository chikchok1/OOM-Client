/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package iterator;

/**
 *
 * @author jms5310
 * [Iterator Pattern: ConcreteIterator]
 * ReservationGroup의 컬렉션을 실제로 순회하는 역할을 담당합니다.
 * 현재 순회 위치를 관리하여 다음 요소를 반환합니다.
 */
import Service.ReservationService;
import java.util.List;

public class ReservationDTOIterator implements Iterator {
    private List<ReservationService.ReservationDTO> reservations;
    private int position = 0;

    public ReservationDTOIterator(List<ReservationService.ReservationDTO> reservations) {
        this.reservations = reservations;
    }

    @Override
    public boolean hasNext() {
        return position < reservations.size();
    }

    @Override
    public Object next() {
        if (hasNext()) {
            return reservations.get(position++);
        }
        return null;
    }
}
