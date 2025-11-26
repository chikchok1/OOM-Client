/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package iterator;

/**
 *
 * @author jms5310
 * [Iterator Pattern: ConcreteAggregate]
 * 예약 정보(ReservationDTO)들의 집합을 관리하는 클래스입니다.
 * 내부적으로 ArrayList를 사용하여 데이터를 저장하지만, 
 * 외부는 createIterator()를 통해서만 순회하므로 내부 구조가 캡슐화됩니다.
 */
import Service.ReservationService;
import java.util.ArrayList;
import java.util.List;

public class ReservationGroup implements Aggregate {
    private List<ReservationService.ReservationDTO> reservations;

    public ReservationGroup() {
        this.reservations = new ArrayList<>();
    }

    public ReservationGroup(List<ReservationService.ReservationDTO> reservations) {
        this.reservations = reservations;
    }

    public void addReservation(ReservationService.ReservationDTO reservation) {
        this.reservations.add(reservation);
    }

    @Override
    public Iterator createIterator() {
        return new ReservationDTOIterator(this.reservations);
    }
}