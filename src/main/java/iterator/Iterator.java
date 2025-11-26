/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package iterator;

/**
 *
 * @author jms5310
 * [Iterator Pattern: Iterator Interface]
 * 집합체의 요소들을 순서대로 접근하기 위한 표준 인터페이스입니다.
 * 내부 구현 방식을 노출하지 않고 순회할 수 있는 메소드를 정의합니다.
 */
public interface Iterator {
    boolean hasNext();
    Object next();
}