# 🖥️ Classroom Reservation System - Client

> Java Swing 기반 강의실 예약 시스템 클라이언트

![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=java)
![Maven](https://img.shields.io/badge/Maven-3.x-C71A36?style=flat-square)
![Swing](https://img.shields.io/badge/Swing-GUI-orange?style=flat-square)

## 🎯 개요

대학교 강의실 예약 시스템의 클라이언트 애플리케이션입니다. 학생, 교수, 조교의 권한에 따라 차별화된 기능을 제공합니다.

### 시스템 아키텍처

```
Client (Swing GUI) ←→ TCP/IP Socket ←→ Server (Multi-thread)
        ↓                                        ↓
    MVC Pattern                          Command Pattern (33+ Commands)
        ↓                                        ↓
  5 Design Patterns                      File-based Storage
```

### 사용자 타입

- **학생 (S)**: 예약 신청, 조회, 변경/취소
- **교수 (P)**: 예약 신청, 전체 현황 조회 (이름 표시)
- **조교 (A)**: 예약 승인/거부, 강의실 관리, 사용자 관리

## ✨ 주요 기능

| 기능               | 학생      | 교수      | 조교      |
| ------------------ | --------- | --------- | --------- |
| 강의실/실습실 예약 | ✅        | ✅        | ✅        |
| 예약 조회          | ✅ (익명) | ✅ (이름) | ✅ (이름) |
| 예약 변경/취소     | ✅        | ✅        | ✅        |
| 실시간 알림        | ✅        | ✅        | ✅        |
| 예약 승인/거부     | ❌        | ❌        | ✅        |
| 강의실 관리        | ❌        | ❌        | ✅        |
| 사용자 관리        | ❌        | ❌        | ✅        |

## 🎨 디자인 패턴

| 패턴                | 적용 클래스                                        | 목적               |
| ------------------- | -------------------------------------------------- | ------------------ |
| **Singleton**       | Session, MessageDispatcher, ClientClassroomManager | 단일 인스턴스 보장 |
| **Observer**        | ClientNotificationObserver, NotificationListener   | 실시간 알림 처리   |
| **Iterator**        | ReservationGroup, ReservationDTOIterator           | 예약 데이터 순회   |
| **Facade**          | ClientFacade                                       | 서브시스템 단순화  |
| **Template Method** | AbstractReservationController                      | 공통 로직 정의     |

## 📁 프로젝트 구조

```
src/main/java/
├── Controller/           # MVC 컨트롤러 (16개)
│   ├── LoginController.java
│   ├── ReservClassController.java
│   ├── ExecutiveController.java
│   └── ...
├── View/                # Swing GUI (15개)
│   ├── LoginForm.java
│   ├── RoomSelect.java
│   └── ...
├── Model/               # Session (Singleton)
├── Service/             # 비즈니스 로직
├── Observer/            # Observer 패턴
├── iterator/            # Iterator 패턴
└── Util/                # MessageDispatcher 등

src/test/java/           # 단위 테스트 (21개)
```

## 🚀 설치 및 실행

### 사전 요구사항

- Java 21+, Maven 3.x
- OOM-Common 모듈 설치
- OOM-Server 실행 중

### 빌드 및 실행

```bash
# 1. Common 모듈 빌드
cd ../OOM-Common && mvn clean install

# 2. Client 빌드 및 실행
cd ../OOM-Client
mvn clean install
java -jar target/pos-client.jar
```

### 테스트 계정

| 타입 | ID   | 비밀번호 |
| ---- | ---- | -------- |
| 학생 | S123 | pass123  |
| 교수 | P678 | pass456  |
| 조교 | A111 | pass789  |

## 🔧 설정

### 로컬 서버 접속 (기본)

```properties
# config.properties
server.ip=localhost
server.port=8000
```

### 외부 서버 접속

서버가 다른 컴퓨터에서 실행 중이라면 `server.ip`를 해당 서버의 IP로 변경하세요.

**로컬 네트워크**

```properties
server.ip=192.168.0.100  # 서버 PC의 IP
server.port=8000
```

**인터넷을 통한 접속**

```properties
server.ip=203.0.113.100  # 서버의 공인 IP
server.port=8000
```

> **참고**: 서버는 `0.0.0.0`으로 바인딩되어 외부 접속이 가능하게 구현되어 있습니다. 클라이언트에서 IP만 변경하면 바로 연결됩니다.

## 📖 사용법

### 예약 프로세스

1. 로그인 → 강의실/실습실 선택
2. 날짜 및 시간 선택
3. 인원수, 사용 목적 입력
4. 예약 신청 → 조교 승인 대기
5. 실시간 알림으로 승인/거부 확인

### 예약 조회

- **학생**: 타인 예약은 "예약됨"으로 표시
- **교수/조교**: 예약자 이름 직접 표시

## 🧪 테스트

```bash
mvn test
```

- **테스트 클래스**: 21개
- **커버리지**: 85%+
- **주요 테스트**: Singleton, Observer, Iterator 패턴 검증

## 🔄 서버 통신

### 프로토콜

- TCP/IP Socket, CSV 기반 메시지
- 비동기 처리 (MessageDispatcher)

### 주요 명령어

```
LOGIN,userId,password
REGISTER,userId,name,password,type
RESERVE_REQUEST,roomId,date,time,...
VIEW_RESERVATION,date
APPROVE_RESERVATION,reservationId
```

### 실시간 알림 (Observer)

```
NOTIFICATION,APPROVED,reservationId
NOTIFICATION,REJECTED,reservationId,reason
```

## 📝 핵심 클래스

- **MessageDispatcher**: 서버 메시지 라우팅 (Singleton, 백그라운드 스레드)
- **Session**: 사용자 세션 및 소켓 관리 (Singleton)
- **ClientNotificationObserver**: 실시간 알림 처리 (Observer)
- **ReservationGroup**: 예약 데이터 컬렉션 (Iterator)
- **ClientFacade**: 클라이언트 기능 통합 인터페이스 (Facade)

## 📊 통계

- **코드**: ~8,000 lines, 60+ classes
- **디자인 패턴**: 5개 적용
- **테스트**: 21 test classes

## 🔗 관련 프로젝트

- [OOM-Server](../OOM-Server) - 멀티스레드 서버 (33+ Commands)
- [OOM-Common](../OOM-Common) - 공통 모듈 (DTO, Model)

## 📄 라이선스

교육 목적 프로젝트

---

**OOM Team** | 객체지향 프로그래밍 과제
