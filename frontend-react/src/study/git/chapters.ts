/**
 * "공부하기 > Git" 챕터 목록.
 * 정리는 notes/chNN.md, 명령과 실행 결과는 code/chNN/(commands.sh, output.txt), 핀볼 저장소 사례는 project/chNN.md에 있다.
 * 명령은 임시 폴더의 새 저장소에서 실제로 실행했고, output.txt는 그때의 출력이다.
 * SVN을 주로 써 온 사람을 기준으로, SVN과 비교하며 설명한다.
 */

export interface GitChapter {
  id: string;
  part: string;
  title: string;
  summary: string;
  /** 학습 목표: 이 챕터에서 무엇을 이해하려고 하는지(1~2줄) */
  goal: string;
  /** 학습 결과: 다 마치면 할 수 있는 것(1~2줄). 정리 끝의 확인 문제와 맞춘다 */
  outcome: string;
  keywords: string[];
  /** 명령과 결과 탭에 보여줄 파일 순서(code/chNN/ 안의 파일 이름). 비어 있으면 아직 준비 중인 챕터 */
  files: string[];
}

export const PARTS = ['기본', '브랜치와 협업', '되돌리기와 운영'];

export const GIT_CHAPTERS: GitChapter[] = [
  {
    id: 'ch01',
    part: '기본',
    title: 'Git이란(SVN과 비교)',
    summary: '중앙 서버 하나에 바로 올리는 SVN과, 내 PC에 저장소 전체를 두는 Git은 무엇이 다른지 봅니다.',
    goal: 'SVN과 Git의 가장 큰 차이(분산 저장소, commit과 push의 분리, 리비전 번호 대신 해시)를 이해한다.',
    outcome: 'SVN 명령(checkout, update, commit)에 해당하는 Git 명령을 말하고, 저장소를 만들어 첫 커밋과 이력을 확인할 수 있다.',
    keywords: ['SVN 비교', '분산 저장소', 'commit과 push'],
    files: ['commands.sh', 'output.txt'],
  },
  {
    id: 'ch02',
    part: '기본',
    title: '설치와 첫 설정',
    summary: '이름·이메일, 줄바꿈 설정, 사내망의 프록시·인증서, 저장소 받기(clone)까지 처음 한 번 하는 설정입니다.',
    goal: 'Git을 처음 쓸 때 해야 하는 설정과, 설정이 저장되는 위치(전역과 저장소별)를 이해한다.',
    outcome: '사용자 이름과 이메일, 줄바꿈 설정을 하고 지금 적용된 설정이 어디서 왔는지 확인하며, 저장소를 clone할 수 있다.',
    keywords: ['git config', 'core.autocrlf', 'clone'],
    files: ['commands.sh', 'output.txt'],
  },
  {
    id: 'ch03',
    part: '기본',
    title: '기본 흐름',
    summary: '작업 폴더 → 스테이징 → 저장소의 세 단계. status, add, commit, diff, log로 변경을 기록합니다.',
    goal: 'SVN에는 없는 스테이징 영역과, 파일이 작업 폴더에서 저장소까지 가는 세 단계를 이해한다.',
    outcome: 'git status로 파일 상태를 읽고, 원하는 변경만 골라 커밋하며, diff로 커밋 전후 차이를 확인할 수 있다.',
    keywords: ['스테이징', 'status', 'diff'],
    files: ['commands.sh', 'output.txt'],
  },
  {
    id: 'ch04',
    part: '기본',
    title: '.gitignore와 올리면 안 되는 것',
    summary: '빌드 결과물·로그·비밀값을 저장소에서 빼는 방법과, 이미 올린 비밀값을 처리하는 순서입니다.',
    goal: '저장소에 넣지 말아야 할 파일과, .gitignore가 이미 추적 중인 파일에는 효과가 없다는 점을 이해한다.',
    outcome: '.gitignore를 작성하고 어떤 규칙에 걸렸는지 확인하며, 비밀값을 커밋했을 때 이력에 남는다는 것을 보이고 대응 순서를 말할 수 있다.',
    keywords: ['.gitignore', 'git rm --cached', '비밀값'],
    files: ['commands.sh', 'output.txt'],
  },
  {
    id: 'ch05',
    part: '브랜치와 협업',
    title: '브랜치와 HEAD',
    summary: '폴더를 복사하던 SVN 브랜치와 달리, Git 브랜치는 커밋을 가리키는 이름표입니다.',
    goal: 'Git 브랜치가 커밋을 가리키는 가벼운 포인터이고, HEAD가 지금 위치를 뜻한다는 것을 이해한다.',
    outcome: '브랜치를 만들고 옮겨 다니며, 커밋 그래프에서 브랜치와 HEAD가 어디를 가리키는지 읽을 수 있다.',
    keywords: ['branch', 'switch', 'HEAD'],
    files: [],
  },
  {
    id: 'ch06',
    part: '브랜치와 협업',
    title: '병합과 충돌 해결',
    summary: 'fast-forward와 병합 커밋의 차이, 같은 줄을 둘이 고쳤을 때 생기는 충돌을 푸는 방법입니다.',
    goal: '병합이 fast-forward로 끝나는 경우와 병합 커밋이 생기는 경우, 충돌이 왜 생기는지 이해한다.',
    outcome: '브랜치를 병합하고, 충돌 표시(<<<<<<<)를 읽고 고쳐서 병합을 마칠 수 있다.',
    keywords: ['merge', 'fast-forward', '충돌'],
    files: [],
  },
  {
    id: 'ch07',
    part: '브랜치와 협업',
    title: '원격 저장소',
    summary: 'origin, fetch·pull·push, 추적 브랜치. 다른 사람이 먼저 올렸을 때 push가 거절되는 이유입니다.',
    goal: '내 저장소와 원격 저장소의 관계, fetch와 pull의 차이를 이해한다.',
    outcome: '원격의 변경을 받아 오고 내 커밋을 올리며, push가 거절됐을 때 원인과 해결 순서를 말할 수 있다.',
    keywords: ['origin', 'pull', 'push'],
    files: [],
  },
  {
    id: 'ch08',
    part: '브랜치와 협업',
    title: 'Pull Request와 브랜치 전략',
    summary: '코드 리뷰를 거쳐 병합하는 Pull Request(Merge Request)와, 운영 중 긴급 수정을 넣는 브랜치 흐름입니다.',
    goal: 'Pull Request로 리뷰한 뒤 병합하는 흐름과, 자주 쓰는 브랜치 전략(Git Flow, GitHub Flow)을 이해한다.',
    outcome: '기능 개발과 운영 긴급 수정이 각각 어느 브랜치에서 시작해 어디로 병합되는지 설명할 수 있다.',
    keywords: ['Pull Request', 'Git Flow', 'hotfix'],
    files: [],
  },
  {
    id: 'ch09',
    part: '되돌리기와 운영',
    title: '되돌리기',
    summary: 'restore, reset(soft·mixed·hard), revert. 이미 운영에 나간 커밋은 revert로 되돌립니다.',
    goal: '되돌리는 명령마다 무엇이 바뀌는지(작업 폴더, 스테이징, 이력)와, 공유된 커밋을 reset하면 안 되는 이유를 이해한다.',
    outcome: '상황에 맞는 되돌리기 명령을 고르고, 운영에 반영된 잘못된 커밋을 revert로 되돌릴 수 있다.',
    keywords: ['reset', 'revert', 'restore'],
    files: [],
  },
  {
    id: 'ch10',
    part: '되돌리기와 운영',
    title: '이력 조사',
    summary: 'log 옵션, show, blame, bisect로 언제 누가 왜 바꿨는지, 장애를 만든 커밋이 무엇인지 찾습니다.',
    goal: '이력을 조사하는 명령들과, 장애 원인 커밋을 좁혀 가는 방법을 이해한다.',
    outcome: '특정 줄을 마지막으로 바꾼 커밋을 찾고, bisect로 문제가 처음 생긴 커밋을 찾을 수 있다.',
    keywords: ['log', 'blame', 'bisect'],
    files: [],
  },
  {
    id: 'ch11',
    part: '되돌리기와 운영',
    title: 'stash, cherry-pick, tag',
    summary: '하던 작업 잠시 치우기, 긴급 수정 커밋을 다른 브랜치에도 넣기, 릴리스 버전에 이름 붙이기.',
    goal: '작업을 잠시 치워 두는 stash, 커밋 하나만 골라 오는 cherry-pick, 버전을 표시하는 tag의 쓰임을 이해한다.',
    outcome: '하던 일을 stash로 치우고 급한 수정을 한 뒤 되돌아오며, 긴급 수정을 다른 브랜치에 cherry-pick하고 태그를 달 수 있다.',
    keywords: ['stash', 'cherry-pick', 'tag'],
    files: [],
  },
  {
    id: 'ch12',
    part: '되돌리기와 운영',
    title: 'rebase와 사고 복구',
    summary: '이력을 한 줄로 정리하는 rebase와, 지운 커밋도 되살리는 reflog. 공유 브랜치에서 하지 말 것들입니다.',
    goal: 'rebase가 커밋을 새로 만든다는 점과, reflog로 잃어버린 커밋을 찾는 방법을 이해한다.',
    outcome: '내 브랜치를 rebase로 최신에 맞추고, 잘못된 reset으로 사라진 커밋을 reflog로 되살릴 수 있다.',
    keywords: ['rebase', 'reflog', '복구'],
    files: [],
  },
];

export function findGitChapter(id: string | undefined): GitChapter | undefined {
  return GIT_CHAPTERS.find((chapter) => chapter.id === id);
}

export function isReady(chapter: GitChapter): boolean {
  return chapter.files.length > 0;
}
