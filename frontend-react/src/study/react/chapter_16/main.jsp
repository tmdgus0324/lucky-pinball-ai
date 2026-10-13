<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- 비교용: 데모와 같은 게시판을 레거시 JSP 방식으로 쓴 것(이 사이트에서는 실행하지 않음).
     첫 목록은 서버(Controller)가 Model에 담은 list를 <c:forEach>로 그리고,
     검색·페이지 이동·상세·저장·삭제는 jQuery $.ajax로 처리한다. --%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>게시판</title>
<script src="/js/jquery.min.js"></script>
<script>
  // ===== 전역 변수 =====
  var currentPage = 1;
  var PAGE_SIZE = 5;

  // ===== 화면이 다 열리면 =====
  $(document).ready(function () {
    $('#keyword').on('keyup', function (e) {
      if (e.keyCode === 13) fn_search(1);   // 엔터로 검색
    });
  });

  // ===== 목록 조회(검색, 페이지 이동) =====
  function fn_search(page) {
    currentPage = page;
    $.ajax({
      url: '/board/list.do',
      type: 'GET',
      data: { keyword: $('#keyword').val(), page: page, size: PAGE_SIZE },
      dataType: 'json',
      success: function (res) {
        var html = '';
        if (res.list.length === 0) {
          html = '<tr><td colspan="4">검색 결과가 없습니다.</td></tr>';
        }
        $.each(res.list, function (i, b) {
          // 문자열을 이어 붙여 HTML을 직접 만든다. 제목을 이스케이프하지 않으면 XSS 위험이 있다.
          html += '<tr onclick="fn_detail(' + b.seq + ')">'
               +  '<td>' + b.seq + '</td><td>' + b.title + '</td>'
               +  '<td>' + b.writer + '</td><td>' + b.regDate + '</td></tr>';
        });
        $('#tbody').html(html);          // 목록 영역을 통째로 갈아 끼운다
        fn_paging(res.totalCount);       // 페이징도 직접 다시 그린다
      }
    });
  }

  // ===== 페이징 버튼 그리기 =====
  function fn_paging(totalCount) {
    var totalPage = Math.ceil(totalCount / PAGE_SIZE);
    var html = '';
    for (var p = 1; p <= totalPage; p++) {
      html += '<button onclick="fn_search(' + p + ')"' + (p === currentPage ? ' disabled' : '') + '>' + p + '</button> ';
    }
    $('#paging').html(html);
  }

  // ===== 상세 =====
  function fn_detail(seq) {
    $.ajax({
      url: '/board/detail.do',
      data: { seq: seq },
      dataType: 'json',
      success: function (b) {
        $('#seq').val(b.seq);                       // 선택한 글 번호를 hidden에 보관
        $('#detailTitle').text(b.title);
        $('#detailWriter').text(b.writer + ' · ' + b.regDate);
        $('#detailContent').text(b.content);
        $('#listArea').hide();
        $('#detailArea').show();
      }
    });
  }

  // ===== 영역 전환 =====
  function fn_goWrite() {
    $('#writeForm')[0].reset();
    $('#listArea').hide();
    $('#writeArea').show();
  }

  function fn_goList() {
    $('#detailArea, #writeArea').hide();
    $('#listArea').show();
  }

  // ===== 저장 =====
  function fn_save() {
    if ($.trim($('#title').val()) === '') {
      alert('제목을 입력하세요.');
      $('#title').focus();
      return;
    }
    $.ajax({
      url: '/board/insert.do',
      type: 'POST',
      data: $('#writeForm').serialize(),
      dataType: 'json',
      success: function () {
        alert('저장되었습니다.');
        fn_goList();
        fn_search(1);
      }
    });
  }

  // ===== 삭제 =====
  function fn_delete() {
    if (!confirm('삭제하시겠습니까?')) return;
    $.ajax({
      url: '/board/delete.do',
      type: 'POST',
      data: { seq: $('#seq').val() },
      dataType: 'json',
      success: function () {
        alert('삭제되었습니다.');
        fn_goList();
        fn_search(currentPage);
      }
    });
  }
</script>
</head>
<body>
<h2>게시판</h2>

<%-- 목록 영역: 처음 열 때는 서버가 그려서 보낸다 --%>
<div id="listArea">
  <input type="text" id="keyword" placeholder="제목·작성자">
  <button onclick="fn_search(1)">검색</button>
  <button onclick="fn_goWrite()">글쓰기</button>
  <table border="1">
    <thead><tr><th>번호</th><th>제목</th><th>작성자</th><th>작성일</th></tr></thead>
    <tbody id="tbody">
      <c:forEach items="${list}" var="b">
        <tr onclick="fn_detail(${b.seq})">
          <td>${b.seq}</td><td><c:out value="${b.title}"/></td><td>${b.writer}</td><td>${b.regDate}</td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
  <div id="paging">
    <c:forEach begin="1" end="${totalPage}" var="p">
      <button onclick="fn_search(${p})" <c:if test="${p == 1}">disabled</c:if>>${p}</button>
    </c:forEach>
  </div>
</div>

<%-- 상세 영역: 처음에는 숨겨 두고 fn_detail()이 채워서 보여 준다 --%>
<div id="detailArea" style="display:none">
  <input type="hidden" id="seq">
  <h3 id="detailTitle"></h3>
  <p id="detailWriter"></p>
  <p id="detailContent"></p>
  <button onclick="fn_goList()">목록</button>
  <button onclick="fn_delete()">삭제</button>
</div>

<%-- 글쓰기 영역 --%>
<div id="writeArea" style="display:none">
  <form id="writeForm">
    <p>제목 <input type="text" id="title" name="title"></p>
    <p>작성자 <input type="text" name="writer"></p>
    <p>내용 <textarea name="content"></textarea></p>
  </form>
  <button onclick="fn_save()">저장</button>
  <button onclick="fn_goList()">취소</button>
</div>
</body>
</html>
