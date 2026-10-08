-- src/main/resources/schema.sql: 서버가 시작할 때 실행된다(메모리 DB라서 매번 새로 만든다).
CREATE TABLE reservation (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id    BIGINT       NOT NULL,
    guest_name VARCHAR(50)  NOT NULL,
    check_in   DATE         NOT NULL,
    nights     INT          NOT NULL,
    status     VARCHAR(20)  NOT NULL
);
