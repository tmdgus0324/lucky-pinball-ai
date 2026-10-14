git init shop
cd shop

# 소스와 함께 빌드 결과물, 로그, 비밀번호가 든 로컬 설정 파일이 있다.
mkdir src build logs
echo 'public class Room {}' > src/Room.java
echo 'binary' > build/app.jar
echo 'INFO started' > logs/app.log
echo 'spring.datasource.password=secret1234' > application-local.properties
git status -s

# 저장소에 넣지 않을 것을 .gitignore에 적는다.
printf 'build/\nlogs/\n*.log\napplication-local.properties\n' > .gitignore
cat .gitignore
git status -s

# 어떤 파일이 어느 규칙 때문에 빠졌는지 확인
git check-ignore -v logs/app.log build/app.jar application-local.properties
git add .
git commit -m "소스와 .gitignore 추가"

# 이미 커밋된 파일은 나중에 .gitignore에 넣어도 계속 추적된다.
echo 'server.port=8080' > application.properties
git add application.properties
git commit -m "설정 파일 추가"
echo 'application.properties' >> .gitignore
echo 'server.port=9090' > application.properties
git status -s

# 추적만 멈추고 내 PC의 파일은 남긴다.
git rm --cached application.properties
git status -s
git commit -am "application.properties 추적 중단"
ls

# 비밀값을 커밋해 버렸다면? 다음 커밋에서 파일을 지워도 이력에는 남는다.
echo 'payment.api-key=sk-test-1234abcd' > payment.properties
git add payment.properties
git commit -m "결제 설정 추가"
git rm -q payment.properties
git commit -m "결제 설정 삭제"
ls
git log --oneline
git show HEAD~1:payment.properties
