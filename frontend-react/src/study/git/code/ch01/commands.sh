# SVN은 서버에 저장소가 있고 checkout으로 받아 온다.
# Git은 내 PC에 저장소를 바로 만들 수 있다(서버는 나중에 연결해도 된다).
git init reservation
cd reservation

# .git 폴더가 저장소 전체다(이력, 브랜치, 설정). SVN의 .svn과 달리 지난 이력이 통째로 들어 있다.
ls -A
ls .git

# 파일을 만들고 첫 커밋
echo "객실 예약 시스템" > README.md
git status
git add README.md
git commit -m "README 추가"

# 한 번 더 고치고 커밋(-a: 이미 추적 중인 파일의 변경을 모두 담아서 커밋)
echo "운영 담당: 김개발" >> README.md
git commit -am "운영 담당자 추가"

# 이력. SVN의 리비전 번호(r1, r2) 대신 해시(앞 7자리만 써도 된다)로 커밋을 부른다.
git log --oneline
git log -1

# 커밋은 아직 내 PC 저장소에만 있다. 연결된 서버가 없어서 아래 명령은 아무것도 출력하지 않는다.
git remote -v
# 서버(원격 저장소)에 올리는 것은 push로 따로 한다(7장). 그래서 인터넷이 없어도 커밋과 이력 조회는 된다.
