<%-- 비교용: reservation-list.html과 같은 화면을 JSP로 만든 것. 이 장의 예제(jar)에서는 쓰이지 않는다. --%>
<%-- 따로 war로 만들어 실행해 본 결과는 output.txt 아래쪽에 있다. --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- JSTL 3.0(jakarta)부터 주소가 jakarta.tags.core로 바뀌었다. 예전 코드는 http://java.sun.com/jsp/jstl/core --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>예약 목록</title>
</head>
<body>
<h1>예약 목록</h1>

<c:if test="${not empty message}">
  <p><c:out value="${message}"/></p>
</c:if>

<table>
  <tr><th>번호</th><th>예약자(c:out)</th><th>예약자(그대로)</th><th>객실</th><th>숙박</th><th>상태</th></tr>
  <c:forEach var="r" items="${reservations}">
    <tr>
      <td>${r.id}</td>
      <%-- 사용자 입력은 c:out으로 찍는다. ${r.guestName}만 쓰면 이스케이프되지 않는다(비교용으로 둘 다 찍음). --%>
      <td><c:out value="${r.guestName}"/></td>
      <td>${r.guestName}</td>
      <td><c:out value="${r.roomName}"/></td>
      <td>${r.nights}박</td>
      <td>${r.status == 'CONFIRMED' ? '확정' : '취소'}</td>
    </tr>
  </c:forEach>
</table>

<c:if test="${empty reservations}">
  <p>예약이 없습니다.</p>
</c:if>

<a href="${pageContext.request.contextPath}/reservations/new">새 예약</a>
</body>
</html>
