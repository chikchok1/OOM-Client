/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package iterator;

/**
 *
 * @author jms5310
 * [Iterator Pattern: Aggregate Interface]
 * 집합체는 이 인터페이스를 구현하여 자신의 Iterator를 반환해야 합니다.
 */


public interface Aggregate {
    Iterator createIterator();
}