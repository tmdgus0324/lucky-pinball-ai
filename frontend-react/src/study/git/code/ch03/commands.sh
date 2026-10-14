git init shop
cd shop

# 새 파일은 처음에 "추적하지 않는 파일(Untracked)"이다.
echo 'public class Room {}' > Room.java
git status

# add로 스테이징하면 "커밋할 변경(Changes to be committed)"이 된다.
git add Room.java
git status
git commit -m "Room 클래스 추가"

# 두 파일을 바꾸고, 하나만 골라서 커밋한다(SVN은 보통 바뀐 파일을 한꺼번에 올린다).
echo '// 객실 이름' >> Room.java
echo 'public class Reservation {}' > Reservation.java
git status
# -s: 짧게 보기. 앞 글자는 스테이징 상태, 뒤 글자는 작업 폴더 상태(M 수정, ?? 추적 안 함, A 추가)
git status -s
git add Reservation.java
git status -s
git commit -m "Reservation 클래스 추가"
# 스테이징한 것만 커밋됐다. Room.java 수정은 작업 폴더에 그대로 남아 있다.
git status -s

# 차이 보기. diff는 작업 폴더와 스테이징의 차이, --staged는 스테이징과 마지막 커밋의 차이다.
git diff
git add Room.java
git diff
git diff --staged
git commit -m "Room에 주석 추가"

# 이력
git log --oneline
git log --stat --oneline -2
# HEAD는 지금 위치(마지막 커밋), HEAD~1은 그 하나 전
git show --name-only --oneline HEAD~1
