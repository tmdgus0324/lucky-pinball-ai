-- src/main/resources/data.sql: schema.sql 다음에 실행되는 초기 데이터
INSERT INTO reservation (room_id, guest_name, check_in, nights, status) VALUES (1, '김민수', DATE '2026-11-01', 2, 'CONFIRMED');
INSERT INTO reservation (room_id, guest_name, check_in, nights, status) VALUES (2, '이영희', DATE '2026-11-03', 1, 'CONFIRMED');
INSERT INTO reservation (room_id, guest_name, check_in, nights, status) VALUES (1, '박지훈', DATE '2026-11-10', 3, 'CANCELED');
INSERT INTO reservation (room_id, guest_name, check_in, nights, status) VALUES (3, '최수진', DATE '2026-12-24', 2, 'CONFIRMED');
