package com.osh.rvs.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;

import com.osh.rvs.bean.PartialWarehouseEntity;
import com.osh.rvs.mapper.PartialWarehouseMapper;
import com.osh.rvs.mapper.UserDefineCodesMapper;

import framework.huiqing.common.util.CodeListUtils;
import framework.huiqing.common.util.copy.DateUtil;

public class PartialWarehouseService {

	private final String NS_STANDARD_TIME = "6";
	private final String DEC_STANDARD_TIME = "9";

	/**
	 * 仓管人员工作当前进度
	 *
	 * @param listResponse
	 * @param conn
	 */
	public void searchProcess(Map<String, Object> listResponse, SqlSession conn) {
		PartialWarehouseMapper dao = conn.getMapper(PartialWarehouseMapper.class);

		getUserDefineCodes(listResponse, conn);

		// 收货搬运移动标准工时
		BigDecimal bdPartialReceptMoveCost = (BigDecimal)listResponse.get("bdPartialReceptMoveCost");


		// 00000000197:高雁梅,00000000198:叶昭杏
		String[] operatorIDs = { "00000000197", "00000000198" };

		// 当前进度
		List<PartialWarehouseEntity> list = new ArrayList<PartialWarehouseEntity>();

		// 今日成果
		List<PartialWarehouseEntity> resultList = new ArrayList<PartialWarehouseEntity>();
		List<PartialWarehouseEntity> percentList = new ArrayList<PartialWarehouseEntity>();

		List<Map<String, String>> ret = new ArrayList<Map<String, String>>();

		long pos = 480;
		for (String operatorID : operatorIDs) {
			List<Map<String, Object>> operatorFeatures = dao.getOperatorFeatures(operatorID);

			for (Map<String, Object> feature : operatorFeatures) {
				Map<String, String> retPf = new HashMap<String, String>();

				String productionType = "" + feature.get("production_type");
				String operatorId = "" + feature.get("operator_id");
				String operatorName = "" + feature.get("operator_name");

				retPf.put("production_type", productionType);
				retPf.put("operatorId", operatorId);
				retPf.put("operatorName", operatorName);

				Long actionTime = (Long) feature.get("action_time");
				Long finishTime = (Long) feature.get("finish_time");
				Long spareMinutes = (finishTime - actionTime);

				retPf.put("action_time", Long.toString(actionTime - pos + 5));
				retPf.put("finish_time", Long.toString(finishTime - pos + 5));
				retPf.put("spare_minutes", Long.toString(spareMinutes));

				ret.add(retPf);
			}

			// 查询正在进行中的作业
			PartialWarehouseEntity entity = dao.searchUnFinish(operatorID);

			if (entity == null) {
				entity = new PartialWarehouseEntity();
				entity.setOperator_id(operatorID);
				if (operatorID.equals("00000000197")) {
					entity.setOperator_name("高雁梅");
				} else if (operatorID.equals("00000000198")) {
					entity.setOperator_name("叶昭杏");
				}
				list.add(entity);
			} else {
				//现品作业KEY
				String factPfKey = entity.getFact_pf_key();

				// 总时间
				BigDecimal totalStandardTime = new BigDecimal("0");
				// 作业内容
				String productionType = String.valueOf(entity.getProduction_type());
				entity.setProduction_type_name(CodeListUtils.getValue("fact_production_type", productionType));

				if ("99".equals(productionType)) {
					// 作业经过时间
					Integer workTime = dao.currentSpendTime(factPfKey);

					if (workTime == null) workTime = 0;
					entity.setSpentMins(workTime);
				} else {
					entity.setIsNow(1);
					if ("10".equals(productionType) || "11".equals(productionType)) {// A：收货
						// 收货工时标准
						BigDecimal standardTime = dao.searchCurrentReceptStandardTime(factPfKey);

						if (standardTime != null) {
							totalStandardTime = totalStandardTime.add(standardTime);
						}

						standardTime = dao.searchCollectCaseStandardTime(entity);
						if (standardTime != null) {
							totalStandardTime = totalStandardTime.add(standardTime);
						}

						totalStandardTime = totalStandardTime.add(bdPartialReceptMoveCost);
					} else if ("50".equals(productionType)) {// E1：NS工程出库,
						totalStandardTime = totalStandardTime.add(new BigDecimal(NS_STANDARD_TIME));
					} else if ("51".equals(productionType)) {// E2：分解工程出库
						totalStandardTime = totalStandardTime.add(new BigDecimal(DEC_STANDARD_TIME));
					} else if("20".equals(productionType)){//B1：核对+上架
						totalStandardTime = dao.searchCurrentCollectAndOnShelfStandardTime(factPfKey);
					} else if("21".equals(productionType)){//B2：核对
						totalStandardTime = dao.searchCurrentCollectStandardTime(factPfKey);
					} else if("30".equals(productionType)){//C：分装
						totalStandardTime = dao.searchCurrentUnPackStandardTime(factPfKey);
					} else if("40".equals(productionType)){//D：上架
						totalStandardTime = dao.searchCurrentOnShelfStandardTime(factPfKey);
					}

					if(totalStandardTime == null) totalStandardTime = BigDecimal.ZERO;

					totalStandardTime = totalStandardTime.setScale(0, RoundingMode.UP);

					// 作业标准时间
					entity.setStandardTime(totalStandardTime.intValue());

					// 当前作业经过时间
					Integer workTime = dao.currentSpendTime(factPfKey);
					if (workTime == null) workTime = 0;

					entity.setSpentMins(workTime);
				}

				list.add(entity);
			}

			entity = countQuantity(operatorID, dao);
			resultList.add(entity);

			entity = setPercent(operatorID, bdPartialReceptMoveCost.toString(), dao);
			percentList.add(entity);
		}

		listResponse.put("productionFeatures", ret);

		listResponse.put("process", list);
		listResponse.put("resultList", resultList);
		listResponse.put("percentList", percentList);

		// 待处理单
		List<PartialWarehouseEntity> waitList = dao.waittingProcess();
		waitList.addAll(waittingOutLine(dao));

		listResponse.put("waitList", waitList);

	}


	private void getUserDefineCodes(Map<String, Object> listResponse,SqlSession conn){
		UserDefineCodesMapper userDefineCodesMapper = conn.getMapper(UserDefineCodesMapper.class);

		// 收货搬运移动标准工时
		String value = userDefineCodesMapper.getValue("PARTIAL_RECEPT_MOVE_COST");
		BigDecimal bdPartialReceptMoveCost = null;
		try {
			bdPartialReceptMoveCost = new BigDecimal(value);
		} catch (Exception e) {
			bdPartialReceptMoveCost = new BigDecimal(12);
		}

		listResponse.put("bdPartialReceptMoveCost", bdPartialReceptMoveCost);

		// 仓管人员能率警报标志上线
		value = userDefineCodesMapper.getValue("FACT_PROCESS_EF_HIGH_LEVER");
		BigDecimal efHighLever = null;
		try {
			efHighLever = new BigDecimal(value);
		} catch (Exception e) {
			efHighLever = new BigDecimal(120);
		}
		listResponse.put("efHighLever", efHighLever);

		// 仓管人员能率警报标志下线
		value = userDefineCodesMapper.getValue("FACT_PROCESS_EF_LOW_LEVER");
		BigDecimal efLowLever = null;
		try {
			efLowLever = new BigDecimal(value);
		} catch (Exception e) {
			efLowLever = new BigDecimal(80.5);
		}
		listResponse.put("efLowLever", efLowLever);

		// 仓管人员负荷率警报标志上线
		value = userDefineCodesMapper.getValue("FACT_PROCESS_STR_HIGH_LEVER");
		BigDecimal strHighLever = null;
		try {
			strHighLever = new BigDecimal(value);
		} catch (Exception e) {
			strHighLever = new BigDecimal(100);
		}
		listResponse.put("strHighLever", strHighLever);

		// 仓管人员负荷率警报标志下线
		value = userDefineCodesMapper.getValue("FACT_PROCESS_STR_LOW_LEVER");
		BigDecimal strLowLever = null;
		try {
			strLowLever = new BigDecimal(value);
		} catch (Exception e) {
			strLowLever = new BigDecimal(50);
		}
		listResponse.put("strLowLever", strLowLever);

	}

	public List<PartialWarehouseEntity> waittingOutLine(PartialWarehouseMapper dao) {
		// 待出库单
		List<PartialWarehouseEntity> list = dao.waittingOutLine();

		Map<String, String> map = new HashMap<String, String>();
		for (PartialWarehouseEntity entity : list) {
			String omr_notifi_no = entity.getOmr_notifi_no();

			String time = "";
			if (entity.getAction_time() != null) {
				time = DateUtil.toString(entity.getAction_time(), DateUtil.DATE_TIME_PATTERN);
			}

			if (!map.containsKey(omr_notifi_no)) {
				map.put(omr_notifi_no, entity.getProcess_code() + ":" + time);
			} else {
				String value = map.get(omr_notifi_no);
				value += "," + entity.getProcess_code() + ":" + time;
				map.put(omr_notifi_no, value);
			}
		}

		list = new ArrayList<PartialWarehouseEntity>();
		for (String key : map.keySet()) {
			PartialWarehouseEntity entity = new PartialWarehouseEntity();
			entity.setOmr_notifi_no(key);
			entity.setContent(map.get(key));
			list.add(entity);

		}

		return list;
	}

	/**
	 * 今日每种作业内容数量
	 *
	 * @param operatorID
	 * @param dao
	 * @return
	 */
	private PartialWarehouseEntity countQuantity(String operatorID, PartialWarehouseMapper dao) {
		PartialWarehouseEntity entity = new PartialWarehouseEntity();

		// 操作者
		entity.setOperator_id(operatorID);

		PartialWarehouseEntity connd = new PartialWarehouseEntity();
		connd.setOperator_id(operatorID);

		// 当日收货数量
		Integer recept = dao.searchCurrentReceptQuantity(operatorID);
		entity.setRecept(recept);

		// B1、核对+上架数量
		connd.setProduction_type(20);
		Integer collationShelf = dao.searchCurrentCollationAndOnShelfQuantity(operatorID);
		entity.setCollation_shelf(collationShelf);

		// B2、核对数量
		connd.setProduction_type(21);
		Integer collation = dao.searchCurrentCollationQuantity(operatorID);
		entity.setCollation(collation);

		// C、分装数量
		connd.setProduction_type(30);
		Integer unpack = dao.searchQuantity(connd);
		entity.setUnpack(unpack);

		// D、上架数量
		connd.setProduction_type(40);
		Integer onShelf = dao.searchQuantity(connd);
		entity.setOn_shelf(onShelf);

		// E1、NS 出库数量
		connd.setProduction_type(50);
		Integer nsOutline = onShelf = dao.countOutLineQuantity(connd);
		entity.setNs_outline(nsOutline);

		// E2、分解出库数量
		connd.setProduction_type(51);
		Integer decOutline = onShelf = dao.countOutLineQuantity(connd);
		entity.setDec_outline(decOutline);

		// O、其它（分钟）
		connd.setProduction_type(99);
		connd.setIsNow(2);
		Integer spentMins = dao.searchSpentMins(connd);
		if (spentMins == null) 	spentMins = 0;
		entity.setSpentMins(spentMins);

		return entity;
	}

	private PartialWarehouseEntity setPercent(String operatorID, String move, PartialWarehouseMapper dao) {
		PartialWarehouseEntity entity = new PartialWarehouseEntity();
		entity.setOperator_id(operatorID);

		PartialWarehouseEntity connd = new PartialWarehouseEntity();
		// 操作者
		connd.setOperator_id(operatorID);
		connd.setIsNow(2);

		// 总标准时间(分钟)
		BigDecimal totalStandardTime = new BigDecimal(0);

		BigDecimal totalActualTime = new BigDecimal(0);

		// A、收货标准工时(分钟)
		BigDecimal standardTime = dao.searchTodayReceptStandardTime(operatorID);
		if(standardTime == null) standardTime = BigDecimal.ZERO;

		// 拆盒标准工时(分钟)
		entity.setIsNow(2);
		BigDecimal collectCaseStandardTime = dao.searchCollectCaseStandardTime(entity);
		if(collectCaseStandardTime == null) collectCaseStandardTime = BigDecimal.ZERO;

		BigDecimal receptStandardTime = standardTime.add(collectCaseStandardTime);

		//收货次数
		Integer countRecept = dao.count(operatorID, "10");
		if(countRecept == null) countRecept  = 0;
		standardTime = new BigDecimal(move);
		standardTime = standardTime.multiply(new BigDecimal(countRecept));

		receptStandardTime = receptStandardTime.add(standardTime);

		totalStandardTime = totalStandardTime.add(receptStandardTime);

		// A、收货实际用时
		connd.setProduction_type(10);
		Integer actualTime = dao.searchSpentMins(connd);
		if (actualTime != null) {
			totalActualTime = totalActualTime.add(new BigDecimal(actualTime));
			entity.setAccept_percent(calculatePercent(receptStandardTime, new BigDecimal(actualTime)));
		}

		// B1、核对+上架标准工时(分钟)
		connd.setProduction_type(20);
		standardTime = dao.searchTodayCollectAndOnShelfStandardTime(operatorID);
		if (standardTime != null) {
			totalStandardTime = totalStandardTime.add(standardTime);
			actualTime = dao.searchSpentMins(connd);
			if (actualTime != null) {
				totalActualTime = totalActualTime.add(new BigDecimal(actualTime));
				entity.setCollation_shelf_percent(calculatePercent(standardTime, new BigDecimal(actualTime)));
			}
		}

		// B2、核对标准工时(分钟)
		connd.setProduction_type(21);
		standardTime = dao.searchTodayCollectStandardTime(operatorID);
		if (standardTime != null) {
			totalStandardTime = totalStandardTime.add(standardTime);
			actualTime = dao.searchSpentMins(connd);
			if (actualTime != null) {
				totalActualTime = totalActualTime.add(new BigDecimal(actualTime));
				entity.setCollation_percent(calculatePercent(standardTime, new BigDecimal(actualTime)));
			}
		}

		// C、分装标准工时(分钟)
		connd.setProduction_type(30);
		standardTime = dao.searchTodayUnPackStandardTime(operatorID);
		if (standardTime != null) {
			totalStandardTime = totalStandardTime.add(standardTime);
			actualTime = dao.searchSpentMins(connd);
			if (actualTime != null) {
				totalActualTime = totalActualTime.add(new BigDecimal(actualTime));
				entity.setUnpack_percent(calculatePercent(standardTime, new BigDecimal(actualTime)));
			}
		}

		// D、上架标准工时(分钟)
		connd.setProduction_type(40);
		standardTime = dao.searchTodayOnShelfStandardTime(operatorID);
		if (standardTime != null) {
			totalStandardTime = totalStandardTime.add(standardTime);
			actualTime = dao.searchSpentMins(connd);
			if (actualTime != null) {
				totalActualTime = totalActualTime.add(new BigDecimal(actualTime));
				entity.setOn_shelf_percent(calculatePercent(standardTime, new BigDecimal(actualTime)));
			}
		}

		// E1、NS 出库标准工时(分钟)
		connd.setProduction_type(50);
		actualTime = dao.searchSpentMins(connd);
		Integer nsCount = dao.count(operatorID, "50");
		if(nsCount == null) nsCount = 0;
		if (actualTime != null) {
			standardTime = new BigDecimal(NS_STANDARD_TIME);
			standardTime = standardTime.multiply(new BigDecimal(nsCount));

			totalStandardTime = totalStandardTime.add(standardTime);

			totalActualTime = totalActualTime.add(new BigDecimal(actualTime));
			entity.setNs_outline_percent(calculatePercent(standardTime, new BigDecimal(actualTime)));
		}

		// E2、分解出库标准工时(分钟)
		connd.setProduction_type(51);
		actualTime = dao.searchSpentMins(connd);
		Integer decCount = dao.count(operatorID, "51");
		if(decCount == null) decCount = 0;
		if (actualTime != null) {
			standardTime = new BigDecimal(DEC_STANDARD_TIME);
			standardTime = standardTime.multiply(new BigDecimal(decCount));
			totalStandardTime = totalStandardTime.add(standardTime);

			totalActualTime = totalActualTime.add(new BigDecimal(actualTime));
			entity.setDec_outline_percent(calculatePercent(standardTime, new BigDecimal(actualTime)));
		}

		// O、其它
		connd.setProduction_type(99);
		actualTime = dao.searchSpentMins(connd);
		if (actualTime != null) {
			totalActualTime = totalActualTime.add(new BigDecimal(actualTime));
			totalStandardTime = totalStandardTime.add(new BigDecimal(actualTime));
		}

		if (totalActualTime != null && totalStandardTime != null && totalActualTime.doubleValue() != 0) {
			entity.setTotal_percent(totalStandardTime.divide(totalActualTime, 3, RoundingMode.HALF_UP).multiply(new BigDecimal(100)));
		}

		return entity;

	}

	/**
	 * 计算百分百
	 *
	 * @param standardTime 标准工时
	 * @param spentMins 实际工时
	 * @return
	 */
	private Integer calculatePercent(BigDecimal standardTime, BigDecimal spentMins) {
		BigDecimal result = standardTime.divide(spentMins, 2, RoundingMode.UP);

		// 乘以100
		result = result.multiply(new BigDecimal(100));

		return result.intValue();
	}

}
