package com.osh.rvs.action.partial;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionManager;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.osh.rvs.form.master.PartialBussinessStandardForm;
import com.osh.rvs.form.master.PartialForm;
import com.osh.rvs.form.partial.FactProductionFeatureForm;
import com.osh.rvs.form.partial.PartialWarehouseDetailForm;
import com.osh.rvs.form.partial.PartialWarehouseDnForm;
import com.osh.rvs.form.partial.PartialWarehouseForm;
import com.osh.rvs.service.PartialBussinessStandardService;
import com.osh.rvs.service.PartialService;
import com.osh.rvs.service.partial.FactProductionFeatureService;
import com.osh.rvs.service.partial.PartialCollationService;
import com.osh.rvs.service.partial.PartialWarehouseDetailService;
import com.osh.rvs.service.partial.PartialWarehouseDnSerice;
import com.osh.rvs.service.partial.PartialWarehouseService;

import framework.huiqing.action.BaseAction;
import framework.huiqing.bean.message.MsgInfo;
import framework.huiqing.common.util.AutofillArrayList;
import framework.huiqing.common.util.CommonStringUtil;
import framework.huiqing.common.util.copy.DateUtil;

/**
 * 零件核对
 *
 * @author liuxb
 *
 */
public class PartialCollationAction extends BaseAction {
	private final Logger log = Logger.getLogger(getClass());

	private final PartialService partialService = new PartialService();

	// 零件入库单
	private final PartialWarehouseService partialWarehouseService = new PartialWarehouseService();
	// 零件入库明细
	private final PartialWarehouseDetailService partialWarehouseDetailService = new PartialWarehouseDetailService();
	// 现品作业信息
	private final FactProductionFeatureService factProductionFeatureService = new FactProductionFeatureService();

	private final PartialBussinessStandardService partialBussinessStandardService = new PartialBussinessStandardService();

	private final PartialCollationService partialCollationService = new PartialCollationService();

	// 零件入库DN编号
	private final PartialWarehouseDnSerice partialWarehouseDnSerice = new PartialWarehouseDnSerice();

	/**
	 * 页面初始化
	 *
	 * @param mapping
	 * @param form
	 * @param req
	 * @param res
	 * @param conn
	 * @throws Exception
	 */
	public void init(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res, SqlSession conn) throws Exception {
		log.info("PartialCollationAction.init start");

		actionForward = mapping.findForward(FW_INIT);

		log.info("PartialCollationAction.init end");
	}

	public void jsinit(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res, SqlSession conn) throws Exception {
		log.info("PartialCollationAction.jsinit start");

		/* Ajax反馈对象 */
		Map<String, Object> callbackResponse = new HashMap<String, Object>();
		List<MsgInfo> errors = new ArrayList<MsgInfo>();

		// 进行中的作业信息
		FactProductionFeatureForm factProductionFeature = factProductionFeatureService.searchUnFinishProduction(req, conn);
		callbackResponse.put("unfinish", factProductionFeature);

		if (factProductionFeature != null) {
			String key = factProductionFeature.getPartial_warehouse_key();

			// 入库单信息
			PartialWarehouseForm partialWarehouseForm = partialWarehouseService.getByKey(key, conn);
			callbackResponse.put("partialWarehouse", partialWarehouseForm);

			// 作业内容
			String productionType = factProductionFeature.getProduction_type();

			// 当前作业单中所有零件
			List<PartialWarehouseDetailForm> list = partialWarehouseDetailService.searchByKey(key, conn);
			callbackResponse.put("allPartialList", list);

			// /过滤核对的数据
			List<PartialWarehouseDetailForm> partialWarehouseDetailList = partialCollationService.filterCollation(list, productionType);
			callbackResponse.put("partialWarehouseDetailList", partialWarehouseDetailList);

			// 查询零件入库DN编号
			List<PartialWarehouseDnForm> partialWarehouseDnList = partialWarehouseDnSerice.searchByKey(key, conn);
			callbackResponse.put("partialWarehouseDnList", partialWarehouseDnList);

			// 作业标准时间
			String leagal_overline = partialCollationService.getStandardTime(list, productionType, conn);
			callbackResponse.put("leagal_overline", leagal_overline);

			// 作业经过时间
			String spent_mins = partialCollationService.getSpentTimes(factProductionFeature, conn);
			callbackResponse.put("spent_mins", spent_mins);
		}

		/* 检查错误时报告错误信息 */
		callbackResponse.put("errors", errors);
		/* 返回Json格式响应信息 */
		returnJsonResponse(res, callbackResponse);

		log.info("PartialCollationAction.jsinit end");
	}

	/**
	 * 查询未核对零件入库单信息
	 *
	 * @param mapping
	 * @param form
	 * @param req
	 * @param res
	 * @param conn
	 * @throws Exception
	 */
	public void searchUnCollation(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res, SqlSession conn) throws Exception {
		log.info("PartialCollationAction.searchUnCollation start");

		/* Ajax反馈对象 */
		Map<String, Object> callbackResponse = new HashMap<String, Object>();
		List<MsgInfo> errors = new ArrayList<MsgInfo>();

		String step = "1";// 1:表示收货结束
		// 零件入库单信息
		List<PartialWarehouseForm> partialWarehouseList = partialWarehouseService.searchPartialWarehouseByStep(step, conn);
		callbackResponse.put("partialWarehouseList", partialWarehouseList);

		/* 检查错误时报告错误信息 */
		callbackResponse.put("errors", errors);
		/* 返回Json格式响应信息 */
		returnJsonResponse(res, callbackResponse);

		log.info("PartialCollationAction.searchUnCollation end");
	}

	/**
	 * 扫描检查
	 *
	 * @param mapping
	 * @param form
	 * @param req
	 * @param res
	 * @param conn
	 * @throws Exception
	 */
	public void checkScanner(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res, SqlSession conn) throws Exception {
		log.info("PartialCollationAction.checkScanner start");

		/* Ajax反馈对象 */
		Map<String, Object> callbackResponse = new HashMap<String, Object>();
		List<MsgInfo> errors = new ArrayList<MsgInfo>();

		String code = req.getParameter("code");

		// 零件信息
		PartialForm partialForm = partialService.getDetail(code, conn, errors);

		if (errors.size() == 0) {
			// 零件出入库工时标准
			PartialBussinessStandardForm partialBussinessStandardForm = partialBussinessStandardService.getPartialBussinessStandardBySpecKind(partialForm.getSpec_kind(), conn);
			callbackResponse.put("partialBussinessStandardForm", partialBussinessStandardForm);
		}

		callbackResponse.put("partialForm", partialForm);

		/* 检查错误时报告错误信息 */
		callbackResponse.put("errors", errors);
		/* 返回Json格式响应信息 */
		returnJsonResponse(res, callbackResponse);

		log.info("PartialCollationAction.checkScanner end");
	}

	/**
	 * 中断
	 *
	 * @param mapping
	 * @param form
	 * @param req
	 * @param res
	 * @param conn
	 * @throws Exception
	 */
	public void doBreak(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res, SqlSessionManager conn) throws Exception {
		log.info("PartialCollationAction.doBreak start");

		/* Ajax反馈对象 */
		Map<String, Object> callbackResponse = new HashMap<String, Object>();
		List<MsgInfo> errors = new ArrayList<MsgInfo>();

		// 进行中的作业信息
		FactProductionFeatureForm factProductionFeatureForm = factProductionFeatureService.searchUnFinishProduction(req, conn);

//		String factPfKey = factProductionFeatureForm.getFact_pf_key();
//		String key = factProductionFeatureForm.getPartial_warehouse_key();
//
//		// 入库单信息
//		PartialWarehouseForm partialWarehouseForm = partialWarehouseService.getByKey(key, conn);
//		String warehouseNo = partialWarehouseForm.getWarehouse_no();
//
//		Pattern p = Pattern.compile("(\\w+).(\\w+)\\[(\\d+)\\]");
//
//		List<PartialWarehouseDetailForm> list = new AutofillArrayList<PartialWarehouseDetailForm>(PartialWarehouseDetailForm.class);
//
//		Map<String, String[]> parameters = req.getParameterMap();
//
//		for (String parameterKey : parameters.keySet()) {
//			Matcher m = p.matcher(parameterKey);
//			if (m.find()) {
//				String entity = m.group(1);
//				if ("partial_warehouse_detail".equals(entity)) {
//					String column = m.group(2);
//					int icounts = Integer.parseInt(m.group(3));
//					String[] value = parameters.get(parameterKey);
//
//					if ("partial_id".equals(column)) {
//						list.get(icounts).setPartial_id(value[0]);
//					}else if ("seq".equals(column)) {
//						list.get(icounts).setSeq(value[0]);
//					}  else if ("collation_quantity".equals(column)) {
//						list.get(icounts).setCollation_quantity(value[0]);
//					} else if ("flg".equals(column)) {
//						list.get(icounts).setFlg(value[0]);
//					}
//
//					list.get(icounts).setKey(key);
//					list.get(icounts).setFact_pf_key(factPfKey);
//				}
//			}
//		}
//
//		//判断是否扫描了新零件
//		for(PartialWarehouseDetailForm partialWarehouseDetailForm : list){
//			String flg = partialWarehouseDetailForm.getFlg();
//
//			if ("1".equals(flg)) {
//				PartialWarehouseDnForm partialWarehouseDnForm =	partialWarehouseDnSerice.getPartialWarehouseDnByDnNo(warehouseNo + "E", conn);
//
//				if(partialWarehouseDnForm == null){
//					partialWarehouseDnForm = new PartialWarehouseDnForm();
//					partialWarehouseDnForm.setKey(key);
//					partialWarehouseDnForm.setSeq("0");
//					partialWarehouseDnForm.setWarehouse_date(DateUtil.toString(Calendar.getInstance().getTime(), DateUtil.DATE_PATTERN));
//					partialWarehouseDnForm.setDn_no(warehouseNo + "E");
//
//					//新建零件入库DN编号
//					partialWarehouseDnSerice.insert(partialWarehouseDnForm, conn);
//				}
//				break;
//			}
//		}
//
//		for (PartialWarehouseDetailForm partialWarehouseDetailForm : list) {
//			String flg = partialWarehouseDetailForm.getFlg();
//
//			if ("1".equals(flg)) {// 零件在此单中不存在
//				String collationQuantity = partialWarehouseDetailForm.getCollation_quantity();
//				partialWarehouseDetailForm.setQuantity(collationQuantity);
//				partialWarehouseDetailForm.setCollation_quantity(collationQuantity);
//				partialWarehouseDetailService.insert(partialWarehouseDetailForm, conn);
//			} else if ("0".equals(flg)) { //零件在此单中不存在,但是已经加入此单中
//				String collationQuantity = partialWarehouseDetailForm.getCollation_quantity();
//				partialWarehouseDetailForm.setQuantity(collationQuantity);
//				partialWarehouseDetailForm.setCollation_quantity(collationQuantity);
//				partialWarehouseDetailService.update(partialWarehouseDetailForm, conn);
//			} else {
//				partialWarehouseDetailService.update(partialWarehouseDetailForm, conn);
//			}
//		}

		// 更新处理结束时间
		factProductionFeatureService.updateFinishTime(factProductionFeatureForm, conn);

		/* 检查错误时报告错误信息 */
		callbackResponse.put("errors", errors);
		/* 返回Json格式响应信息 */
		returnJsonResponse(res, callbackResponse);

		log.info("PartialCollationAction.doBreak end");
	}

	/**
	 * 检查是否核对完毕
	 *
	 * @param mapping
	 * @param form
	 * @param req
	 * @param res
	 * @param conn
	 * @throws Exception
	 */
	public void checkCollationFinish(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res, SqlSession conn) throws Exception {
		log.info("PartialCollationAction.checkCollationFinish start");

		/* Ajax反馈对象 */
		Map<String, Object> callbackResponse = new HashMap<String, Object>();
		List<MsgInfo> errors = new ArrayList<MsgInfo>();

//		Pattern p = Pattern.compile("(\\w+).(\\w+)\\[(\\d+)\\]");
//		List<PartialWarehouseDetailForm> partialMapList = new AutofillArrayList<PartialWarehouseDetailForm>(PartialWarehouseDetailForm.class);
//		Map<String, String> partialMap = new HashMap<String, String>();
//		Map<String, String[]> parameters = req.getParameterMap();
//
//		for (String parameterKey : parameters.keySet()) {
//			Matcher m = p.matcher(parameterKey);
//			if (m.find()) {
//				String entity = m.group(1);
//				if ("partial_warehouse_detail".equals(entity)) {
//					String column = m.group(2);
//					String[] value = parameters.get(parameterKey);
//					int icounts = Integer.parseInt(m.group(3));
//					if ("partial_id".equals(column)) {
//						partialMapList.get(icounts).setPartial_id(value[0]);
//					} else if ("seq".equals(column)) {
//						partialMapList.get(icounts).setSeq(value[0]);
//					} else if ("collation_quantity".equals(column)) {
//						partialMapList.get(icounts).setCollation_quantity(value[0]);
//					}
//				}
//			}
//		}
//
//		for (PartialWarehouseDetailForm o : partialMapList) {
//			partialMap.put(o.getPartial_id() + "/" + o.getSeq(), o.getCollation_quantity());
//		}

		// 进行中的作业信息
		FactProductionFeatureForm factProductionFeatureForm = factProductionFeatureService.searchUnFinishProduction(req, conn);
		String key = factProductionFeatureForm.getPartial_warehouse_key();

		// 当前作业单中所有零件
		List<PartialWarehouseDetailForm> list = partialWarehouseDetailService.searchByKey(key, conn);
		for (PartialWarehouseDetailForm partialWarehouseDetailForm : list) {
//			String partialID = partialWarehouseDetailForm.getPartial_id();
//			String seq = partialWarehouseDetailForm.getSeq();

			// 页面核对的零件不用检查
//			if (partialMap.containsKey(partialID + "/" + seq)) {
//				continue;
//			}

			String factPfKey = partialWarehouseDetailForm.getFact_pf_key();
			// 未核对
			if (CommonStringUtil.isEmpty(factPfKey)) {
				MsgInfo error = new MsgInfo();
				error.setErrmsg("此单尚有零件" + partialWarehouseDetailForm.getCode() + "未核对完毕，不能完成核对。如要进行另一部分的核对，请先中断作业。");
				errors.add(error);
				break;
			}
		}
		if (errors.size() == 0) {
			boolean flg = false;
			for (PartialWarehouseDetailForm partialWarehouseDetailForm : list) {

//				String partialID = partialWarehouseDetailForm.getPartial_id();
//				String seq = partialWarehouseDetailForm.getSeq();

//				String mapKey = partialID +"/" + seq;

				// 数量
				String quantity = partialWarehouseDetailForm.getQuantity();

				// 核对数量
				String collationQuantity = null;

//				if (partialMap.containsKey(mapKey)) {
//					collationQuantity = Integer.valueOf(partialMap.get(mapKey));
//				} else {
					collationQuantity = partialWarehouseDetailForm.getCollation_quantity();
//				}

				// 核对数量不一致
				if (!quantity.equals(collationQuantity)) {
					flg = true;
					break;
				}
			}

			callbackResponse.put("differ", flg);
		}

		/* 检查错误时报告错误信息 */
		callbackResponse.put("errors", errors);
		/* 返回Json格式响应信息 */
		returnJsonResponse(res, callbackResponse);

		log.info("PartialCollationAction.checkCollationFinish end");

	}

	public void doFinish(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res, SqlSessionManager conn) throws Exception {
		log.info("PartialCollationAction.doFinish start");

		/* Ajax反馈对象 */
		Map<String, Object> callbackResponse = new HashMap<String, Object>();
		List<MsgInfo> errors = new ArrayList<MsgInfo>();

		//入库进展，2：核对完成
		String step = "2";

		// 进行中的作业信息
		FactProductionFeatureForm factProductionFeatureForm = factProductionFeatureService.searchUnFinishProduction(req, conn);
		//String factPfKey = factProductionFeatureForm.getFact_pf_key();
		String key = factProductionFeatureForm.getPartial_warehouse_key();

		// 入库单信息
		PartialWarehouseForm partialWarehouseForm = partialWarehouseService.getByKey(key, conn);
		//String warehouseNo = partialWarehouseForm.getWarehouse_no();


//		Pattern p = Pattern.compile("(\\w+).(\\w+)\\[(\\d+)\\]");
//		List<PartialWarehouseDetailForm> list = new AutofillArrayList<PartialWarehouseDetailForm>(PartialWarehouseDetailForm.class);
//
//		Map<String, String[]> parameters = req.getParameterMap();
//
//		for (String parameterKey : parameters.keySet()) {
//			Matcher m = p.matcher(parameterKey);
//			if (m.find()) {
//				String entity = m.group(1);
//				if ("partial_warehouse_detail".equals(entity)) {
//					String column = m.group(2);
//					int icounts = Integer.parseInt(m.group(3));
//					String[] value = parameters.get(parameterKey);
//
//					if ("partial_id".equals(column)) {
//						list.get(icounts).setPartial_id(value[0]);
//					} else if ("seq".equals(column)) {
//						list.get(icounts).setSeq(value[0]);
//					} else if ("collation_quantity".equals(column)) {
//						list.get(icounts).setCollation_quantity(value[0]);
//					} else if ("flg".equals(column)) {
//						list.get(icounts).setFlg(value[0]);
//					}
//
//					list.get(icounts).setKey(key);
//					list.get(icounts).setFact_pf_key(factPfKey);
//				}
//			}
//		}
//
//
//		//判断是否扫描了新零件
//		for(PartialWarehouseDetailForm partialWarehouseDetailForm : list){
//			String flg = partialWarehouseDetailForm.getFlg();
//
//			if ("1".equals(flg)) {
//				PartialWarehouseDnForm partialWarehouseDnForm =	partialWarehouseDnSerice.getPartialWarehouseDnByDnNo(warehouseNo + "E", conn);
//
//				if(partialWarehouseDnForm == null){
//					partialWarehouseDnForm = new PartialWarehouseDnForm();
//					partialWarehouseDnForm.setKey(key);
//					partialWarehouseDnForm.setSeq("0");
//					partialWarehouseDnForm.setWarehouse_date(DateUtil.toString(Calendar.getInstance().getTime(), DateUtil.DATE_PATTERN));
//					partialWarehouseDnForm.setDn_no(warehouseNo + "E");
//
//					//新建零件入库DN编号
//					partialWarehouseDnSerice.insert(partialWarehouseDnForm, conn);
//				}
//				break;
//			}
//		}
//
//		for (PartialWarehouseDetailForm partialWarehouseDetailForm : list) {
//			String flg = partialWarehouseDetailForm.getFlg();
//
//			if ("1".equals(flg)) {// 零件在此单中不存在
//				String collationQuantity = partialWarehouseDetailForm.getCollation_quantity();
//				partialWarehouseDetailForm.setQuantity(collationQuantity);
//				partialWarehouseDetailForm.setCollation_quantity(collationQuantity);
//				partialWarehouseDetailService.insert(partialWarehouseDetailForm, conn);
//			} else if ("0".equals(flg)) {
//				String collationQuantity = partialWarehouseDetailForm.getCollation_quantity();
//				partialWarehouseDetailForm.setQuantity(collationQuantity);
//				partialWarehouseDetailForm.setCollation_quantity(collationQuantity);
//				partialWarehouseDetailService.update(partialWarehouseDetailForm, conn);
//			} else {
//				partialWarehouseDetailService.update(partialWarehouseDetailForm, conn);
//			}
//		}

		// 更新处理结束时间
		factProductionFeatureService.updateFinishTime(factProductionFeatureForm, conn);

		int b1= 0;
		List<PartialWarehouseDetailForm> allPartialList = partialWarehouseDetailService.searchByKey(key, conn);
		for (PartialWarehouseDetailForm partialWarehouseDetailForm : allPartialList) {
			// 上架
			BigDecimal onShelf = new BigDecimal(partialWarehouseDetailForm.getOn_shelf());
			if (onShelf.compareTo(BigDecimal.ZERO) < 0){// 【B1：核对+上架】
				b1++;
			}
		}

		//订购单零件都是B1时，step为3
		if(b1 == allPartialList.size()){
			step = "3";
		}

		// 结束核对单
		// 入库进展
		partialWarehouseForm.setStep(step);

		// 更新入库进展
		partialWarehouseService.updateStep(partialWarehouseForm, conn);

		/* 检查错误时报告错误信息 */
		callbackResponse.put("errors", errors);
		/* 返回Json格式响应信息 */
		returnJsonResponse(res, callbackResponse);

		log.info("PartialCollationAction.doFinish end");
	}


	/**
	 * 检查选择的作业内容在入库单中是否匹配
	 * @param mapping
	 * @param form
	 * @param req
	 * @param res
	 * @param conn
	 * @throws Exception
	 */
	public void checkUnMatch(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res, SqlSession conn)throws Exception{
		log.info("PartialCollationAction.checkUnMatch start");

		/* Ajax反馈对象 */
		Map<String, Object> callbackResponse = new HashMap<String, Object>();
		List<MsgInfo> errors = new ArrayList<MsgInfo>();

		//入库单KEY
		String key  = req.getParameter("partial_warehouse_key");

		//作业内容
		String productionType  = req.getParameter("production_type");

		// 当前作业单中所有零件
		List<PartialWarehouseDetailForm> list = partialWarehouseDetailService.searchByKey(key, conn);
		// 过滤核对的数据
		List<PartialWarehouseDetailForm> partialWarehouseDetailList = partialCollationService.filterCollation(list, productionType);

		boolean matchFlg = true;

		if(partialWarehouseDetailList.size() == 0){
			matchFlg = false;
		}

		callbackResponse.put("matchFlg", matchFlg);

		/* 检查错误时报告错误信息 */
		callbackResponse.put("errors", errors);
		/* 返回Json格式响应信息 */
		returnJsonResponse(res, callbackResponse);

		log.info("PartialCollationAction.checkUnMatch end");
	}

	/**
	 * 更新核对数量
	 * @param mapping
	 * @param form
	 * @param req
	 * @param res
	 * @param conn
	 * @throws Exception
	 */
	public void doUpdateQuantity(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res, SqlSessionManager conn)throws Exception{
		log.info("PartialCollationAction.doUpdateQuantity start");

		/* Ajax反馈对象 */
		Map<String, Object> callbackResponse = new HashMap<String, Object>();
		List<MsgInfo> errors = new ArrayList<MsgInfo>();

		// 进行中的作业信息
		FactProductionFeatureForm factProductionFeatureForm = factProductionFeatureService.searchUnFinishProduction(req, conn);

		String factPfKey = factProductionFeatureForm.getFact_pf_key();
		String key = factProductionFeatureForm.getPartial_warehouse_key();

		// 入库单信息
		PartialWarehouseForm partialWarehouseForm = partialWarehouseService.getByKey(key, conn);
		String warehouseNo = partialWarehouseForm.getWarehouse_no();

		Pattern p = Pattern.compile("(\\w+).(\\w+)\\[(\\d+)\\]");

		List<PartialWarehouseDetailForm> list = new AutofillArrayList<PartialWarehouseDetailForm>(PartialWarehouseDetailForm.class);

		Map<String, String[]> parameters = req.getParameterMap();

		for (String parameterKey : parameters.keySet()) {
			Matcher m = p.matcher(parameterKey);
			if (m.find()) {
				String entity = m.group(1);
				if ("partial_warehouse_detail".equals(entity)) {
					String column = m.group(2);
					int icounts = Integer.parseInt(m.group(3));
					String[] value = parameters.get(parameterKey);

					if ("partial_id".equals(column)) {
						list.get(icounts).setPartial_id(value[0]);
					}else if ("seq".equals(column)) {
						list.get(icounts).setSeq(value[0]);
					}  else if ("collation_quantity".equals(column)) {
						list.get(icounts).setCollation_quantity(value[0]);
					} else if ("flg".equals(column)) {
						list.get(icounts).setFlg(value[0]);
					}

					list.get(icounts).setKey(key);
					list.get(icounts).setFact_pf_key(factPfKey);
				}
			}
		}

		//判断是否扫描了新零件
		for(PartialWarehouseDetailForm partialWarehouseDetailForm : list){
			String flg = partialWarehouseDetailForm.getFlg();

			if ("1".equals(flg)) {
				PartialWarehouseDnForm partialWarehouseDnForm =	partialWarehouseDnSerice.getPartialWarehouseDnByDnNo(warehouseNo + "E", conn);

				if(partialWarehouseDnForm == null){
					partialWarehouseDnForm = new PartialWarehouseDnForm();
					partialWarehouseDnForm.setKey(key);
					partialWarehouseDnForm.setSeq("0");
					partialWarehouseDnForm.setWarehouse_date(DateUtil.toString(Calendar.getInstance().getTime(), DateUtil.DATE_PATTERN));
					partialWarehouseDnForm.setDn_no(warehouseNo + "E");

					//新建零件入库DN编号
					partialWarehouseDnSerice.insert(partialWarehouseDnForm, conn);
				}
				break;
			}
		}

		for (PartialWarehouseDetailForm partialWarehouseDetailForm : list) {
			String flg = partialWarehouseDetailForm.getFlg();

			if ("1".equals(flg)) {// 零件在此单中不存在
				String collationQuantity = partialWarehouseDetailForm.getCollation_quantity();
				partialWarehouseDetailForm.setQuantity(collationQuantity);
				partialWarehouseDetailForm.setCollation_quantity(collationQuantity);
				partialWarehouseDetailService.insert(partialWarehouseDetailForm, conn);
			} else if ("0".equals(flg)) { //零件在此单中不存在,但是已经加入此单中
				String collationQuantity = partialWarehouseDetailForm.getCollation_quantity();
				partialWarehouseDetailForm.setQuantity(collationQuantity);
				partialWarehouseDetailForm.setCollation_quantity(collationQuantity);
				partialWarehouseDetailService.update(partialWarehouseDetailForm, conn);
			} else {
				partialWarehouseDetailService.update(partialWarehouseDetailForm, conn);
			}
		}


		/* 检查错误时报告错误信息 */
		callbackResponse.put("errors", errors);
		/* 返回Json格式响应信息 */
		returnJsonResponse(res, callbackResponse);

		log.info("PartialCollationAction.doUpdateQuantity end");
	}

}
