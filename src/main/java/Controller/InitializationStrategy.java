package Controller;

/**
 * Strategy 패턴: 초기화 전략 인터페이스
 * AbstractReservationController의 초기화 과정을 유연하게 처리
 */
public interface InitializationStrategy {
    
    /**
     * 초기화 실행
     * @param controller 컨트롤러 인스턴스
     */
    void initialize(AbstractReservationController controller);
}
