package com.osh.rvs.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionManager;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;

import com.osh.rvs.bean.LoginData;
import com.osh.rvs.bean.data.AlarmMesssageEntity;
import com.osh.rvs.bean.data.AlarmMesssageSendationEntity;
import com.osh.rvs.bean.data.MaterialEntity;
import com.osh.rvs.bean.data.ProductionFeatureEntity;
import com.osh.rvs.bean.inline.ForSolutionAreaEntity;
import com.osh.rvs.bean.inline.PauseFeatureEntity;
import com.osh.rvs.bean.master.ModelEntity;
import com.osh.rvs.bean.master.OperatorEntity;
import com.osh.rvs.bean.master.OperatorNamedEntity;
import com.osh.rvs.bean.master.PositionEntity;
import com.osh.rvs.common.FseBridgeUtil;
import com.osh.rvs.common.PathConsts;
import com.osh.rvs.common.PcsUtils;
import com.osh.rvs.common.RvsConsts;
import com.osh.rvs.common.RvsUtils;
import com.osh.rvs.form.data.AlarmMesssageForm;
import com.osh.rvs.mapper.CommonMapper;
import com.osh.rvs.mapper.data.AlarmMesssageMapper;
import com.osh.rvs.mapper.data.MaterialMapper;
import com.osh.rvs.mapper.inline.ForSolutionAreaMapper;
import com.osh.rvs.mapper.inline.LeaderPcsInputMapper;
import com.osh.rvs.mapper.inline.PauseFeatureMapper;
import com.osh.rvs.mapper.inline.ProductionFeatureMapper;
import com.osh.rvs.mapper.master.ModelMapper;
import com.osh.rvs.mapper.master.OperatorMapper;
import com.osh.rvs.mapper.master.PositionMapper;
import com.osh.rvs.service.inline.ForSolutionAreaService;

import framework.huiqing.bean.message.MsgInfo;
import framework.huiqing.common.util.AutofillArrayList;
import framework.huiqing.common.util.CodeListUtils;
import framework.huiqing.common.util.CommonStringUtil;
import framework.huiqing.common.util.copy.BeanUtil;
import framework.huiqing.common.util.copy.CopyOptions;
import framework.huiqing.common.util.copy.DateUtil;
import framework.huiqing.common.util.message.ApplicationMessage;

public class AlarmMesssageService {

	private Logger logger = Logger.getLogger(getClass());

	/**
	 * 建立警报信息
	 * @param conn 数据库连接
	 * @param triggerList 
	 * @throws Exception 
	 */
	public String createAlarmMessage(AlarmMesssageEntity entity, SqlSessionManager conn, boolean sendToScheduler, List<String> triggerList) throws Exception {
		AlarmMesssageMapper dao = conn.getMapper(AlarmMesssageMapper.class);
		// 建立警报信息记录
		dao.createAlarmMessage(entity);

		// 取到信息主键
		CommonMapper cDao = conn.getMapper(CommonMapper.class);
		String amId = cDao.getLastInsertID();

		if (amId == null){
			logger.error("getLastInsertID失败！！");
			List<AlarmMesssageEntity> l = dao.searchAlarmMessages(entity);
			if (l != null && l.size() > 0) {
				amId = l.get(0).getAlarm_messsage_id();
			}
			logger.error("重新试图取得的ID是：" + amId);
		}
		// 查找发送目标
		// 线长
		OperatorMapper oDao = conn.getMapper(OperatorMapper.class);
		OperatorEntity condBean = new OperatorEntity();
		condBean.setLine_id(entity.getLine_id());
		condBean.setSection_id(entity.getSection_id());
		condBean.setRole_id("00000000005"); //TODO
		List<OperatorNamedEntity> leaders = oDao.searchOperator(condBean);

		List<String> fingerOperators = new ArrayList<String>();

		for(OperatorNamedEntity leader : leaders) {
			AlarmMesssageSendationEntity sendation = new AlarmMesssageSendationEntity();
			sendation.setAlarm_messsage_id(amId);
			sendation.setSendation_id(leader.getOperator_id());
			dao.createAlarmMessageSendation(sendation);
			fingerOperators.add(leader.getOperator_id());
		}

		if (sendToScheduler) {
			// 计划管理员
			condBean = new OperatorEntity();
			condBean.setRole_id(RvsConsts.ROLE_SCHEDULER);
			List<OperatorNamedEntity> schedulers = oDao.searchOperator(condBean);
			for(OperatorNamedEntity scheduler : schedulers) {
				AlarmMesssageSendationEntity sendation = new AlarmMesssageSendationEntity();
				sendation.setAlarm_messsage_id(amId);
				sendation.setSendation_id(scheduler.getOperator_id());
				dao.createAlarmMessageSendation(sendation);
				fingerOperators.add(scheduler.getOperator_id());
			}
		}

		if (fingerOperators.size() > 0 && triggerList != null) {
			String noticeString = "http://localhost:8080/rvspush/trigger/postMessage/";
			if (fingerOperators.size() == 1) {
				noticeString += fingerOperators.get(0) + "/"  + fingerOperators.get(0) + "/" + fingerOperators.get(0) + "/"; // 重要的事情
			} else if (fingerOperators.size() == 2) {
				noticeString += fingerOperators.get(0) + "/"  + fingerOperators.get(0) + "/" + fingerOperators.get(1) + "/"; // 重要的事情
			} else {
				for (String fingerOperator : fingerOperators) {
					noticeString += fingerOperator + "/";
				}
			}
			// 通知
			triggerList.add(noticeString);
		}

//		HttpAsyncClient httpclient = new DefaultHttpAsyncClient();
//		httpclient.start();
//
//		try { 
//			HttpGet request = new HttpGet("http://localhost:8080/rvsTouch/beep/" + entity.getSection_id() +
//					"/" + entity.getLine_id());
//			logger.info("finger:"+request.getURI());
//	        httpclient.execute(request, null);
//        } catch (Exception e) {
//		} finally {
//			Thread.sleep(100);
//			httpclient.shutdown();
//		}

		return amId;
	}

	/**
	 * 建立警报信息-从工作信息得到
	 * @param conn 数据库连接
	 */
	public AlarmMesssageEntity createBreakAlarmMessage(ProductionFeatureEntity workingPf, Integer reason) {
		AlarmMesssageEntity amEntity = new AlarmMesssageEntity();
		amEntity.setLevel(RvsConsts.WARNING_LEVEL_NORMAL);
		amEntity.setLine_id(workingPf.getLine_id());
		amEntity.setMaterial_id(workingPf.getMaterial_id());
		amEntity.setOperator_id(workingPf.getOperator_id());
		amEntity.setPosition_id(workingPf.getPosition_id());
		amEntity.setSection_id(workingPf.getSection_id());
		amEntity.setReason(reason);
		return amEntity;
	}
	public AlarmMesssageEntity createBreakAlarmMessage(ProductionFeatureEntity workingPf) {
		return createBreakAlarmMessage(workingPf, RvsConsts.WARNING_REASON_BREAK);
	}

	/**
	 * 建立警报信息-终检不通过
	 * @param conn 数据库连接
	 * @throws Exception 
	 */
	public void createDefectsAlarmMessage(ProductionFeatureEntity workingPf, SqlSessionManager conn) throws Exception {
		String material_id = workingPf.getMaterial_id();

		AlarmMesssageEntity amEntity = new AlarmMesssageEntity();
		amEntity.setLevel(RvsConsts.WARNING_LEVEL_SCHEDULE);
		amEntity.setLine_id("00000000015");
		amEntity.setMaterial_id(material_id);
		amEntity.setOperator_id(null);
		amEntity.setPosition_id(workingPf.getPosition_id());
		amEntity.setSection_id(null);
		amEntity.setReason(RvsConsts.WARNING_REASON_QAFORBID);

		AlarmMesssageMapper dao = conn.getMapper(AlarmMesssageMapper.class);
		// 建立警报信息记录
		dao.createAlarmMessage(amEntity);

		// 取到信息主键
		CommonMapper cDao = conn.getMapper(CommonMapper.class);
		String amId = cDao.getLastInsertID();

		// 推送信息
		MaterialMapper mDao = conn.getMapper(MaterialMapper.class);
		MaterialEntity mBean = mDao.getMaterialEntityByKey(material_id);
		String section_id = mBean.getSection_id(); 
		ModelMapper mdlMapper = conn.getMapper(ModelMapper.class);
		ModelEntity mdBean = mdlMapper.getModelByID(mBean.getModel_id());

		// 查找发送目标
		// 线长
		OperatorMapper oDao = conn.getMapper(OperatorMapper.class);
		OperatorEntity condBean = new OperatorEntity();
		if ("07".equals(mdBean.getKind()) || "7".equals(mdBean.getKind())) {
			condBean.setLine_id("00000000070");
		} else if ("06".equals(mdBean.getKind()) || "6".equals(mdBean.getKind())) {
			condBean.setLine_id("00000000050");
		} else if ("03".equals(mdBean.getKind()) || "3".equals(mdBean.getKind()) ||
				"04".equals(mdBean.getKind()) || "4".equals(mdBean.getKind())) {
			condBean.setLine_id("00000000061");
		} else {
			condBean.setLine_id("00000000014");
		}
		condBean.setSection_id(section_id);
		condBean.setRole_id(RvsConsts.ROLE_LINELEADER);
		List<OperatorNamedEntity> leaders = oDao.searchOperator(condBean);

		for(OperatorNamedEntity leader : leaders) {
			AlarmMesssageSendationEntity sendation = new AlarmMesssageSendationEntity();
			sendation.setAlarm_messsage_id(amId);
			sendation.setSendation_id(leader.getOperator_id());
			dao.createAlarmMessageSendation(sendation);
		}

		// 经理
		condBean = new OperatorEntity();
		condBean.setRole_id(RvsConsts.ROLE_MANAGER);
		condBean.setSection_id(section_id);
		List<OperatorNamedEntity> managers = oDao.searchOperator(condBean);

		for(OperatorNamedEntity manager : managers) {
			AlarmMesssageSendationEntity sendation = new AlarmMesssageSendationEntity();
			sendation.setAlarm_messsage_id(amId);
			sendation.setSendation_id(manager.getOperator_id());
			dao.createAlarmMessageSendation(sendation);
		}

		// 计划管理员
		condBean = new OperatorEntity();
		condBean.setRole_id(RvsConsts.ROLE_SCHEDULER);
		List<OperatorNamedEntity> schedulers = oDao.searchOperator(condBean);

		for(OperatorNamedEntity scheduler : schedulers) {
			AlarmMesssageSendationEntity sendation = new AlarmMesssageSendationEntity();
			sendation.setAlarm_messsage_id(amId);
			sendation.setSendation_id(scheduler.getOperator_id());
			dao.createAlarmMessageSendation(sendation);
		}
	}

	/**
	 * 建立警报信息-独立工位
	 * @param conn 数据库连接
	 */
	public AlarmMesssageEntity createSoloBreakAlarmMessage(ProductionFeatureEntity workingPf) {
		AlarmMesssageEntity amEntity = new AlarmMesssageEntity();
		amEntity.setLevel(RvsConsts.WARNING_LEVEL_NORMAL);
		amEntity.setLine_id(workingPf.getLine_id());
		amEntity.setMaterial_id(workingPf.getMaterial_id());
		amEntity.setOperator_id(workingPf.getOperator_id());
		amEntity.setPosition_id(workingPf.getPosition_id());
		amEntity.setSection_id(workingPf.getSection_id());
		amEntity.setReason(RvsConsts.WARNING_REASON_BREAK_SOLO);
		return amEntity;
	}

	/**
	 * 取得警报信息-不良中断
	 * @param conn 数据库连接
	 */
	public AlarmMesssageEntity getBreakAlarmMessage(String material_id, String position_id, SqlSession conn) {
		AlarmMesssageMapper dao = conn.getMapper(AlarmMesssageMapper.class);

		AlarmMesssageEntity amEntity = dao.getBreakAlarmMessage(material_id, position_id);

		return amEntity;
	}

	/**
	 * 取消警报信息-中断再开
	 * @param conn 数据库连接
	 */
	public void undoAlarmMessage(SqlSessionManager conn) {
	}

	/**
	 * 完成警报处理
	 * @param conn 数据库连接
	 */
	public void resolveAlarmMessage(SqlSessionManager conn) {
	}

	/**
	 * 收信者的警报数量
	 * @param conn 数据库连接
	 */
	public int getMessageCountsByOperator(SqlSession conn, String operator_id) {
		AlarmMesssageMapper dao = conn.getMapper(AlarmMesssageMapper.class);
		return dao.countAlarmMessageOfSendation(operator_id);
	}

	/**
	 * 得到针对每个用户的未解决信息
	 * @param conn
	 * @param operator_id
	 * @return
	 */
	public List<AlarmMesssageEntity> getMessageByOperator(SqlSession conn, String operator_id) {
		AlarmMesssageMapper dao = conn.getMapper(AlarmMesssageMapper.class);
		return dao.getAlarmMessageBySendation(operator_id);
	}

	public List<AlarmMesssageEntity> getUnredAlarmMessagesByMaterial(String material_id, SqlSession conn) {
		AlarmMesssageMapper dao = conn.getMapper(AlarmMesssageMapper.class);

		List<AlarmMesssageEntity> amEntities = dao.getBreakAlarmMessages(material_id, null);

		return amEntities;
	}

	public List<AlarmMesssageForm> search(ActionForm form, SqlSession conn, List<MsgInfo> errors) {
		List<AlarmMesssageForm> ret = new ArrayList<AlarmMesssageForm>();
		AlarmMesssageEntity condBean = new AlarmMesssageEntity();
		BeanUtil.copyToBean(form, condBean, CopyOptions.COPYOPTIONS_NOEMPTY);
		AlarmMesssageMapper dao = conn.getMapper(AlarmMesssageMapper.class);

		try {
			List<AlarmMesssageEntity> amEntities = dao.searchAlarmMessages(condBean);

			BeanUtil.copyToFormList(amEntities, ret, CopyOptions.COPYOPTIONS_NOEMPTY, AlarmMesssageForm.class);
		} catch (Exception e) {
			logger.error(e.getMessage());
			MsgInfo arg0 = new MsgInfo();
			arg0.setErrmsg(ApplicationMessage.WARNING_MESSAGES.getMessage("info.search.timeout"));
			errors.add(arg0);
		}

		return ret;
	}

	/**
	 * 检查在工程内是否有中断
	 * @param material_id
	 * @param line_id
	 * @param conn
	 * @return
	 */
	public String searchInlinePartialBlock(String material_id, String line_id, SqlSession conn) {
		AlarmMesssageMapper dao = conn.getMapper(AlarmMesssageMapper.class);

		List<AlarmMesssageEntity> amEntities = dao.searchAlarmMessagesByMaterialInline(material_id, line_id);

		if (amEntities.size() > 0) {
			return amEntities.get(0).getAlarm_messsage_id();
		}

		return null;
	}

	/**
	 * 取得中断信息
	 * @param alarm_messsage_id
	 * @param conn
	 * @return
	 */
	public AlarmMesssageForm getWarning(String alarm_messsage_id, SqlSession conn) {
		// 取得对应工位的中断信息
		AlarmMesssageMapper dao = conn.getMapper(AlarmMesssageMapper.class);
		AlarmMesssageEntity entity = dao.getBreakAlarmMessageByKey(alarm_messsage_id);
		AlarmMesssageForm form = new AlarmMesssageForm();
		CopyOptions co = new CopyOptions();

		co.dateConverter("MM-dd HH:mm", "occur_time");
		co.include("material_id", "alarm_messsage_id", "material_id", "occur_time", "sorc_no", "model_name", "serial_no",
				"line_name", "position_id", "process_code", "position_name", "operator_name", "reason", "level", "section_name");
		BeanUtil.copyToForm(entity, form, co);

		Integer amReason = entity.getReason();

		if (RvsConsts.WARNING_REASON_BREAK.equals(amReason)) {
			// 取得原因
			PauseFeatureEntity pauseEntity = dao.getBreakOperatorMessageByID(alarm_messsage_id);
			if (pauseEntity == null) {
				pauseEntity = dao.getBreakOperatorMessage(entity.getOperator_id(), entity.getMaterial_id(),entity.getPosition_id());
			}

			if (pauseEntity != null) {
				// 取得暂停信息里的记录
				Integer iReason = pauseEntity.getReason();
				// 不良理由
				String sReason = null;
				if (iReason != null && iReason < 10) {
					sReason = CodeListUtils.getValue("break_reason", "0" + iReason);
				} else {
					sReason = PathConsts.POSITION_SETTINGS.getProperty("break."+ pauseEntity.getProcess_code() +"." + iReason);
				}

				// 备注信息
				String sComments = pauseEntity.getComments();
				if (CommonStringUtil.isEmpty(sComments)) {
					form.setComment(sReason);
				} else {
					form.setComment(sReason + " : " + sComments);
				}
			} else {
				form.setComment("(不明)");
			}
			
		} else if (RvsConsts.WARNING_REASON_INLINE_LATE.equals(amReason)) {
			form.setComment("请确认是何原因还未投线。"); // TODO 请确认XXX(同意日期mom-dad)是何原因还未投线。
		} else if (RvsConsts.WARNING_REASON_QAFORBID.equals(amReason)) {
			form.setComment("由于品保终检不合格被退回，请知晓并及时处理！");
		} else if (RvsConsts.WARNING_REASON_PARTIAL_ON_POISTION.equals(amReason)) {
			form.setComment(entity.getSection_name() + "的" + entity.getProcess_code() + "工位零件签收清点发生异常。请前去确认！");
		} else if (RvsConsts.WARNING_REASON_INFECT_ERROR.equals(amReason)) {
			form.setComment(entity.getSection_name() + "的" + entity.getProcess_code() + "工位发生点检错误。请前去确认！");
		} else if (RvsConsts.WARNING_REASON_WAITING_OVERFLOW.equals(amReason)) {
			// 取得等待区上线数
			String overflow = "0";
			String position_id = entity.getPosition_id();
			if (position_id != null) {
				overflow = RvsUtils.getWaitingflow(entity.getSection_id(), null, entity.getProcess_code());
			}
			form.setComment(entity.getSection_name() + "的" + entity.getProcess_code() + "工位的仕挂数已经超过" + overflow +
					"，请知晓并及时处理！");
		} else if (RvsConsts.WARNING_REASON_POSITION_OVERTIME.equals(amReason)) {
			form.setComment("维修品在" + entity.getProcess_code() + "工位的实际作业时间已超出标准。请予以确认！");
		}

		form.setLevel(CodeListUtils.getValue("alarm_level", form.getLevel()));
		form.setReason(CodeListUtils.getValue("alarm_reason", form.getReason()));

		List<AlarmMesssageSendationEntity> listSendation = dao.getBreakAlarmMessageSendation(alarm_messsage_id);
		List<Map<String, String>> sendationList = new ArrayList<Map<String, String>>();
		for (AlarmMesssageSendationEntity sendation : listSendation) {
			Map<String, String> sendationMap = new HashMap<String, String>();
			sendationMap.put("sendation_name", sendation.getSendation_name());
			String resolve_time = DateUtil.toString(sendation.getResolve_time(), "MM-dd HH:mm");
			sendationMap.put("resolve_time", (resolve_time == null ? "(未处理)" : resolve_time));
			sendationMap.put("comment", (sendation.getComment() == null ? "" : sendation.getComment()));
			sendationList.add(sendationMap);
		}
		form.setSendations(sendationList);

		return form;
	}

	/**
	 * 完成中断
	 * @param req
	 * @param conn
	 * @throws Exception
	 */
	public void closebreak(HttpServletRequest req, SqlSession conn) throws Exception {
		// 取得用户信息
		HttpSession session = req.getSession();
		LoginData user = (LoginData) session.getAttribute(RvsConsts.SESSION_USER);

		String alarm_messsage_id =req.getParameter("alarm_messsage_id");
		String material_id = req.getParameter("material_id");
		String position_id = req.getParameter("position_id");

		if (CommonStringUtil.isEmpty(material_id) || CommonStringUtil.isEmpty(position_id)) {
			// 
		}

		// 完成中断
		PauseFeatureMapper pfdao = conn.getMapper(PauseFeatureMapper.class);
		pfdao.stopBreakFeature(material_id, user.getSection_id(), position_id, user.getOperator_id());

		// 作业状态变成等待（表示为中断等待再开）
		ProductionFeatureMapper ppDao = conn.getMapper(ProductionFeatureMapper.class);
		ppDao.breakOverOperateResult(material_id, position_id);

		// 处理人处理信息
		AlarmMesssageSendationEntity sendation = new AlarmMesssageSendationEntity();
		sendation.setAlarm_messsage_id(alarm_messsage_id);
		sendation.setComment(req.getParameter("comment"));
		sendation.setRed_flg(1);
		sendation.setSendation_id(user.getOperator_id());
		sendation.setResolve_time(new Date());

		AlarmMesssageMapper amDao = conn.getMapper(AlarmMesssageMapper.class);
		int me = amDao.countAlarmMessageSendation(sendation);
		if (me <= 0) {
			// 没有发给处理者的信息时（代理线长），新建一条
			amDao.createAlarmMessageSendation(sendation);
		} else {
			amDao.updateAlarmMessageSendation(sendation);
		}
	}

	/**
	 * 中断注释
	 * @param req
	 * @param conn
	 * @throws Exception
	 */
	public void comment(HttpServletRequest req, LoginData user, SqlSessionManager conn) throws Exception {
		String alarm_messsage_id =req.getParameter("alarm_messsage_id");

		// 处理人处理信息
		AlarmMesssageSendationEntity sendation = new AlarmMesssageSendationEntity();
		sendation.setAlarm_messsage_id(alarm_messsage_id);
		sendation.setComment(req.getParameter("comment"));
		sendation.setRed_flg(0);
		sendation.setSendation_id(user.getOperator_id());
		sendation.setResolve_time(new Date());

		AlarmMesssageMapper amDao = conn.getMapper(AlarmMesssageMapper.class);
		amDao.updateAlarmMessageSendation(sendation);
	}

	public void reworkbreak(HttpServletRequest req, SqlSessionManager conn, List<String> reworkPositions, List<String> triggerList) throws Exception {
		// 取得用户信息
		HttpSession session = req.getSession();
		LoginData user = (LoginData) session.getAttribute(RvsConsts.SESSION_USER);

		String material_id = req.getParameter("material_id");
		String position_id = req.getParameter("position_id");
		List<String> break_position_ids = new ArrayList<String>();

		PauseFeatureMapper pfdao = conn.getMapper(PauseFeatureMapper.class);

		if (position_id != null) {
			break_position_ids.add(position_id);
		} else {
			PauseFeatureEntity entity = new PauseFeatureEntity();
			entity.setMaterial_id(material_id);
			// 跨工程返工时结束所有中断
			List<String> allPositionBreaked = pfdao.getAllPositionBreaked(entity);
			break_position_ids.addAll(allPositionBreaked);
		}

		ProductionFeatureMapper ppDao = conn.getMapper(ProductionFeatureMapper.class);
		// 本工位作业状态等待的记录中取得Rework
		int rework = 0;
		rework = ppDao.getReworkCount(material_id);

		PositionMapper pDao = conn.getMapper(PositionMapper.class);
		// 完成中断
		for (String break_position_id : break_position_ids) {
			pfdao.stopBreakFeature(material_id, user.getSection_id(), break_position_id, user.getOperator_id());

			ProductionFeatureEntity pfBean = new ProductionFeatureEntity();
			pfBean.setMaterial_id(material_id);
			pfBean.setPosition_id(break_position_id);
			PositionEntity pBean = pDao.getPositionByID(break_position_id);

			pfBean.setPcs_inputs("{\"EX" + pBean.getProcess_code() + "00\" : \"1\"}");
			// 本工位作业状态等待的记录删除
			ppDao.removeBreakWaiting(pfBean);
		}

		// 标记返工
		MaterialProcessService mpService = new MaterialProcessService();
		mpService.assignReworkTrigger(material_id, position_id, reworkPositions, conn);

		// 指定工位的完成状态记录变为不通过
		for (String reworkPosition_id : reworkPositions) {
			ppDao.reworkOperateResult(material_id, reworkPosition_id);
		}

		// 从开始重新指派
		ProductionFeatureService pfService = new ProductionFeatureService();
		pfService.reprocess(material_id, rework + 1, ppDao, triggerList, conn); // Rework 加 1

		// 处理人处理信息
		String message_id = req.getParameter("alarm_messsage_id");
		if (message_id != null) {
			AlarmMesssageSendationEntity sendation = new AlarmMesssageSendationEntity();
			sendation.setAlarm_messsage_id(message_id);
			sendation.setComment(req.getParameter("comment"));
			sendation.setRed_flg(1);
			sendation.setSendation_id(user.getOperator_id());
			sendation.setResolve_time(new Date());

			AlarmMesssageMapper amDao = conn.getMapper(AlarmMesssageMapper.class);
			int me = amDao.countAlarmMessageSendation(sendation);
			if (me <= 0) {
				// 没有发给处理者的信息时（代理线长），新建一条
				amDao.createAlarmMessageSendation(sendation);
			} else {
				amDao.updateAlarmMessageSendation(sendation);
			}
		}
	}

	/**
	 * 工序再指派
	 * @param req
	 * @param conn
	 * @throws Exception 
	 */
	public void rework(HttpServletRequest req, List<String> triggerList, SqlSessionManager conn) throws Exception {
		Map<String, String[]> parameterMap = req.getParameterMap();
		List<String> reworkPositions = new AutofillArrayList<String>(String.class);
		Pattern p = Pattern.compile("(\\w+).(\\w+)\\[(\\d+)\\]");

		// 整理提交数据
		for (String parameterKey : parameterMap.keySet()) {
			Matcher m = p.matcher(parameterKey);
			if (m.find()) {
				String entity = m.group(1);
				if ("rework".equals(entity)) {
					String column = m.group(2);
					int icounts = Integer.parseInt(m.group(3));

					// TODO 全
					if ("positions".equals(column)) {
						reworkPositions.set(icounts, parameterMap.get(parameterKey)[0]);
					}
				}
			}
		}
		LoginData user = (LoginData) req.getSession().getAttribute(RvsConsts.SESSION_USER);

		// Add in Rvs2 Start
		String material_id = req.getParameter("material_id");
		String pat_id = req.getParameter("pat_id");
		String pcs_signed = req.getParameter("pcs_signed");
		String append_parts = req.getParameter("append_parts");
//		String breakPositionId = req.getParameter("position_id");

		MaterialEntity entity = null;
		if (pat_id != null) {
			MaterialMapper mMapper = conn.getMapper(MaterialMapper.class);
			entity = mMapper.getMaterialNamedEntityByKey(material_id);
			mMapper.updateMaterialPat(material_id, pat_id);

			// FSE 数据同步
			try{
				FseBridgeUtil.toUpdateMaterial(material_id, "rework");
			} catch (Exception e) {
				e.printStackTrace();
			}

			ProductionFeatureService featureService = new ProductionFeatureService();

			MaterialProcessService mpService = new MaterialProcessService();
			List<String> oldHasLines = mpService.loadMaterialProcessLineIds(material_id, conn); // 取得已存在工程
			if (oldHasLines != null && oldHasLines.size() > 0) {
				ProcessAssignService pas = new ProcessAssignService();
				List<String> newHasLines = pas.checkPatHasLine(pat_id, "" + entity.getLevel(), conn);
				mpService.resignMaterialProcess(material_id, oldHasLines, newHasLines, conn);
			}

			// 删除目前的等待作业
			featureService.removeWorking(material_id, null, conn);
//		} else if (reworkPositions.size() > 0) {
//			// 删除目前的等待作业
//			ProductionFeatureService featureService = new ProductionFeatureService();
//			featureService.removeWorking(material_id, breakPositionId, conn);
		}

		// 需要在工程检查票建立错误处理记录时
		if ("true".equals(pcs_signed)) {
			if (entity == null) {
				MaterialMapper mMapper = conn.getMapper(MaterialMapper.class);
				entity = mMapper.getMaterialNamedEntityByKey(material_id);
			}
			if (entity != null) {
				createResolveLeaderInput(req, user, entity.getKind(), conn);
			}
		}
		// Add in Rvs2 End
		ForSolutionAreaService fsoServer = new ForSolutionAreaService();
		ForSolutionAreaMapper fsaMapper = conn.getMapper(ForSolutionAreaMapper.class);

		// 处理人处理信息
		String message_id = req.getParameter("alarm_messsage_id");
		String for_solution_area_key = fsaMapper.findByAlarmMesssage(message_id);

		if (for_solution_area_key != null) {

			ForSolutionAreaEntity fsaEntity = fsaMapper.getByKey(for_solution_area_key);

			if (fsaEntity != null) {
				if ("1".equals(append_parts)) {
					// 等待解决状态变为“中断解决追加零件”
					fsoServer.updateToAppend(fsaEntity, conn);
				} else {
					fsaEntity.setResolver_id(user.getOperator_id());
					// 等待解决完成
					fsaMapper.solve(fsaEntity);
				}
			}
		}
		// Add in Rvs2+ End


		// 如果没有选择返工，处理逻辑同closebreak TODELETE
		if (reworkPositions.size() == 0) {
			if (pat_id == null) {
				closebreak(req, conn);
			} else {
				resignbreak(req, triggerList, conn);
			}
		} else {
			reworkbreak(req, conn, reworkPositions, triggerList);
		}
	}

	/**
	 * 处理内容添加到总组工程检查票
	 * @param req
	 * @param user
	 * @param kind 
	 * @param conn
	 * @throws Exception
	 */
	public void createResolveLeaderInput(HttpServletRequest req, LoginData user, String kind, SqlSessionManager conn) throws Exception {
		String material_id = req.getParameter("material_id");

		// 取得最新rework在line_id
		ProductionFeatureMapper pfMapper = conn.getMapper(ProductionFeatureMapper.class);
		String lineId = "00000000014";
		if (kind != null) {
			if ("07".equals(kind) || "7".equals(kind)) {
				lineId = "00000000070";
			} else if ("06".equals(kind) || "6".equals(kind)) {
				lineId = "00000000050";
			} else if ("03".equals(kind) || "3".equals(kind) ||
					"04".equals(kind) || "4".equals(kind)) {
				lineId = "00000000061";
			}
		}
		int lineMaxRework = pfMapper.getReworkCountWithLine(material_id, lineId);

		String comment = req.getParameter("comment");

		// 保存到线长工检票记录
		LeaderPcsInputMapper dao = conn.getMapper(LeaderPcsInputMapper.class);
		String ecCode = "EC00001"; // TODO

		String pcs_comments = "{\"" + ecCode + "\" : \"" + comment + "\"}";

		ProductionFeatureEntity pfBean = new ProductionFeatureEntity();
		pfBean.setMaterial_id(material_id);
		pfBean.setPcs_inputs("{}");
		pfBean.setPcs_comments(RvsUtils.setContentWithMemo(
				pcs_comments, PcsUtils.PCS_COMMENTS_SIZE, conn));
		pfBean.setOperator_id(user.getOperator_id());
		pfBean.setLine_id(lineId);
		pfBean.setRework(lineMaxRework);

		dao.insert(pfBean);
	}

	/**
	 * 重新指派
	 * @param req
	 * @param conn
	 * @throws Exception
	 */
	public void resignbreak(HttpServletRequest req, List<String> triggerList, SqlSessionManager conn) throws Exception {
		// 取得用户信息
		HttpSession session = req.getSession();
		LoginData user = (LoginData) session.getAttribute(RvsConsts.SESSION_USER);

		String material_id = req.getParameter("material_id");

		ProductionFeatureMapper ppDao = conn.getMapper(ProductionFeatureMapper.class);
		// 本工位作业状态等待的记录中取得Rework
		int rework = 0;
		rework = ppDao.getReworkCount(material_id);

		// 从开始重新指派
		ProductionFeatureService pfService = new ProductionFeatureService();
		pfService.reprocess(material_id, rework, ppDao,triggerList, conn); // Rework 加 1

		// 处理人处理信息
		String message_id = req.getParameter("alarm_messsage_id");
		if (message_id != null) {
			AlarmMesssageSendationEntity sendation = new AlarmMesssageSendationEntity();
			sendation.setAlarm_messsage_id(message_id);
			sendation.setComment(req.getParameter("comment"));
			sendation.setRed_flg(1);
			sendation.setSendation_id(user.getOperator_id());
			sendation.setResolve_time(new Date());

			AlarmMesssageMapper amDao = conn.getMapper(AlarmMesssageMapper.class);
			int me = amDao.countAlarmMessageSendation(sendation);
			if (me <= 0) {
				// 没有发给处理者的信息时（代理线长），新建一条
				amDao.createAlarmMessageSendation(sendation);
			} else {
				amDao.updateAlarmMessageSendation(sendation);
			}
		}
	}

	/**
	 * 建立点检错误警报信息
	 * @param smlEntity 
	 * @param conn 数据库连接
	 * @throws Exception 
	 */
	public String createInfectAlarmMessage(AlarmMesssageEntity entity, String manager_operator_id, SqlSessionManager conn) throws Exception {
		AlarmMesssageMapper dao = conn.getMapper(AlarmMesssageMapper.class);

		List<AlarmMesssageEntity> amList = dao.searchAlarmMessagesInfect(entity);
		if (amList.size() > 0) {
			// if (amList.get(0).getReason() == 9) { TODO month
				return amList.get(0).getAlarm_messsage_id();
			// }
		}

		// 建立警报信息记录
		dao.createAlarmMessage(entity);

		// 取到信息主键
		CommonMapper cDao = conn.getMapper(CommonMapper.class);
		String amId = cDao.getLastInsertID();

		if (amId == null){
			logger.error("getLastInsertID失败！！");
			List<AlarmMesssageEntity> l = dao.searchAlarmMessages(entity);
			if (l != null && l.size() > 0) {
				amId = l.get(0).getAlarm_messsage_id();
			}
			logger.error("重新试图取得的ID是：" + amId);
		}
		// 查找发送目标
		// 上级
		AlarmMesssageSendationEntity sendation = new AlarmMesssageSendationEntity();
		sendation.setAlarm_messsage_id(amId);
		sendation.setSendation_id(manager_operator_id);
		dao.createAlarmMessageSendation(sendation);

//		HttpAsyncClient httpclient = new DefaultHttpAsyncClient();
//		httpclient.start();
//
//		try { 
//			HttpGet request = new HttpGet("http://localhost:8080/rvsTouch/beep/" + entity.getSection_id() +
//					"/" + entity.getLine_id());
//			logger.info("finger:"+request.getURI());
//	        httpclient.execute(request, null);
//        } catch (Exception e) {
//		} finally {
//			Thread.sleep(100);
//			httpclient.shutdown();
//		}

		return amId;
	}
}
