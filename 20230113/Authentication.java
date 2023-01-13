package icia.js.hoonzzang.services.auth;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;

import icia.js.hoonzzang.beans.EmployeesBean;
import icia.js.hoonzzang.SimpleTransactionManager;
import icia.js.hoonzzang.TransactionAssistant;
import icia.js.hoonzzang.beans.CategoriesBean;
import icia.js.hoonzzang.beans.GroupBean;
import icia.js.hoonzzang.beans.StoreBean;
import icia.js.hoonzzang.utils.Encryption;
import lombok.extern.slf4j.Slf4j;

/* Authentication + Memeber Join */
@Service
@Slf4j
public class Authentication extends TransactionAssistant {
	@Autowired
	private Encryption enc;
	private SimpleTransactionManager tranManager;
	
	public Authentication() {}
	
	/* Ajax 방식의 요청 컨트롤러 */
	public void backController(int serviceCode, Model model){
		switch(serviceCode) {
		case 20:
			//try {
				this.groupRegistration(model);
			//} catch (Exception e) {e.printStackTrace();}
			break;
		case 21:
			this.groupNameDuplicateCheckCtl(model);
			break;
		case 22:
			this.deleteTempGroupName(model);
			break;
		case 23:
			this.storeCodeDuplicateCheckCtl(model);
			break;
		}
	}
	
	/* View 방식의 요청 컨트롤러 */
	public void backController(int serviceCode, ModelAndView mav) {
		switch(serviceCode) {
		case 1:
			
			break;
		}
	}
	
	/* 사업자등록번호 중복 검사 */
	private void storeCodeDuplicateCheckCtl(Model model) {
		GroupBean group = (GroupBean)model.getAttribute("group");
		String message = null;
		
		this.tranManager = this.getTransaction(true);
		try {
			this.tranManager.tranStart();
			if(!this.convertToBoolean(this.sqlSession.selectOne("isStoreCode", group))) {
				message = "사업자등록번호 정상:등록 가능한 사업자 등록 번호입니다.";
			}else {
				group.getStoreInfoList().get(0).setStoreCode(null);
				message = "사업자등록번호 오류:등록 불가능한 사업자 등록 번호입니다.\n확인 후 다시 입력해주세요";
			}
			group.setMessage(message);
		}catch(Exception e) {
			e.printStackTrace();
		}finally {this.tranManager.tranEnd();}
	}
	
	/* GroupName 중복검사 후 임시 테이블에 저장 */
	private void groupNameDuplicateCheckCtl(Model model) {
		ArrayList<StoreBean> storeList = null;
		StoreBean store = null;
		ArrayList<EmployeesBean> empList = null;
		EmployeesBean emp = null;
		ArrayList<CategoriesBean> cateList = null;
		CategoriesBean cate = null;
		
		GroupBean group = (GroupBean)model.getAttribute("group");
		// 중복검사 (SG + TEMP)
		this.tranManager = getTransaction(false);
		try {
			this.tranManager.tranStart();
			if(!this.convertToBoolean(
					this.sqlSession.selectOne("isGroupName", group))) {
				empList = new ArrayList<EmployeesBean>();
				emp = new EmployeesBean();
				empList.add(emp);
				cateList = new ArrayList<CategoriesBean>();
				cate = new CategoriesBean();
				cateList.add(cate);
				storeList = new ArrayList<StoreBean>();
				store = new StoreBean();
				store.setCateList(cateList);
				store.setEmpList(empList);
				storeList.add(store);
				group.setStoreInfoList(storeList);

				/* Temp 테이블에 ins << 동시성 이슈 */
				if(!this.convertToBoolean(this.sqlSession.insert("insTemp", group))) {
					group.setMessage("네트워크 오류:네트워크가 불안정합니다. 잠시 후 다시 시도해주세요");
				}
				this.tranManager.commit();
			}else { 
				group.setMessage("그룹네임 중복 오류:이미 사용중인 그룹명입니다.");
				group.setGroupName(null);
			}
		}catch(Exception e) { e.printStackTrace();
			this.tranManager.rollback();
		}finally {this.tranManager.tranEnd();}
	}
	
	private void groupRegistration(Model model) {
		GroupBean group = (GroupBean)model.getAttribute("group");
		String message = "그룹등록 오류:죄송합니다.\n예기치 않은 오류가 발생하였습니다.\n 잠시 후 다시 시도해 주세요";
		
		/* AES 암호화 처리 */
		String aesString;
		try {
			aesString = enc.aesEncode(group.getGroupCeo(), group.getStoreInfoList().get(0).getStoreCode() + group.getGroupName());
			group.setGroupCeo(aesString);
			group.getStoreInfoList().get(0).getEmpList().get(0).setEmpName(aesString);
		} catch (Exception e) {	e.printStackTrace();}
		
		
		/* SHA 암호화 처리 */
		String encPassword = enc.encode(group.getGroupPin());
		group.setGroupPin(encPassword);
		group.getStoreInfoList().get(0).getEmpList().get(0).setEmpPin(encPassword);
		
		/* Transaction Start */
		try {
			this.tranManager = getTransaction(false);
			this.tranManager.tranStart();
			
			if(this.convertToBoolean(this.sqlSession.insert("insGroup", group))){
				//group.getStoreInfoList().get(0).setStoreCode("1234567890");
				if(this.convertToBoolean(this.sqlSession.insert("insStore", group))) {
					if(this.convertToBoolean(this.sqlSession.insert("insCate", group))){
						if(this.convertToBoolean(this.sqlSession.insert("insEmp", group))) {
							this.tranManager.commit();
						}else {group.setMessage(message);}
					}else {group.setMessage(message);}
				}else {group.setMessage(message);}
			}else {group.setMessage(message);}
		}catch(Exception e) {this.tranManager.rollback();
		}finally {
			this.tranManager.tranEnd();
			this.deleteTempGroupName(model, true);
		}
	}
	
	/* 임시 저장된 GroupName 삭제 */
	private void deleteTempGroupName(Model model, boolean...isReg) {
		GroupBean group = (GroupBean)model.getAttribute("group");
		String message = null;
		boolean delResult;
		
		
		try {	
			if(group.getGroupName() != "") {
				delResult = this.convertToBoolean(this.sqlSession.delete("delTemp", group));
				if(isReg.length > 0 && isReg[0] == true) {
					try {
						message = (delResult)? "회원가입성공:"+ this.enc.aesDecode(group.getGroupCeo(), group.getStoreInfoList().get(0).getStoreCode() + group.getGroupName())  +"님 회원가입을 감사드립니다.\n아래의 정보로 로그인 해주세요.\n"
								+ "[GroupCode >>> "+ group.getGroupCode() +"]\n"
								+ "[StoreCode >>> "+ group.getStoreInfoList().get(0).getStoreCode() +"]\n"
								+ "[EmployeeCode >>> "+ group.getStoreInfoList().get(0).getEmpList().get(0).getEmpCode() +"]": "회원가입 오류:일시적인 장애로 회원가입이 되지 않았습니다. 잠시후 다시 시도해 주세요.";
					} catch (Exception e) {e.printStackTrace();}	
				} else message = (delResult)? "정상처리:이전에 입력된 그룹네임이 삭제되었습니다.": "그룹네임 삭제 오류:이전에 입력된 그룹네임이 삭제되지 않아 이전 그룹네임을 사용할 수 없습니다.";

				/* 그룹네임 삭제 후 기존 입력 정보 지우기 */
				group.setMessage(message);
				group.setGroupName(null);
				group.setGroupPin(null);
				group.setGroupCeo(null);
				group.setStoreInfoList(null);
			}
			
		}catch(Exception e) {}
	}
	
	/* boolean 변환 */
	private boolean convertToBoolean(int value) {
		return (value >= 1)? true: false;
	}
	
	
	
	/* Registration Using AOP */
	/*
	@Transactional(rollbackFor=Exception.class)
	public void groupRegistration(Model model) throws Exception {
		GroupBean group = (GroupBean)model.getAttribute("group");
		
		try {
			String aesString = enc.aesEncode(group.getGroupCeo(), group.getStoreInfoList().get(0).getStoreCode() + group.getGroupName());
			group.setGroupCeo(aesString);
			group.getStoreInfoList().get(0).getEmpList().get(0).setEmpName(aesString);
		} catch (Exception e) {	e.printStackTrace(); throw new Exception();} 
		
		String encPassword = enc.encode(group.getGroupPin());
		group.setGroupPin(encPassword);
		group.getStoreInfoList().get(0).getEmpList().get(0).setEmpPin(encPassword);
		
		if(this.convertToBoolean(this.session.insert("insGroup", group))){
			//group.getStoreInfoList().get(0).setStoreCode("1234567890");
			if(this.convertToBoolean(this.session.insert("insStore", group))) {
				if(this.convertToBoolean(this.session.insert("insCate", group))){
					if(this.convertToBoolean(this.session.insert("insEmp", group))) {
						this.deleteTempGroupName(model, true);
					}
				}else {}
			}else {}
		}
	}
	*/
}
