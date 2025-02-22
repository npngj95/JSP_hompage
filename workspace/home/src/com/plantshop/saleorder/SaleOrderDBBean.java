package com.plantshop.saleorder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

public class SaleOrderDBBean {
	Connection conn = null;
	Statement stmt = null;
	PreparedStatement pstmt = null;
	ResultSet rs = null;
	ResultSet rs2 = null;
	String sql = null;
	
	private SaleOrderDBBean () {};
	
	private static SaleOrderDBBean instance = new SaleOrderDBBean();
	
	public static SaleOrderDBBean getInstance() {
		return instance;
	}
	
	private Connection getConnection() throws Exception {
		Context context = new InitialContext();
		DataSource ds = (DataSource)context.lookup("java:comp/env/jdbc/plantshop");
		return ds.getConnection(); 
	}
	
	private void close(Connection conn, Statement st) {
		if(st != null) try {st.close();} catch(Exception e) { e.printStackTrace();}
		if(conn != null) try {conn.close();} catch(Exception e) { e.printStackTrace();}
	}
	
	private void close(Connection conn, Statement st, ResultSet rs) {
		if(rs != null) try {rs.close();} catch(Exception e) { e.printStackTrace();}
		if(st != null) try {st.close();} catch(Exception e) { e.printStackTrace();}
		if(conn != null) try {conn.close();} catch(Exception e) { e.printStackTrace();}
	}
	
	private void close(Connection conn, Statement st, ResultSet rs, ResultSet rs2) {
		if(rs2 != null) try {rs.close();} catch(Exception e) { e.printStackTrace();}
		if(rs != null) try {rs.close();} catch(Exception e) { e.printStackTrace();}
		if(st != null) try {st.close();} catch(Exception e) { e.printStackTrace();}
		if(conn != null) try {conn.close();} catch(Exception e) { e.printStackTrace();}
	}
	
//	**************************************************************
	
	public List<SaleOrderDataBean> getSaleorderList(String condition) {
		List<SaleOrderDataBean> saleOrderList= new ArrayList<SaleOrderDataBean>();
		SaleOrderDataBean saleOrder = null;
		try {
			conn = this.getConnection();
			sql = "select * from saleorder " + condition;
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			
			while(rs.next()) {
				saleOrder = new SaleOrderDataBean();
				saleOrder.setId(rs.getInt("id"));
				saleOrder.setUid(rs.getString("uid"));
				saleOrder.setName(rs.getString("name"));
				saleOrder.setOrderdate(rs.getString("orderdate"));
				saleOrder.setAddress(rs.getString("address"));
				saleOrder.setTel(rs.getString("tel"));
				saleOrder.setPay(rs.getString("pay"));
				saleOrder.setCardno(rs.getString("cardno"));
				saleOrder.setProductcount(rs.getInt("productcount"));
				saleOrder.setTotal(rs.getInt("total"));
				
				saleOrderList.add(saleOrder);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("--> getSaleorder() 메소드 오류");
		} finally {
			this.close(conn, pstmt, rs);
		}
		return saleOrderList;
	}
	
	// * 상품 상세보기에서 바로 구매 
	public int insertSaleorderDirect(SaleOrderDataBean article, int pid, int qty) throws SQLException {
		int check1 = -1;
		int check2 = -1; // 재고량 체크 - rollback 처리 후 실패시 -1 리턴
		int saleId = -1; // 정상주문 완료 시 주문번호인 saleId 리턴
		int itemno = 0;
		try {
			conn = this.getConnection();
			// 트랜잭션(transaction) 방법 - All or Nothing
			// 1. 자동커밋을 꺼준다.
			conn.setAutoCommit(false);
			
			sql = "select ifnull(max(id), 0)+1 from saleorder"; 
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			
			// saleorder 테이블의 maxId를 생성
			if(rs.next()) saleId = rs.getInt(1);
			
			// order에서 넘어온 정보를 saleorder 테이블에 삽입
			sql = "insert into saleorder(id, uid, name, orderdate, address, tel, pay, cardno, productcount, total) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, saleId);
			pstmt.setString(2, article.getUid());
			pstmt.setString(3, article.getName());
			pstmt.setString(4, article.getOrderdate());
			pstmt.setString(5, article.getAddress());
			pstmt.setString(6, article.getTel());
			pstmt.setString(7, article.getPay());
			pstmt.setString(8, article.getCardno()); // 없으면 null
			pstmt.setInt(9, article.getProductcount());
			pstmt.setInt(10, article.getTotal());
			check1 = pstmt.executeUpdate();
			
			if(check1 > 0) {
				// 주문 바로 하기 했을 때 - 구매하는 상품이 있는지 확인
				System.out.println("바로구매처리");
				sql = "select * from product where id=?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setInt(1, pid);
				rs = pstmt.executeQuery();
				
				if(rs.next()) {
					itemno++;
					// 1. product 테이블의 stock과 주문수량의 유효성 검사
					String pro_pname = rs.getString("pname"); // 상품의 이름 검색
					int price = rs.getInt("price"); // 상품의 가격
					int stock = rs.getInt("stock"); // 상품의 재고량 검색
					
					
					if(qty > stock) { // 품목의 재고량이 주문량 보다 많을 때 처리 - saleorder, item 테이블에서 주문 삭제하고, cart_list.jsp로 돌아감
						// 트랜잭션 설정 이전 상태로 되돌아감.
						conn.rollback();
						return check2 = -1;
						
					} else { // 상품의 재고량이 주문량보다 많거나 같을 때 처리			
						// item 테이블 주문 품목을 추가
						sql = "insert into item(orderid, itemno, productid, pname, quantity, price) values(?, ?, ?, ?, ?, ?)";
						pstmt = conn.prepareStatement(sql);
						pstmt.setInt(1, saleId);
						pstmt.setInt(2, itemno);
						pstmt.setInt(3, pid);
						pstmt.setString(4, pro_pname);
						pstmt.setInt(5, qty);
						pstmt.setInt(6, price);
						pstmt.executeUpdate();
						
						// < 정상적으로 처리가 끝났을 때 처리하는 부분 >
						// 1. 주문을 했으므로 주문 수량 만큼을 product 테이블의 재고량에서 빼줌
						// product 테이블에서 재고량을 뺴줌, 판매량에는 더해 준다.
						sql = "update product set stock=stock-?, salecount=salecount+? where id=?";
						pstmt = conn.prepareStatement(sql);
						pstmt.setInt(1, qty);
						pstmt.setInt(2, qty);
						pstmt.setInt(3, pid);
						pstmt.executeUpdate();	
					}
				}
				
			}
		} catch(Exception e) {
			conn.rollback();
			e.printStackTrace();
			System.out.println("--> insertSaleorderDirect() 메소드 오류");
		} finally {
			// 자동 커밋을 꼭 켜줘야함.
			conn.setAutoCommit(true);
			this.close(conn, pstmt, rs, rs2);
		}
		
		return saleId;
	}

	// * 장바구니에서 구매
	public int insertSaleorderCart(SaleOrderDataBean article, int pid) throws SQLException {
		int check1 = -1;
		int check2 = -1; // 재고량 체크 - rollback 처리 후 실패시 -1 리턴
		int saleId = -1; // 정상주문 완료 시 주문번호인 saleId 리턴
		int itemno = 0;
		try {
			conn = this.getConnection();
			// 트랜잭션(transaction) 방법 - All or Nothing
			// 1. 자동커밋을 꺼준다.
			conn.setAutoCommit(false);
			
			sql = "select ifnull(max(id), 0)+1 from saleorder"; // 주문번호가 각 아이디마다 1번으로 해야되는거 아닌가...?
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			
			// saleorder 테이블의 maxId를 생성
			saleId = 0;
			if(rs.next()) saleId = rs.getInt(1);
			
			// order에서 넘어온 정보를 saleorder 테이블에 삽입
			sql = "insert into saleorder(id, uid, name, orderdate, address, tel, pay, cardno, productcount, total) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, saleId);
			pstmt.setString(2, article.getUid());
			pstmt.setString(3, article.getName());
			pstmt.setString(4, article.getOrderdate());
			pstmt.setString(5, article.getAddress());
			pstmt.setString(6, article.getTel());
			pstmt.setString(7, article.getPay());
			pstmt.setString(8, article.getCardno()); // 없으면 null
			pstmt.setInt(9, article.getProductcount());
			pstmt.setInt(10, article.getTotal());
			check1 = pstmt.executeUpdate();
			
			if(check1 > 0) {
				// 장바구니에 있는 물건을 살 때 - pid가 0이면 전체구매 pid를 가져오면 개별구매
				System.out.println("장바구니처리");
				if(pid == 0) {
					sql = "select pid, pname, quantity, price from cart where uid=?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, article.getUid());
				} else {
					sql = "select pid, pname, quantity, price from cart where pid=? and uid=?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setInt(1, pid);
					pstmt.setString(2, article.getUid());
				}
				
				rs = pstmt.executeQuery();
				while(rs.next()) {
					itemno++;
					// 카트 테이블의 데이터 검색
					int cart_pid = rs.getInt("pid"); // 상품의 고유아이디
					String cart_pname = rs.getString("pname"); // 상품의 이름 검색
					int cart_qty = rs.getInt("quantity"); // 상품의 주문수량
					int cart_price = rs.getInt("price"); // 상품의 가격
					
					// product 테이블의 id 에 따른 재고량 검색
					// product 테이블의 stock과 cart테이블의 주문수량의 유효성 검사
					sql = "select stock from product where id=?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setInt(1, rs.getInt("pid")); // cart테이블의 pid를 가지고 product테이블의 재고량을 검색
					rs2 = pstmt.executeQuery();
					
					int stock = 0;
					if (rs2.next()) stock = rs2.getInt("stock"); // 상품의 재고량 검색
					
					if(cart_qty > stock) { // 품목의 재고량이 주문량 보다 많을 때 처리 - saleorder, item 테이블에서 주문 삭제하고, cart_list.jsp로 돌아감
						// 트랜잭션 설정 이전 상태로 되돌아감.
						conn.rollback();				
						return check2 = -1;
						
					} else { // 상품의 재고량이 주문량보다 많거나 같을 때 처리			
						// item 테이블 주문 품목을 추가
						sql = "insert into item(orderid, itemno, productid, pname, quantity, price) values(?, ?, ?, ?, ?, ?)";
						pstmt = conn.prepareStatement(sql);
						pstmt.setInt(1, saleId);
						pstmt.setInt(2, itemno);
						pstmt.setInt(3, cart_pid);
						pstmt.setString(4, cart_pname);
						pstmt.setInt(5, cart_qty);
						pstmt.setInt(6, cart_price);
						pstmt.executeUpdate();
						
						// < 정상적으로 처리가 끝났을 때 처리하는 부분 >
						// 1. 주문을 했으므로 주문 수량 만큼을 product 테이블의 재고량에서 빼줌
						// product 테이블에서 재고량을 뺴줌, 판매량에는 더해 준다.
						sql = "update product set stock=stock-?, salecount=salecount+? where id=?";
						pstmt = conn.prepareStatement(sql);
						pstmt.setInt(1, cart_qty);
						pstmt.setInt(2, cart_qty);
						pstmt.setInt(3, cart_pid);
						pstmt.executeUpdate();	
					}
				}
			}
			
			// saleorder, item 테이블에 cart 테이블의 정보를 담았다면
			// cart 테이블의 정보를 삭제
			if(pid == 0) { // 전체 장바구니 삭제
				System.out.println("전체");
				sql = "delete from cart where uid=?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, article.getUid());
			} else { // 해당 주문목록의 장바구니 삭제
				System.out.println("개별");
				sql = "delete from cart where pid=? and uid=?";
				pstmt = conn.prepareStatement(sql);
				pstmt.setInt(1, pid);
				pstmt.setString(2, article.getUid());
			}
			pstmt.executeUpdate();
			
		} catch(Exception e) {
			conn.rollback();
			e.printStackTrace();
			System.out.println("--> insertSaleorderCart() 메소드 오류");
		} finally {
			conn.setAutoCommit(true);
			this.close(conn, pstmt, rs, rs2);
		}
		
		return saleId;
	}
	
	// *************************
	// 관리자 모든 주문정보 얻기
	public List<SaleOrderDataBean> getAllSaleorderList() {
		List<SaleOrderDataBean> saleOrderList= new ArrayList<SaleOrderDataBean>();
		SaleOrderDataBean saleOrder = null;
		try {
			conn = this.getConnection();
			sql = "select * from saleorder order by orderdate";
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			
			while(rs.next()) {
				saleOrder = new SaleOrderDataBean();
				saleOrder.setId(rs.getInt("id"));
				saleOrder.setUid(rs.getString("uid"));
				saleOrder.setName(rs.getString("name"));
				saleOrder.setOrderdate(rs.getString("orderdate"));
				saleOrder.setAddress(rs.getString("address"));
				saleOrder.setTel(rs.getString("tel"));
				saleOrder.setPay(rs.getString("pay"));
				saleOrder.setCardno(rs.getString("cardno"));
				saleOrder.setProductcount(rs.getInt("productcount"));
				saleOrder.setTotal(rs.getInt("total"));
				
				saleOrderList.add(saleOrder);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("--> getSaleorder() 메소드 오류");
		} finally {
			this.close(conn, pstmt, rs);
		}
		return saleOrderList;
	}
	
	
}
