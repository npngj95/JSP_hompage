<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="java.sql.*, java.text.*, java.util.*"%>
<%@page import="com.plantshop.cart.*, com.plantshop.product.*"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>장바구니</title>
<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.5.0/jquery.min.js"></script>

<link href="./css/cart_main.css" rel="stylesheet">
<link href="./css/cart_list.css" rel="stylesheet">
<script src="./js/cart_list.js"></script>
</head>
<%
request.setCharacterEncoding("utf-8");

if (session.getAttribute("login_id") == null) {
	out.print("<script>alert('로그인을 후 주문목록을 사용해주세요!!'); location='../member/loginForm.jsp';</script>");
	return;
}
%>
<body>
<div id="container">
	<%@ include file="../module/top.jsp"%>
	<section>
		<article id="content">
			<div class='move_page'>
				<a href="../member/select.jsp">개인정보 조회</a> <a id="current_page"
					href="./cart_list.jsp">나의 장바구니</a> <a
					href="../order/order_list.jsp?uid=<%=login_id%>">나의 주문 목록</a>
			</div>
			<%
				String pageNum = "1";
				String category = "";
				String searchPname = "";

				if (request.getParameter("pageNum") != null && !request.getParameter("pageNum").equals("")) {
					pageNum = request.getParameter("pageNum");
				}

				if (request.getParameter("category") != null && !request.getParameter("category").equals("")) {
					category = request.getParameter("category");
				}

				if (request.getParameter("searchPname") != null && !request.getParameter("searchPname").equals("")) {
					searchPname = request.getParameter("searchPname");
				}

				int qty = 0; // 수량
				int productcount = 0; // 품목수
				NumberFormat nf = NumberFormat.getInstance();
				String price_nf = ""; // 포맷형식을 가진 가격
				String sum = ""; // 수량 * 가격

				//DB 처리를 위한 변수
				String sname = "";
				String cart_pname = ""; // 카트에 담긴 상품의 이름을 보여주기 위함
				String uid = ""; // cart테이블로부터 각 상품의 회원 아이디를 받아옴
				int db_pid = 0; // 상품번호
				String small = "";

				int price = 0;
				int total = 0; // (수량 * 가격)을 누적한 값

				String url = "/plantshop"; // server.xml에서 로컬(절대)경로를 변경한 경로

				out.print("<h2>" + login_name + "님의 장바구니 목록</h2>");
				out.print("<table><tr>");
				out.print("<th width='8%'>카트번호</th>");
				out.print("<th width='22%'>상품정보</th>");
				out.print("<th width='10%'>상품번호</th>");
				out.print("<th width='10%'>제조사</th>");
				out.print("<th width='12%'>주문 수량</th>");
				out.print("<th width='14%'>판매가</th>");
				out.print("<th width='14%'>합계</th>");
				out.print("<th width='10%'>상품주문</th></tr>");

				// 1. cart 테이블로 부터 처리 - DB
				CartDBBean cartDBBean = CartDBBean.getInstance();
				List<CartDataBean> cartList = cartDBBean.getCartList(login_id, 0); // 현재 로그인된 아이디로 카트정보 불러옴
				
				// 2. product 테이블로 부터 재고량 받기 위해
				ProductDBBean proDBBean = ProductDBBean.getInstance();
				ProductDataBean product = null;
				
				if (cartList.size() < 1) { // 장바구니에 담긴 상품이 없을 때
					out.print("<tr><th colspan='8'>장바구니 담긴 상품이 없습니다.</th></tr>");
				} else { //장바구니에 담긴 상품이 있을 때
					for (CartDataBean cart : cartList) {
						++productcount;
						uid = cart.getUid();
						db_pid = cart.getPid();
						cart_pname = cart.getPname();
						sname = cart.getSname();
						qty = cart.getQuantity();
						price = cart.getPrice();
						small = cart.getSmall();

						price_nf = nf.format(price); // 포맷된 가격			
						sum = nf.format(price * qty); // 수량 * 가격
						total += (price * qty); // 총금액

						product = proDBBean.getProduct(db_pid); // 재고량 얻기위함
						
						out.println("<form name='cart_form' method='post' action='cart_update.jsp' onsubmit='cartUpdate(this)'>");
						out.println("<input type='hidden' name='stock' value='" + product.getStock() + "'>");
						out.println("<input type='hidden' name='cart_pname' value='" + cart_pname + "'>");
						
						out.println("<input type='hidden' name='pid' value='" + db_pid + "'>");
						out.println("<input type='hidden' name='uid' value='" + uid + "'>");
						out.println("<input type='hidden' name='pageNum' value='" + pageNum + "'>");
						out.println("<input type='hidden' name='category' value='" + category + "'>");
						out.println("<input type='hidden' name='searchPname' value='" + searchPname + "'>");
						
						out.println("<tr>");
						out.println("<td class='center'>" + productcount + "</td>");
						out.println("<td><a href='../product/product_detail.jsp?id=" + db_pid + "'>");
						out.println("<img id='p_img' src='" + url + "/" + small + "' width='110px' height='150px'>");
						out.println("<div id='p_name'>" + cart_pname + "</div></a></td>");
						out.println("<td class='center'>" + db_pid + "</td>");
						out.println("<td class='center'>" + sname + "</td>");
						out.println("<td class='center'>");

						if (qty > 1) { // 수량이 2이상일 때 - 버튼 있음
							out.println("<input type='button' class='cahnge_qty' value='-' onclick='changeQty(this.form, -1)'>");
						} else { // 수량이 1이하 일때 - 버튼 없음
							out.println("<input type='button' class='cahnge_qty' value='-' style='visibility: hidden'>");
						}
						out.println("<input  type='number' id='quantity' name='quantity' size='1' value='" + qty + "'>&nbsp;");
						out.println("<input class='cahnge_qty' type='button'  value='+' onclick='changeQty(this.form, 1)'>");
						out.println("<br><input class='form_btn' type='submit' value='수정'>&nbsp;");
						// onclick 시 홀따옴표의 중요성!! 한번갑싸주고 location경로도 한번 감싸준다.
						out.println("<input class='form_btn' type='button' onclick=\"location='cart_delete.jsp?pageNum="
								+ pageNum + "&category=" + category + "&searchPname=" + searchPname + "&pid=" + db_pid
								+ "&uid=" + uid + "'\" value='삭제'>");
						
						out.println("</td>");
						out.println("<td class='center'>" + price_nf + "원</td>");
						out.println("<td class='center'>" + sum + "원</td>");
						
						// 개별주문
						out.println("<td class='center'><a href='../order/order.jsp?total=" + (price * qty)
								+ "&productcount=1&uid=" + uid + "&pid=" + db_pid + "'>");
						out.println("<img src='./images/buy.png' class='cart_img'></a></td>"); 
						out.println("</tr></form>");
					}
				}
				out.println("<tr class='end_row'><td colspan='8'>주문상품 수 : " + productcount + "개 품목&nbsp;&nbsp;/&nbsp;&nbsp;");
				out.println("총 합계 금액 : " + nf.format(total) + "원</td></tr>");
				out.println("</table>");

				out.println("<div id='move'><a href='../shop_list.jsp?pageNum=" + pageNum + "&category=" + category
						+ "&searchPname=" + searchPname + "'>◀ 계속 쇼핑하기</a>&nbsp;&nbsp;");
				out.println("<a href='../order/order.jsp?total=" + total + "&productcount=" + productcount + "&uid=" + uid
						+ "'>전체주문하기 ▶</a><div>");
			%>
		</article>
	</section>
	<%@ include file="../module/bottom.jsp"%>
</div>
</body>
</html>