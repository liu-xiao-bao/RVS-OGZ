package com.osh.rvs.service;

import static framework.huiqing.common.util.CommonStringUtil.isEmpty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.DefaultHttpAsyncClient;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionManager;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;

import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.osh.rvs.bean.LoginData;
import com.osh.rvs.bean.data.AlarmMesssageEntity;
import com.osh.rvs.bean.data.AlarmMesssageSendationEntity;
import com.osh.rvs.bean.data.PostMessageEntity;
import com.osh.rvs.bean.infect.CheckResultEntity;
import com.osh.rvs.bean.infect.CheckUnqualifiedRecordEntity;
import com.osh.rvs.bean.infect.CheckedFileStorageEntity;
import com.osh.rvs.bean.infect.DryingOvenDeviceEntity;
import com.osh.rvs.bean.infect.ElectricIronDeviceEntity;
import com.osh.rvs.bean.infect.JigCheckResultEntity;
import com.osh.rvs.bean.infect.PeriodsEntity;
import com.osh.rvs.bean.infect.TorsionDeviceEntity;
import com.osh.rvs.bean.master.CheckFileManageEntity;
import com.osh.rvs.bean.master.DeviceCheckItemEntity;
import com.osh.rvs.bean.master.DevicesManageEntity;
import com.osh.rvs.bean.master.JigManageEntity;
import com.osh.rvs.bean.master.LineEntity;
import com.osh.rvs.bean.master.OperatorEntity;
import com.osh.rvs.bean.master.OperatorNamedEntity;
import com.osh.rvs.bean.master.PositionEntity;
import com.osh.rvs.bean.master.SectionEntity;
import com.osh.rvs.common.PathConsts;
import com.osh.rvs.common.RvsConsts;
import com.osh.rvs.common.RvsUtils;
import com.osh.rvs.common.XlsUtil;
import com.osh.rvs.form.infect.CheckUnqualifiedRecordForm;
import com.osh.rvs.form.infect.ToolsCheckResultForm;
import com.osh.rvs.form.infect.UsageCheckForm;
import com.osh.rvs.mapper.CommonMapper;
import com.osh.rvs.mapper.data.AlarmMesssageMapper;
import com.osh.rvs.mapper.data.PostMessageMapper;
import com.osh.rvs.mapper.infect.CheckResultMapper;
import com.osh.rvs.mapper.infect.CheckUnqualifiedRecordMapper;
import com.osh.rvs.mapper.infect.DryingOvenDeviceMapper;
import com.osh.rvs.mapper.infect.ElectricIronDeviceMapper;
import com.osh.rvs.mapper.infect.JigCheckResultMapper;
import com.osh.rvs.mapper.infect.TorsionDeviceMapper;
import com.osh.rvs.mapper.master.CheckFileManageMapper;
import com.osh.rvs.mapper.master.DevicesManageMapper;
import com.osh.rvs.mapper.master.HolidayMapper;
import com.osh.rvs.mapper.master.JigManageMapper;
import com.osh.rvs.mapper.master.LineMapper;
import com.osh.rvs.mapper.master.OperatorMapper;
import com.osh.rvs.mapper.master.PositionMapper;
import com.osh.rvs.mapper.master.SectionMapper;

import framework.huiqing.common.util.AutofillArrayList;
import framework.huiqing.common.util.CodeListUtils;
import framework.huiqing.common.util.CommonStringUtil;
import framework.huiqing.common.util.copy.BeanUtil;
import framework.huiqing.common.util.copy.CopyOptions;
import framework.huiqing.common.util.copy.DateUtil;
import framework.huiqing.common.util.message.ApplicationMessage;

public class CheckResultService {

	private static final int TYPE_ITEM_FREE = 0;
	private static final int TYPE_ITEM_DAY = 1;
	public static final int TYPE_ITEM_WEEK = 2;
	public static final int TYPE_ITEM_MONTH = 3;
	public static final int TYPE_ITEM_PERIOD = 4;
	public static final int TYPE_ITEM_YEAR = 5;

	private static final Integer TYPE_FILED_WEEK_OF_MONTH = 6;
	private static final Integer TYPE_FILED_MONTH = 7;
	private static final Integer TYPE_FILED_YEAR = 8;
	private static final String ELECTRIC_IRON_FILE_A = "00000000100";
	private static final String ELECTRIC_IRON_FILE_B = "00000000101";
	private static final String ELECTRIC_IRON_FILE_B2 = "00000000102";
	private static final String TORSION_FILE_A = "00000000105";
	private static final String TORSION_FILE_B = "00000000106";
	private static final String TORSION_FILE_C = "00000000107";
	private static final String TORSION_FILE_D = "00000000108";
	private static final String TORSION_FILE_E = "00000000109";
	private static final String TORSION_FILE_F = "00000000110";
	private static final String TORSION_FILE_G = "00000000111";
	private static final String TORSION_FILE_H = "00000000112";

	// 按照日期的所在点检周期信息
	private static Map<String, PeriodsEntity> periodsOfDate = new HashMap<String, PeriodsEntity>();
	private static Map<String, Date[][]> weekEndOfMonth = new HashMap<String, Date[][]>();

	Logger _logger = Logger.getLogger(CheckResultService.class);

	/**
	 * 确认点检项目是否建立
	 * @param operator_id
	 * @param list 
	 * @param position_id
	 * @param line_id
	 * @param form 
	 * @param callbackResponse 
	 * @param conn
	 * @return X（新建立Z）件点检项目，其中必须当天完成Y件点检项目
	 * @throws Exception 
	 */
	public String checkAndCreateItems(String section_id, String operator_id, List<PositionEntity> positionList, String position_id,
			String line_id, ActionForm form, Map<String, Object> callbackResponse, SqlSession conn, int isLeader) throws Exception {
		// 返回信息
		String ret = "";
		CheckResultMapper crMapper = conn.getMapper(CheckResultMapper.class);
		UsageCheckForm ucForm = (UsageCheckForm) form;
		CheckResultEntity entity = new CheckResultEntity();
		CheckResultEntity entityConditionForTorsion = new CheckResultEntity();
		CheckResultEntity entityConditionForElectricIron = new CheckResultEntity();
		BeanUtil.copyToBean(ucForm, entity, CopyOptions.COPYOPTIONS_NOEMPTY);
		BeanUtil.copyToBean(ucForm, entityConditionForTorsion, CopyOptions.COPYOPTIONS_NOEMPTY);
		BeanUtil.copyToBean(ucForm, entityConditionForElectricIron, CopyOptions.COPYOPTIONS_NOEMPTY);

		// 取得当前所在周，月，期
		String todayString = DateUtil.toString(new Date(), DateUtil.ISO_DATE_PATTERN);
		PeriodsEntity periodsEntity = getPeriodsOfDate(todayString, conn);

		List<UsageCheckForm> ucList = new ArrayList<UsageCheckForm>();

		if (!"2".equals(ucForm.getObject_type())) {
			getDevices(ucList, section_id, operator_id, position_id, positionList, line_id, entity, periodsEntity, conn, crMapper, isLeader);
			if (entity.getDevice_type_id() == null 
					|| TORSION_FILE_A.equals(entity.getDevice_type_id())
					|| TORSION_FILE_B.equals(entity.getDevice_type_id())
					|| TORSION_FILE_C.equals(entity.getDevice_type_id())
					|| TORSION_FILE_D.equals(entity.getDevice_type_id())
					|| TORSION_FILE_E.equals(entity.getDevice_type_id())
					|| TORSION_FILE_F.equals(entity.getDevice_type_id())
					|| TORSION_FILE_G.equals(entity.getDevice_type_id())
					|| TORSION_FILE_H.equals(entity.getDevice_type_id()))
				getTorsionDevices(ucList, section_id, operator_id, position_id, positionList, entityConditionForTorsion, periodsEntity, conn, crMapper, isLeader);
			if (entity.getDevice_type_id() == null 
					|| ELECTRIC_IRON_FILE_A.equals(entity.getDevice_type_id())
					|| ELECTRIC_IRON_FILE_B.equals(entity.getDevice_type_id())
					|| ELECTRIC_IRON_FILE_B2.equals(entity.getDevice_type_id()))
				getElectricIronDevices(ucList, section_id, operator_id, position_id, positionList, entityConditionForElectricIron, periodsEntity, conn, crMapper, isLeader);
		}

		if (!"1".equals(ucForm.getObject_type()) && isEmpty(ucForm.getSheet_manage_no()) && isEmpty(ucForm.getDevices_type_id())) {
			getTools(ucList, section_id, operator_id, position_id, line_id, entity, periodsEntity, conn, crMapper, isLeader);
		}

		// 过滤数据
		if (ucForm.getCheck_proceed() != null || ucForm.getCheck_result() != null){
			String check_proceed = ucForm.getCheck_proceed();
			String check_result = ucForm.getCheck_result();

			// 0:未点检;1:点检中;2:已点检
			// 1:通过;2:不通过;3:遗失;4:备品
			for (int idx = ucList.size() - 1; idx >= 0 ;idx--) {
				UsageCheckForm ucf = ucList.get(idx);
				boolean filterOut = false;
				if ("1".equals(check_proceed) && !"2".equals((ucf.getCheck_proceed()))) {
					filterOut = true;
				}
				if ("2".equals(check_proceed) && "2".equals((ucf.getCheck_proceed()))) {
					filterOut = true;
				}
				if ("1".equals(check_result) && !"1".equals((ucf.getCheck_result()))) {
					filterOut = true;
				}
				if ("2".equals(check_result) && "1".equals((ucf.getCheck_result()))) {
					filterOut = true;
				}

				if (filterOut) {
					ucList.remove(ucf);
				}
			}
		}
		callbackResponse.put("ucList", ucList);
		return ret;
	}

	/**
	 * 力矩设备一览
	 * @param ucList
	 * @param section_id
	 * @param operator_id
	 * @param position_id
	 * @param positionList
	 * @param line_id
	 * @param condEntity
	 * @param periodsEntity
	 * @param conn
	 * @param crMapper
	 * @param isLeader
	 * @throws Exception
	 */
	public void getTorsionDevices(List<UsageCheckForm> ucList,
			String section_id, String operator_id, String position_id,
			List<PositionEntity> positionList,
			CheckResultEntity condEntity, PeriodsEntity periodsEntity,
			SqlSession conn, CheckResultMapper crMapper, int isLeader) throws Exception {

		// 查询根据当前周期（周）是否有相关但没有生成（0状态）的点检记录
		TorsionDeviceMapper tdMapper = conn.getMapper(TorsionDeviceMapper.class);
		List<DevicesManageEntity> checkFileList = null; // 工位单位
		if (isLeader > 0) {
			if (isLeader == 1) { // 线长管理者 else 设备管理员
				condEntity.setOperator_id(operator_id);
			}
			checkFileList = tdMapper.searchTorsionDeviceOnLineByManager(condEntity);
		} else {
			condEntity.setSection_id(section_id);
			checkFileList = new ArrayList<DevicesManageEntity>(); 
			String cond_position_id = condEntity.getPosition_id();
			if (positionList != null) {
				for (PositionEntity position : positionList) {
					if (cond_position_id == null || cond_position_id.equals(position.getPosition_id())) {
						condEntity.setPosition_id(position.getPosition_id());
						checkFileList.addAll(tdMapper.searchTorsionDeviceOnLineByOperator(condEntity));
					}
				}
			} else if(position_id != null) {
				condEntity.setPosition_id(position_id);
				checkFileList = tdMapper.searchTorsionDeviceOnLineByManager(condEntity);
			}
		}

		// 力矩设备一览
		for (DevicesManageEntity checkFile : checkFileList) {
			String manage_code = checkFile.getManage_code();

			List<CheckResultEntity> checkResults = null;

			int status = 0; // 未开始点检
			boolean empty = false; // 点检未完成

			CheckResultEntity cond = new CheckResultEntity();
			cond.setCheck_file_manage_id(checkFile.getDaily_sheet_manage_no()); // cfm.check_file_manage_id as daily_sheet_manage_no
			cond.setCheck_confirm_time_start(periodsEntity.getStartOfWeek());
			cond.setCheck_confirm_time_end(periodsEntity.getEndOfWeek());
			cond.setPosition_id(checkFile.getPosition_id());
			cond.setSection_id(checkFile.getSection_id());
			checkResults = crMapper.getNeedTorsionCheck(cond);

			// 待点检信息没有时，按现在时间建立
			if (checkResults.size() == 0) {

				TorsionDeviceEntity tdCond = new TorsionDeviceEntity();
				tdCond.setSection_id(checkFile.getSection_id());
				tdCond.setPosition_id(checkFile.getPosition_id());
				List<TorsionDeviceEntity> tds = tdMapper.searchTorsionDevice(tdCond);

				// 临时采用可写连接
				SqlSessionManager writableConn = RvsUtils.getTempWritableConn();
				CheckResultMapper crWriteMapper = writableConn.getMapper(CheckResultMapper.class);

				try {
					writableConn.startManagedSession(false);
					CheckResultEntity createWait = new CheckResultEntity();
					createWait.setCheck_file_manage_id(checkFile.getDaily_sheet_manage_no()); // check_file_manage_id as daily_sheet_manage_no

					for (TorsionDeviceEntity td : tds) {
						createWait.setManage_id(td.getManage_id());
						createWait.setItem_seq(td.getSeq());
						crWriteMapper.createDeviceWaitingCheck(createWait);
					}
					writableConn.commit();
				} catch (Exception e) {
					if (writableConn!= null && writableConn.isManagedSessionStarted()) {
						writableConn.rollback();
					}
				} finally {
					try {
						writableConn.close();
					} catch (Exception e) {
					} finally {
						writableConn = null;
					}
				} if (ucList == null) continue;

				status = 0; // 刚生成
			} else {
				if (ucList == null) continue; if (isLeader == -1) continue; // 仅仅是判断待点检的情况
				status = 0;
				// Map<Integer, Date> un = new HashMap<Integer, Date>();

				for (CheckResultEntity checkResult : checkResults) {
					Integer checked_status = checkResult.getChecked_status();
//					periodsEntity.getStartOfMonth();
					if (checked_status == 4) {
						status = 4;
						break;
					} else if (checked_status == 0) {
						empty = true;
//						break;
					} else if (checked_status == 2) {
						status = 2;
						break;
					} else if (checked_status == 3) {
						status = 3;
						break;
					} else if (checked_status == 1 && status == 0) {
						status = 1;
					}
				}
			}

			// 返回值
			UsageCheckForm ucForm = new UsageCheckForm();
			ucForm.setManage_id(checkFile.getDevices_manage_id());
			ucForm.setName(checkFile.getName());
			ucForm.setModel_name(checkFile.getModel_name());
			ucForm.setSheet_manage_no(checkFile.getRegular_sheet_manage_no()); // cfm.sheet_file_name as regular_sheet_manage_no
			ucForm.setSection_id(checkFile.getSection_id());
			ucForm.setPosition_id(checkFile.getPosition_id());
			ucForm.setProcess_code(checkFile.getProcess_code());
			ucForm.setCycle_type("" + TYPE_FILED_WEEK_OF_MONTH);
			ucForm.setCheck_file_manage_id(checkFile.getDaily_sheet_manage_no()); // check_file_manage_id as daily_sheet_manage_no
			ucForm.setManage_code(manage_code);

			ucForm.setObject_type("1"); // 设备工具

			String check_proceed = "0";
			if (status != 0){
				if (empty) check_proceed = "1";
				else check_proceed = "2";
			}
			ucForm.setCheck_proceed(check_proceed);
			if ("2".equals(check_proceed) || status == 2)
				ucForm.setCheck_result(""+status);
//			if (condEntity.getCheck_proceed() != null) {
//				
//			}
			if (condEntity.getChecked_status() != null) {
				if (status != condEntity.getChecked_status()) 
					continue;
			}

			ucList.add(ucForm);
		}
	}

	/**
	 * 电烙铁工具一览
	 * @param ucList
	 * @param section_id
	 * @param operator_id
	 * @param position_id
	 * @param condEntity
	 * @param periodsEntity
	 * @param conn
	 * @param crMapper
	 * @param isLeader
	 * @throws Exception
	 */
	public void getElectricIronDevices(List<UsageCheckForm> ucList,
			String section_id, String operator_id, 
			String position_id, List<PositionEntity> positionList,
			CheckResultEntity condEntity, PeriodsEntity periodsEntity,
			SqlSession conn, CheckResultMapper crMapper, int isLeader) throws Exception {

		// 查询根据当天是否有相关但没有生成（0状态）的点检记录
		ElectricIronDeviceMapper eidMapper = conn.getMapper(ElectricIronDeviceMapper.class);
		List<DevicesManageEntity> checkFileList = null;
		if (isLeader > 0) {
			if (isLeader == 1) {
				condEntity.setOperator_id(operator_id);
			}
			checkFileList = eidMapper.searchElectricIronDeviceOnLineByManager(condEntity);
		} else {
			condEntity.setSection_id(section_id);
			if (positionList != null && positionList.size() > 0) {
				checkFileList = new ArrayList<DevicesManageEntity>();
				for (PositionEntity position : positionList) {
					if (position_id == null || position_id.equals(position.getPosition_id())) {
						condEntity.setPosition_id(position.getPosition_id());
						checkFileList.addAll(eidMapper.searchElectricIronDeviceOnLineByOperator(condEntity));
					}
				}
			} else if (position_id != null){
				condEntity.setPosition_id(position_id);
				checkFileList = eidMapper.searchElectricIronDeviceOnLineByOperator(condEntity);
			}
		}

		for (DevicesManageEntity checkFile : checkFileList) {
			String manage_code = checkFile.getManage_code();

			List<CheckResultEntity> checkResults = null;

			int status = 0; // 未开始点检
			boolean empty = false; // 点检未完成

			Integer kind = checkFile.getSpecialized();
			String checkFileManageId = ELECTRIC_IRON_FILE_B;
			String sheetManageNo = "MR0501-4 电烙铁点检记录(日常)";
			switch(kind) {
			case 1: 
				checkFileManageId = ELECTRIC_IRON_FILE_A; 
				sheetManageNo = "MR0501-4A 成型电烙铁点检记录(日常)";
				break;
			case 2: 
				checkFileManageId = ELECTRIC_IRON_FILE_B; 
				sheetManageNo = "MR0501-4B 焊接电烙铁点检记录(日常)";
				break;
			case 3: 
				checkFileManageId = ELECTRIC_IRON_FILE_B2; 
				sheetManageNo = "MR0501-4B 焊接电烙铁点检记录(日常)";
				break;
			}

			CheckResultEntity cond = new CheckResultEntity();
			cond.setCheck_file_manage_id(checkFileManageId);
			cond.setPosition_id(checkFile.getPosition_id());
			cond.setSection_id(checkFile.getSection_id());
			checkResults = crMapper.getNeedElectricIronCheck(cond);

			// 待点检信息没有时，按现在时间建立
			if (checkResults.size() == 0) {

				ElectricIronDeviceEntity tdCond = new ElectricIronDeviceEntity();
				tdCond.setSection_id(checkFile.getSection_id());
				tdCond.setPosition_id(checkFile.getPosition_id());
				tdCond.setKind(checkFile.getSpecialized());
				List<ElectricIronDeviceEntity> eids = eidMapper.searchElectricIronDevice(tdCond);

				// 临时采用可写连接
				SqlSessionManager writableConn = RvsUtils.getTempWritableConn();
				CheckResultMapper crWriteMapper = writableConn.getMapper(CheckResultMapper.class);

				try {
					writableConn.startManagedSession(false);
					CheckResultEntity createWait = new CheckResultEntity();
					createWait.setCheck_file_manage_id(checkFileManageId);

					for (ElectricIronDeviceEntity eid : eids) {
						String manage_id = eid.getManage_id();
						createWait.setManage_id(manage_id);
						createWait.setItem_seq(eid.getSeq());
						crWriteMapper.createDeviceWaitingCheck(createWait);
					}
					writableConn.commit();
				} catch (Exception e) {
					if (writableConn!= null && writableConn.isManagedSessionStarted()) {
						writableConn.rollback();
					}
				} finally {
					try {
						writableConn.close();
					} catch (Exception e) {
					} finally {
						writableConn = null;
					}
				} if (ucList == null) continue;

				status = 0; // 刚生成
			} else {
				if (ucList == null) continue; if (isLeader == -1) continue; // 仅仅是判断待点检的情况
				status = 0;
				// Map<Integer, Date> un = new HashMap<Integer, Date>();

				for (CheckResultEntity checkResult : checkResults) {
					Integer checked_status = checkResult.getChecked_status();
//					periodsEntity.getStartOfMonth();
					if (checked_status == 4) {
						status = 4;
						break;
					} else if (checked_status == 0) {
						empty = true;
//						break;
					} else if (checked_status == 2) {
						status = 2;
						break;
					} else if (checked_status == 3) {
						status = 3;
						break;
					} else if (checked_status == 1 && status == 0) {
						status = 1;
					}
				}
			}

			// 返回值
			UsageCheckForm ucForm = new UsageCheckForm();
			ucForm.setManage_id(checkFile.getDevices_manage_id());
			ucForm.setName(checkFile.getName());
			ucForm.setModel_name(checkFile.getModel_name());
			ucForm.setSheet_manage_no(sheetManageNo);
			ucForm.setSection_id(checkFile.getSection_id());
			ucForm.setPosition_id(checkFile.getPosition_id());
			ucForm.setProcess_code(checkFile.getProcess_code());
			ucForm.setCycle_type("0");
			ucForm.setCheck_file_manage_id(checkFileManageId);
			ucForm.setManage_code(manage_code);

			ucForm.setObject_type("1"); // 设备工具

			String check_proceed = "0";
			if (status != 0){
				if (empty) check_proceed = "1";
				else check_proceed = "2";
			}
			ucForm.setCheck_proceed(check_proceed);
			if ("2".equals(check_proceed) || status == 2)
				ucForm.setCheck_result(""+status);
//			if (condEntity.getCheck_proceed() != null) {
//				
//			}
			if (condEntity.getChecked_status() != null) {
				if (status != condEntity.getChecked_status()) 
					continue;
			}

			ucList.add(ucForm);
		}
	}

	/**
	 * 取得条件下的全部治具
	 * @param ucList
	 * @param section_id
	 * @param operator_id
	 * @param position_id
	 * @param line_id
	 * @param periodsEntity
	 * @param conn
	 * @param crMapper
	 * @param isLeader
	 */
	private void getTools(List<UsageCheckForm> ucList, String section_id,
			String operator_id, String position_id, String line_id, CheckResultEntity crEntity,
			PeriodsEntity periodsEntity, SqlSession conn,
			CheckResultMapper crMapper, int isLeader) {

		if (isLeader == 2) { // 设备管理员无限制
		} else if (isLeader == 1) {
			crEntity.setManager_operator_id(operator_id);
			crEntity.setOperator_id(null);
		} else {
			crEntity.setOperator_id(operator_id);
		}

		crEntity.setCheck_confirm_time_start(periodsEntity.getStartOfMonth());
		crEntity.setCheck_confirm_time_end(periodsEntity.getEndOfMonth());

		List<JigManageEntity> positions = crMapper.searchToolCheckPositionsByOperator(crEntity);
		for (JigManageEntity position : positions) {
			if (position.getManage_code() != null) continue;

			UsageCheckForm toolPos = new UsageCheckForm();
			toolPos.setPosition_id(position.getPosition_id());
			toolPos.setSection_id(position.getSection_id());
			toolPos.setOperator_id(position.getResponsible_operator_id());
			toolPos.setProcess_code(position.getProcess_code());
			toolPos.setObject_type("2"); // 治具
			toolPos.setName("-");
			toolPos.setModel_name("-");
			toolPos.setManage_code("(责任人 " + position.getResponsible_operator() + ")");
			toolPos.setSheet_manage_no("-");
			toolPos.setCycle_type("8"); // 月
			if (!"0".equals(position.getClassify())) {
				toolPos.setCheck_proceed(position.getClassify());
				if (!"0".equals(position.getStatus())) {
					toolPos.setCheck_result("2");
				} else {
					toolPos.setCheck_result("1");
				}
			} else {
				toolPos.setCheck_proceed("0");
			}
			ucList.add(toolPos);
		}
	}

	/**
	 * 取得条件下的全部设备工具
	 * @param ucList 返回列表
	 * @param section_id
	 * @param operator_id
	 * @param position_id
	 * @param positionList
	 * @param line_id
	 * @param condEntity
	 * @param periodsEntity
	 * @param conn
	 * @param crMapper
	 * @param isLeader
	 * @throws Exception
	 */
	public void getDevices(List<UsageCheckForm> ucList, String section_id,
			String operator_id, String current_position_id, List<PositionEntity> positionList, String line_id,
			CheckResultEntity condEntity, PeriodsEntity periodsEntity, SqlSession conn, 
			CheckResultMapper crMapper, int isLeader) throws Exception {
		// 日期（默认今天）
		Calendar showDate = Calendar.getInstance();
		showDate.set(Calendar.HOUR_OF_DAY, 0);
		showDate.set(Calendar.MINUTE, 0);
		showDate.set(Calendar.SECOND, 0);
		showDate.set(Calendar.MILLISECOND, 0);

		// 查询根据当前周期（日，周，月，期）是否有相关但没有生成（0状态）的点检记录
		CheckFileManageMapper cfmMapper = conn.getMapper(CheckFileManageMapper.class);
		List<CheckFileManageEntity> checkFileList = null;
		if (isLeader > 0) {
			if (isLeader == 1) {
				condEntity.setOperator_id(operator_id);
			}
			checkFileList = cfmMapper.searchManageCodeByManager(condEntity);
		} else {
			checkFileList = new ArrayList<CheckFileManageEntity>();
			String cond_position_id = condEntity.getPosition_id();
			if (positionList != null) {
				for (PositionEntity position : positionList) {
					if (cond_position_id == null || cond_position_id.equals(position.getPosition_id())) {
						condEntity.setSection_id(section_id);
						condEntity.setLine_id(line_id);
						condEntity.setPosition_id(position.getPosition_id());
						checkFileList.addAll(cfmMapper.searchManageCodeByOperator(condEntity));
					}
				}
			} else if (isLeader == -1) {
				condEntity.setSection_id(section_id);
				condEntity.setLine_id(line_id);
				condEntity.setPosition_id(current_position_id);
				checkFileList.addAll(cfmMapper.searchManageCodeByOperator(condEntity));
			}
		}

		Set<String> diffKeys = new HashSet<String>();
		Map<String, List<DeviceCheckItemEntity>> itemsCache = new HashMap<String, List<DeviceCheckItemEntity>>();

		// 单个管理编号+点检表循环
		for (CheckFileManageEntity checkFile : checkFileList) {
			String manage_code = checkFile.getManage_code();
			String check_file_manage_id = checkFile.getCheck_file_manage_id();

			if (diffKeys.contains(manage_code + "_" + check_file_manage_id)) {
				continue;
			} else {
				diffKeys.add(manage_code + "_" + check_file_manage_id);
			}

			// 取得需点检项目
			List<DeviceCheckItemEntity> items = null;
			if (!itemsCache.containsKey(check_file_manage_id)) {
				items = cfmMapper.getSeqItemsByFile(check_file_manage_id);
				itemsCache.put(check_file_manage_id, items);
			} else {
				items = itemsCache.get(check_file_manage_id);
			}
			// 收集此表触发限制种类
			Set<Integer> triggerStatusSet = new HashSet<Integer>();
			for (DeviceCheckItemEntity item : items) {
				triggerStatusSet.add(item.getTrigger_state());
			}

			int status = 0; // 未开始点检
			boolean empty = false; // 存在点检未完成项目

			List<CheckResultEntity> checkResults = new ArrayList<CheckResultEntity>();
			if (CheckFileManageEntity.ACCESS_PLACE_BEFORE_USE == checkFile.getAccess_place()) {
				// 使用前不需要设定待点检
				if (isLeader > 0) {
					// 线长以上
					for (Integer trigger_status : triggerStatusSet) {
						CheckResultEntity cond = new CheckResultEntity();
						cond.setCheck_file_manage_id(check_file_manage_id);
						cond.setManage_id(checkFile.getDevices_manage_id());
						
						switch(trigger_status) {
						case TYPE_ITEM_DAY: 
							checkResults.addAll(crMapper.getNeedDailyCheck(cond));
							break;
						case TYPE_ITEM_WEEK:
							cond.setCheck_confirm_time_start(periodsEntity.getStartOfWeek());
							cond.setCheck_confirm_time_end(periodsEntity.getEndOfWeek());
							checkResults.addAll(crMapper.getNeedRegularCheck(cond));
							break;
						}

						// 是否点检过（因使用前必须全部点检）
						if (checkResults.size() == 0) {
							status = 0;
							empty = true;
						} else {
							status = 1;
						}
					}
				} else {
					// 操作者跳开
					continue;
				}
			} else {
				for (Integer trigger_status : triggerStatusSet) {
					CheckResultEntity cond = new CheckResultEntity();
					cond.setCheck_file_manage_id(check_file_manage_id);
					cond.setManage_id(checkFile.getDevices_manage_id());
					cond.setCycle_type(trigger_status);

					switch(trigger_status) {
					case TYPE_ITEM_DAY: 
						checkResults.addAll(crMapper.getNeedDailyCheck(cond));
						break;
					case TYPE_ITEM_WEEK:
						cond.setCheck_confirm_time_start(periodsEntity.getStartOfWeek());
						cond.setCheck_confirm_time_end(periodsEntity.getEndOfWeek());
						checkResults.addAll(crMapper.getNeedRegularCheck(cond));
						break;
					case TYPE_ITEM_MONTH:
						cond.setCheck_confirm_time_start(periodsEntity.getStartOfMonth());
						cond.setCheck_confirm_time_end(periodsEntity.getEndOfMonth());
						checkResults.addAll(crMapper.getNeedRegularCheck(cond));
						break;
					case TYPE_ITEM_PERIOD:
						cond.setCheck_confirm_time_start(periodsEntity.getStartOfHbp());
						cond.setCheck_confirm_time_end(periodsEntity.getEndOfHbp());
						checkResults.addAll(crMapper.getNeedRegularCheck(cond));
						break;
					case TYPE_ITEM_YEAR:
						cond.setCheck_confirm_time_start(periodsEntity.getStartOfPeriod());
						cond.setCheck_confirm_time_end(periodsEntity.getEndOfPeriod());
						checkResults.addAll(crMapper.getNeedRegularCheck(cond));
						break;
					}

				}

				if (ucList == null) continue; if (isLeader == -1) continue; // 仅仅是判断待点检的情况

				// 根据当前点检（或待点检）记录取得状态
				for (CheckResultEntity checkResult : checkResults) {
					Integer checked_status = checkResult.getChecked_status();

					if (checked_status == 0) { // 存在未点检
						empty = true;
					} else if (checked_status == 2) { // 不合格
						status = 2;
						break;
					} else if (checked_status == 3) { // 遗失
						status = 3;
						break;
					} else if (checked_status == 1 && status == 0) { // 已点检通过
						status = 1;
					}
				}

				List<String> lostItems = new ArrayList<String>();
				for (DeviceCheckItemEntity item : items) {
					// 判断型号
					if (!isEmpty(item.getSpecified_model_name())) {
						if (item.getSpecified_model_name().indexOf(checkFile.getModel_name()) < 0) {
							continue;
						}
					}
					if (item.getTrigger_state() == TYPE_ITEM_FREE) { // 无限制
						continue;
					}
					String checkItemSeq = item.getItem_seq();
					for (CheckResultEntity checkResult : checkResults) {
						if (checkItemSeq.equals(checkResult.getItem_seq())) {
							if (checkResult.getChecked_status() > 0 
									|| checkResult.getCheck_confirm_time().after(showDate.getTime())) {
								checkItemSeq = null; // 找到有点检（或待点检）记录
								break;
							}
						}
					}
					if (checkItemSeq != null)
						lostItems.add(checkItemSeq);
				}

				if (lostItems.size() > 0 ){
					// 待点检信息没有时，按现在时间建立
					CheckResultEntity createWait = new CheckResultEntity();
					createWait.setManage_id(checkFile.getDevices_manage_id());
					createWait.setCheck_file_manage_id(check_file_manage_id);

					// 临时采用可写连接
					SqlSessionManager writableConn = RvsUtils.getTempWritableConn();
					CheckResultMapper crWriteMapper = writableConn.getMapper(CheckResultMapper.class);

					try {
						writableConn.startManagedSession(false);

						for (String item_seq : lostItems) {
							createWait.setItem_seq(item_seq);

							crWriteMapper.createDeviceWaitingCheck(createWait);
							status = 0; // 有刚生成
							empty = true;
						}

						writableConn.commit();
					} catch (Exception e) {
						if (writableConn != null && writableConn.isManagedSessionStarted()) {
							writableConn.rollback();
						}
					} finally {
						try {
							writableConn.close();
						} catch (Exception e) {
						} finally {
							writableConn = null;
						}
					}
				}
			}

			// 返回值
			UsageCheckForm ucForm = new UsageCheckForm();
			ucForm.setManage_id(checkFile.getDevices_manage_id());
			ucForm.setName(checkFile.getName());
			ucForm.setModel_name(checkFile.getModel_name());
			ucForm.setSheet_manage_no(checkFile.getSheet_file_name());
			ucForm.setPosition_id(checkFile.getPosition_id());
			ucForm.setProcess_code(checkFile.getProcess_code());
			if (checkFile.getAccess_place() == CheckFileManageEntity.ACCESS_PLACE_BEFORE_USE) {
				ucForm.setCycle_type("9");
			} else {
				ucForm.setCycle_type(""+checkFile.getCycle_type());
			}
			ucForm.setCheck_file_manage_id(check_file_manage_id);
			ucForm.setManage_code(manage_code);

			ucForm.setObject_type("1"); // 设备工具

			String check_proceed = "0";
			if (status != 0){
				if (empty) check_proceed = "1";
				else check_proceed = "2";
			}
			ucForm.setCheck_proceed(check_proceed);
			if ("2".equals(check_proceed) || status == 2)
				ucForm.setCheck_result(""+status);

			if (condEntity.getChecked_status() != null) {
				if (status != condEntity.getChecked_status()) 
					continue;
			}

			ucList.add(ucForm);
		}
	}

	/**
	 * 取得或计算时间范围
	 * @param dayString
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public synchronized static PeriodsEntity getPeriodsOfDate(String dayString,
			SqlSession conn) throws Exception {
		if (periodsOfDate.containsKey(dayString)) {
			return periodsOfDate.get(dayString);
		}

		HolidayMapper hMapper = conn.getMapper(HolidayMapper.class);
		// 建立日的开始结束时间
		PeriodsEntity old = null;
		PeriodsEntity neo = new PeriodsEntity();
		for (String key : periodsOfDate.keySet()) {
			old = periodsOfDate.get(key);
		}
		synchronized (periodsOfDate) {
			periodsOfDate.clear();

			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			Date today = cal.getTime();

			// 周开始终了
			int week = cal.get(Calendar.DAY_OF_WEEK);
			if (week == Calendar.SUNDAY) {
				neo.setEndOfWeek(cal.getTime());

				cal.add(Calendar.DATE, -6);
				neo.setStartOfWeek(cal.getTime());
			} else {
				cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
				neo.setStartOfWeek(cal.getTime());

				cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
				cal.add(Calendar.DATE, 1);
				neo.setEndOfWeek(cal.getTime());
			}

			// 月开始终了
			cal.setTime(today);
			cal.set(Calendar.DATE, 1);
			neo.setStartOfMonth(cal.getTime());

			cal.add(Calendar.MONTH, 1);
			cal.add(Calendar.DATE, -1);
			neo.setEndOfMonth(cal.getTime());

			// 半月开始终了
			cal.setTime(today);
			if (cal.get(Calendar.DATE) <= 15) {
				neo.setStartOfHMonth(neo.getStartOfMonth());
				cal.set(Calendar.DATE, 15);
				neo.setEndOfHMonth(cal.getTime());
			} else {
				cal.set(Calendar.DATE, 16);
				neo.setStartOfHMonth(cal.getTime());
				neo.setEndOfHMonth(neo.getEndOfMonth());
			}
			
			// 半期开始终了
			cal.setTime(today);
			int nowMonth = cal.get(Calendar.MONTH);
			if (nowMonth < Calendar.APRIL) {
				cal.add(Calendar.YEAR, -1);
				cal.set(Calendar.MONTH, Calendar.APRIL);
				cal.set(Calendar.DATE, 1);
				neo.setStartOfPeriod(cal.getTime());
				cal.set(Calendar.MONTH, Calendar.OCTOBER);
			} else if (nowMonth >= Calendar.OCTOBER) {
				cal.set(Calendar.MONTH, Calendar.APRIL);
				cal.set(Calendar.DATE, 1);
				neo.setStartOfPeriod(cal.getTime());
				cal.set(Calendar.MONTH, Calendar.OCTOBER);
			} else {
				cal.set(Calendar.MONTH, Calendar.APRIL);
				cal.set(Calendar.DATE, 1);
				neo.setStartOfPeriod(cal.getTime());
			}

			neo.setStartOfHbp(cal.getTime());
			cal.add(Calendar.MONTH, 6);
			cal.add(Calendar.DATE, -1);
			neo.setEndOfHbp(cal.getTime());

			cal.setTimeInMillis(neo.getStartOfPeriod().getTime());
			cal.add(Calendar.MONTH, 12);
			cal.add(Calendar.DATE, -1);
			neo.setEndOfPeriod(cal.getTime());

			Map<String, Object> cond = new HashMap<String, Object>();

			// 周点检限期
			if (old != null) {
				// 如果上一天在同一个区域
				if (neo.getStartOfWeek().equals(old.getStartOfWeek())) {
					neo.setExpireOfWeek(old.getExpireOfWeek());
				}
				if (neo.getStartOfMonth().equals(old.getStartOfMonth())) {
					neo.setExpireOfMonth(old.getExpireOfMonth());
				}
				if (neo.getStartOfHMonth().equals(old.getStartOfHMonth())) {
					neo.setExpireOfHMonth(old.getExpireOfHMonth());
				}
				if (neo.getStartOfHbp().equals(old.getStartOfHbp())) {
					neo.setExpireOfHbp(old.getExpireOfHbp());
				}
				if (neo.getStartOfPeriod().equals(old.getStartOfPeriod())) {
					neo.setExpireOfPeriod(old.getExpireOfPeriod());
				}
			}

			if (neo.getExpireOfWeek() == null) {
				Date expireDate = new Date(neo.getStartOfWeek().getTime() - 1);
				// 往前1天开始算天数
				cond.put("date", expireDate);
				cond.put("interval", 2);
				expireDate = hMapper.addWorkdays(cond);
				if (expireDate.after(neo.getEndOfWeek()) ) {
					// 一周两天都没有的情况
					expireDate.setTime(neo.getStartOfWeek().getTime());
				}
				neo.setExpireOfWeek(expireDate);
			}
			if (neo.getExpireOfMonth() == null) {
				Date expireDate = new Date(neo.getStartOfMonth().getTime() - 1);

				cond.put("date", expireDate);
				// 月3天宽限
				cond.put("interval", 4);
				expireDate = hMapper.addWorkdays(cond);
				neo.setExpireOfMonth(expireDate);
				cond.put("date", expireDate);
				cond.put("interval", 2);
				expireDate = hMapper.addWorkdays(cond);
				neo.setExpireOfMonthOfJig(expireDate);
			}
			if (neo.getExpireOfMonthOfJig() == null) {
				Date expireDate = new Date(neo.getStartOfMonth().getTime() - 1);

				cond.put("date", expireDate);
				// 月5天宽限
				cond.put("interval", 6);
				expireDate = hMapper.addWorkdays(cond);
				neo.setExpireOfMonthOfJig(expireDate);
			}
			if (neo.getExpireOfHMonth() == null) {

				// 半月3天宽限
				if (neo.getStartOfMonth().equals(neo.getStartOfHMonth())) {
					neo.setExpireOfHMonth(neo.getExpireOfMonth());
				} else {
					Date expireDate = new Date(neo.getStartOfHMonth().getTime() - 1);
					cond.put("date", expireDate);
					cond.put("interval", 4);
					expireDate = hMapper.addWorkdays(cond);
					neo.setExpireOfHMonth(expireDate);
				}
			}

			if (neo.getExpireOfHbp() == null) {
				Date expireDate = new Date(neo.getStartOfHbp().getTime() - 1);
				
				cond.put("date", expireDate);
				cond.put("interval", 4);
				expireDate = hMapper.addWorkdays(cond);
				neo.setExpireOfHbp(expireDate);
			}

			if (neo.getExpireOfPeriod() == null) {
				Date expireDate = new Date(neo.getStartOfPeriod().getTime() - 1);
				
				cond.put("date", expireDate);
				cond.put("interval", 4);
				expireDate = hMapper.addWorkdays(cond);
				neo.setExpireOfPeriod(expireDate);
			}

			periodsOfDate.put(dayString, neo);
		}
		return neo;
	}

	/**
	 * 取得治具点检表
	 * @param section_id
	 * @param position_id
	 * @param operator_id
	 * @param isLeader
	 * @param adjustDate 
	 * @param conn
	 * @throws Exception 
	 */
	public String getToolCheckSheet(String section_id, String position_id, String operator_id, String post_operator_id,
			int leaderLevel, Date adjustDate, SqlSession conn) throws Exception {
		CheckResultMapper crMapper = conn.getMapper(CheckResultMapper.class);
		// <tr class='tcs_content'><td>1</td><td class=''>管理号码</td><td>治具号码</td><td class='HL WT'>专用工具名称</td><td>1</td><td>1月/次</td><td class='HC WT' colspan='12'>定期清点保养结果</td><td class='HL'>备注事项</td></tr>
		String retContent = "";

		BufferedReader input = null;
		StringBuffer checkContent = new StringBuffer("");

		Calendar adjustCal = Calendar.getInstance();
		adjustCal.setTime(adjustDate);
		adjustCal.set(Calendar.HOUR_OF_DAY, 0);
		adjustCal.set(Calendar.MINUTE, 0);
		adjustCal.set(Calendar.SECOND, 0);
		adjustCal.set(Calendar.MILLISECOND, 0);

		// crMapper.get
		ToolsCheckResultForm searchForm = new ToolsCheckResultForm();
		searchForm.setPosition_id(position_id);
		searchForm.setSection_id(section_id);
		if (leaderLevel == 2) {
			searchForm.setResponsible_operator_id(post_operator_id);
		} else if (leaderLevel == 1) {
			searchForm.setManager_operator_id(operator_id);
			searchForm.setResponsible_operator_id(post_operator_id);
		} else {
			searchForm.setResponsible_operator_id(operator_id);
		}
		ToolsCheckResultService tcrService = new ToolsCheckResultService();
		List<ToolsCheckResultForm> list = tcrService.searchToolsCheckResult(searchForm, conn);

		CheckResultEntity commentCondition = new CheckResultEntity();
		BeanUtil.copyToBean(searchForm, commentCondition, CopyOptions.COPYOPTIONS_NOEMPTY);
		String dayString = DateUtil.toString(adjustDate, DateUtil.ISO_DATE_PATTERN);
		PeriodsEntity pd = getPeriodsOfDate(dayString, conn);
		commentCondition.setCheck_confirm_time_start(pd.getStartOfPeriod());
		commentCondition.setCheck_confirm_time_end(pd.getEndOfPeriod());
		List<String> commentExists = getCommentsExists(2, commentCondition, crMapper);

		boolean isLeader = leaderLevel > 0;
		// 取得位置
		int axis = getAxis(adjustCal, TYPE_ITEM_MONTH, TYPE_FILED_YEAR);

		for (int ii = 0; ii < list.size();ii++) {
			ToolsCheckResultForm toolsCheckResultForm = list.get(ii);
			int isNowAvalible = isEmpty(toolsCheckResultForm.getResponsible_operator_id()) ? 0 : 12;
			String manage_id = toolsCheckResultForm.getManage_id();
			boolean hasPhoto = new File(PathConsts.BASE_PATH + PathConsts.PHOTOS + "\\jig\\" + toolsCheckResultForm.getTools_no()).exists();
			checkContent.append("<tr class='tcs_content' stat manage_id='"+manage_id+"'>"
					+ "<td>"+(ii+ 1)+"</td>"
					+ "<td>" +toolsCheckResultForm.getManage_code()+ "</td>"
					+ "<td>"+toolsCheckResultForm.getTools_no()+"</td>"
					+ "<td class='HL WT'>"+toolsCheckResultForm.getTools_name()+"</td>"
					+ "<td>1</td>"
					+ "<td>1月/次</td>"
					+ "<td class='WT'>"+getStatusT(toolsCheckResultForm.getApril(), isLeader, 0-axis+isNowAvalible, manage_id)+"</td>"
					+ "<td class='WT'>"+getStatusT(toolsCheckResultForm.getMay(), isLeader, 1-axis+isNowAvalible, manage_id)+"</td>"
					+ "<td class='WT'>"+getStatusT(toolsCheckResultForm.getJune(), isLeader, 2-axis+isNowAvalible, manage_id)+"</td>"
					+ "<td class='WT'>"+getStatusT(toolsCheckResultForm.getJuly(), isLeader, 3-axis+isNowAvalible, manage_id)+"</td>"
					+ "<td class='WT'>"+getStatusT(toolsCheckResultForm.getAugust(), isLeader, 4-axis+isNowAvalible, manage_id)+"</td>"
					+ "<td class='WT'>"+getStatusT(toolsCheckResultForm.getSeptember(), isLeader, 5-axis+isNowAvalible, manage_id)+"</td>"
					+ "<td class='WT'>"+getStatusT(toolsCheckResultForm.getOctober(), isLeader, 6-axis+isNowAvalible, manage_id)+"</td>"
					+ "<td class='WT'>"+getStatusT(toolsCheckResultForm.getNovember(), isLeader, 7-axis+isNowAvalible, manage_id)+"</td>"
					+ "<td class='WT'>"+getStatusT(toolsCheckResultForm.getDecember(), isLeader, 8-axis+isNowAvalible, manage_id)+"</td>"
					+ "<td class='WT'>"+getStatusT(toolsCheckResultForm.getJanuary(), isLeader, 9-axis+isNowAvalible, manage_id)+"</td>"
					+ "<td class='WT'>"+getStatusT(toolsCheckResultForm.getFebruary(), isLeader, 10-axis+isNowAvalible, manage_id)+"</td>"
					+ "<td class='WT'>"+getStatusT(toolsCheckResultForm.getMarch(), isLeader, 11-axis+isNowAvalible, manage_id)+"</td>"
					+ "<td class='HL'><input type='radio' class='t_comment' name='comment_target' id='comment_"+manage_id+"' value='"+manage_id+"'/>"
					+ "<label for='comment_"+manage_id+"'" + (commentExists.contains(CommonStringUtil.fillChar(manage_id, '0', 11, true)) ? " exists" : "") + ">备注</label>" 
					+ (hasPhoto ? "<button class='t_pic'>照片</button>" : "") + "</td></tr>");
		}
		try {
			input = new BufferedReader(new InputStreamReader(new FileInputStream(PathConsts.BASE_PATH + "\\DeviceInfection\\xml\\QF0601-5.html"),"UTF-8"));
			StringBuffer buffer = new StringBuffer();
			String text;

			while ((text = input.readLine()) != null)
				buffer.append(text);

			String content = buffer.toString();
			retContent = content.replaceAll("#insert#", checkContent.toString());

			PositionMapper pMapper = conn.getMapper(PositionMapper.class);
			PositionEntity position = pMapper.getPositionByID(position_id);

			retContent = retContent.replaceAll("#LINE#", position.getLine_name())
					.replaceAll("#POSITION#", position.getProcess_code())
					.replaceAll("#periodc#", RvsUtils.getBussinessYearString(adjustCal).replaceAll("P", ""));

			CheckResultEntity cond = new CheckResultEntity();
			// 取得各月点检人
			cond.setSection_id(section_id);
			cond.setPosition_id(position_id);

			// 去期间头
			Calendar monCal = getStartOfPeriod(adjustCal);

			for (int i=0;i<=axis;i++) {
				Date check_confirm_time_start = monCal.getTime();
				monCal.add(Calendar.MONTH, 1);
				monCal.add(Calendar.DATE, -1);
				Date check_confirm_time_end = monCal.getTime();
				monCal.add(Calendar.DATE, 1);
				cond.setCheck_confirm_time_start(check_confirm_time_start);
				cond.setCheck_confirm_time_end(check_confirm_time_end);
				if (!isLeader) {
					cond.setOperator_id(operator_id);
				} else {
					cond.setOperator_id(post_operator_id);
				}

				List<CheckResultEntity> lStamp = crMapper.getResponseStamp(cond);
				if (lStamp != null && lStamp.size() > 0) {
					retContent = retContent.replaceAll("#sign_"+i+"#", "<img src=\"/images/sign_v/" + lStamp.get(0).getJob_no() + "\"/>");
				}
				List<CheckResultEntity> lUpper = crMapper.getJigUpperStamp(cond);
				if (lUpper != null && lUpper.size() > 0) {
					retContent = retContent.replaceAll("#lder_"+i+"#", "<img src=\"/images/sign_v/" + lUpper.get(0).getJob_no() + "\"/>");
				} else if (isLeader && i==axis) {
					retContent = retContent.replaceAll("#lder_"+i+"#", "<input type='checkbox' id='upper_check' value='上级确认'/><label for='upper_check'>上级确认</label>");
				}
			}

			// 清除多余
			retContent = retContent.replaceAll("#[^#]*#", "");

		} catch (IOException ioException) {
		} finally {
			try {
				input.close();
			} catch (IOException e) {
			}
			input = null;
		}
		// 读取文件
		return retContent;
	}

	private List<String> getCommentsExists(int objectType, CheckResultEntity commentCondition, CheckResultMapper crMapper) {
		if (objectType == 2) {
			List<String> existsJigCheckComment = crMapper.checkExistsJigCheckCommentByCondition(commentCondition);
			return existsJigCheckComment;
		} else if (objectType == 1) {
			List<String> existsDeviceCheckComment = crMapper.checkExistsDeviceCheckCommentByCondition(commentCondition);
			return existsDeviceCheckComment;
		}
		return null;
	}

	private Calendar getStartOfPeriod(Calendar adjustCal) {
		String sdate = RvsUtils.getBussinessStartDate(adjustCal);
		Date date = DateUtil.toDate(sdate, DateUtil.DATE_PATTERN);
		Calendar ret = Calendar.getInstance();
		ret.setTime(date);
		return ret;
	}

	private int getMaxAxis(Integer itemType, Integer fileType) {
		if (itemType == TYPE_ITEM_MONTH && fileType == TYPE_FILED_YEAR) {
			return 11;
		} else if (itemType == TYPE_ITEM_PERIOD && fileType == TYPE_FILED_YEAR) {
			return 1;
		} else if (itemType == TYPE_ITEM_YEAR && fileType == TYPE_FILED_YEAR) {
			return 0;
//		} else if (type == TYPE_HALF_MONTH_OF_YEAR) {
//			return 23;
		} else if (itemType == TYPE_ITEM_DAY && fileType == TYPE_FILED_MONTH) {
			return 30;
		} else if (itemType == TYPE_ITEM_WEEK && fileType == TYPE_FILED_WEEK_OF_MONTH) {
			return 4;
		} else if (itemType == TYPE_ITEM_MONTH && fileType == TYPE_FILED_MONTH) {
			return 0;
		}
		return -1;
	}

	private int getAxis(Calendar adjustCal, Integer itemType, Integer fileType) {
		if (itemType == TYPE_ITEM_MONTH && fileType == TYPE_FILED_YEAR) {
			int month = adjustCal.get(Calendar.MONTH);
			if (month < 3) {
				return month + 9;
			} else {
				return month - 3;
			}
		} else if (itemType == TYPE_ITEM_PERIOD && fileType == TYPE_FILED_YEAR) {
			int month = adjustCal.get(Calendar.MONTH);
			if (month < 3) {
				return 1;
			} else if (month >= 9) {
				return 1;
			} else {
				return 0;
			}
		} else if (itemType == TYPE_ITEM_YEAR && fileType == TYPE_FILED_YEAR) {
			return 0;
		} else if (itemType == TYPE_ITEM_DAY && fileType == TYPE_FILED_MONTH) {
			return adjustCal.get(Calendar.DATE) - 1;
		} else if (itemType == TYPE_ITEM_WEEK && fileType == TYPE_FILED_WEEK_OF_MONTH) {
			int ret = 0;
			// java里的第一周如果没工作日则不算第一周,同理月末跨越下一周但没有工作日则算上一月的最后周
			Date[][] wesOfMonth = getWeekEndsOfMonth(adjustCal);
			for (int j = 0; j < wesOfMonth.length; j++) {
				Date[] wes = wesOfMonth[j];
				if (adjustCal.getTimeInMillis() <= wes[1].getTime()) {
					ret = j;
					break;
				}
			}

			return ret;
		}
		return 0;
	}

	private String getStatusT(String status, boolean isLeader, int current, String manage_id) {
		if (status == null || "0".equals(status)) {
			if (current == 0) {
				if (isLeader) {
					return  "<input name='t_" + manage_id + "' type='radio' id='t_" + manage_id + "_cb3_m' value=1 ><label for='t_" + manage_id + "_cb3_m'>〇</label>"
							+ "<input name='t_" + manage_id + "' type='radio' id='t_" + manage_id + "_cb3_b' value=2 ><label for='t_" + manage_id + "_cb3_b'>×</label>";
				} else {
					return "<input name='t_" + manage_id + "' type='radio' id='t_" + manage_id + "_cb3_m' value=1 ><label for='t_" + manage_id + "_cb3_m'>〇</label>"
							+ "<input name='t_" + manage_id + "' type='radio' id='t_" + manage_id + "_cb3_b' value=2 lock><label for='t_" + manage_id + "_cb3_b'>×</label>";
				}
			}
			if (current > 0) {
				return "";
			}
			return "/";
		} else {
			if (isLeader && current==0) {
				if ("1".equals(status)) {
					return "<input name='t_" + manage_id + "' type='radio' id='t_" + manage_id + "_cb3_m' value=1 checked><label for='t_" + manage_id + "_cb3_m'>〇</label>"
							+ "<input name='t_" + manage_id + "' type='radio' id='t_" + manage_id + "_cb3_b' value=2 ><label for='t_" + manage_id + "_cb3_b'>×</label>";
				} else if ("2".equals(status)) {
					return "<input name='t_" + manage_id + "' type='radio' id='t_" + manage_id + "_cb3_m' value=1 ><label for='t_" + manage_id + "_cb3_m'>〇</label>"
							+ "<input name='t_" + manage_id + "' type='radio' id='t_" + manage_id + "_cb3_b' value=2 checked><label for='t_" + manage_id + "_cb3_b'>×</label>";
				} else if ("3".equals(status)) {
					return "△";
				} else if ("4".equals(status)) {
					return "●";
				}
			} else {
				if ("1".equals(status)) {
					return "〇";
				} else if ("2".equals(status)) {
					return "×";
				} else if ("3".equals(status)) {
					return "△";
				} else if ("4".equals(status)) {
					return "●";
				}
			}
		}
		return null;
	}

	private String getStatusD(String status, boolean isLeader, int current, String manage_id, String item_seq) {
		String matchKey = "d_" + manage_id + "_"+item_seq+"_" + current;

		if (status == null || "0".equals(status)) {

			if (current == 0) {
				if (isLeader) {
					return  "<input name='" + matchKey + "' type='radio' id='" + matchKey + "_cb3_m' value=1 ><label for='" + matchKey + "_cb3_m'>〇</label>"
							+ "<input name='" + matchKey + "' type='radio' id='" + matchKey + "_cb3_b' value=2 ><label for='" + matchKey + "_cb3_b'>×</label>";
				} else {
					return "<input name='" + matchKey + "' type='radio' id='" + matchKey + "_cb3_m' value=1 ><label for='" + matchKey + "_cb3_m'>〇</label>"
							+ "<input name='" + matchKey + "' type='radio' id='" + matchKey + "_cb3_b' value=2 ><label for='" + matchKey + "_cb3_b'>×</label>";
				}
			}
			if (current > 0) {
				return "";
			}
			return "/";
		} else {
			if (isLeader && current==0) {
				if ("1".equals(status)) {
					return "<input name='" + matchKey + "' type='radio' id='" + matchKey + "_cb3_m' value=1 checked><label for='" + matchKey + "_cb3_m'>〇</label>"
							+ "<input name='" + matchKey + "' type='radio' id='" + matchKey + "_cb3_b' value=2 ><label for='" + matchKey + "_cb3_b'>×</label>";
				} else if ("2".equals(status)) {
					return "<input name='" + matchKey + "' type='radio' id='" + matchKey + "_cb3_m' value=1 ><label for='" + matchKey + "_cb3_m'>〇</label>"
							+ "<input name='" + matchKey + "' type='radio' id='" + matchKey + "_cb3_b' value=2 checked><label for='" + matchKey + "_cb3_b'>×</label>";
				} else if ("3".equals(status)) {
					return "△";
				} else if ("4".equals(status)) {
					return "●";
				}
			} else {
				if ("1".equals(status)) {
					return "〇";
				} else if ("2".equals(status)) {
					return "×";
				} else if ("3".equals(status)) {
					return "△";
				} else if ("4".equals(status)) {
					return "●";
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * @param status
	 * @param digit
	 * @param isLeader
	 * @param item_seq
	 * @param current
	 * @param manage_id
	 * @param limitOfSeq
	 * @return
	 */
	private String getStatusDV(String status, String digit, boolean isLeader,
			String item_seq, int current, String manage_id, String limitOfSeq) {
		String matchKey = "d_" + manage_id + "_"+item_seq+"_" + current;

		if (status == null || "0".equals(status)) {
			if (current == 0) {
				return "<input name='" + matchKey + "' type='text' value='' " + limitOfSeq +" ovalue=''>";
			}
			if (current > 0) {
				return "";
			}
			return "/";
		} else {
			if (isLeader && current==0) {
				if (isEmpty(digit)) {
					return "<input name='" + matchKey + "' type='text' value='' " + limitOfSeq +" ovalue=''>";
//					if ("1".equals(status)) {
//						return "<input name='" + matchKey + "' type='radio' id='" + matchKey + "_cb3_m' value=1 checked><label for='" + matchKey + "_cb3_m'>〇</label>"
//								+ "<input name='" + matchKey + "' type='radio' id='" + matchKey + "_cb3_b' value=2 ><label for='" + matchKey + "_cb3_b'>×</label>";
//					} else if ("2".equals(status)) {
//						return "<input name='" + matchKey + "' type='radio' id='" + matchKey + "_cb3_m' value=1 ><label for='" + matchKey + "_cb3_m'>〇</label>"
//								+ "<input name='" + matchKey + "' type='radio' id='" + matchKey + "_cb3_b' value=2 checked><label for='" + matchKey + "_cb3_b'>×</label>";
//					} else if ("3".equals(status)) {
//						return "△";
//					} else if ("4".equals(status)) {
//						return "●";
//					}
				} else {
					// "<input name='" + matchKey + "' type='text' id='" + matchKey + "_i' "+limitOfSeq.get(sSeq)+" ovalue=''>"
					return "<input name='" + matchKey + "' type='text' value='" + getNoScale(digit) +"' " + limitOfSeq +" ovalue='"+digit+"'>";
				}
			} else {
				if (isEmpty(digit)) {
					if ("1".equals(status)) {
						return "〇";
					} else if ("2".equals(status)) {
						return "×";
					} else if ("3".equals(status)) {
						return "△";
					} else if ("4".equals(status)) {
						return "●";
					}
				} else {
					return getNoScale(digit);
				}
			}
		}
		return null;
	}

	/**
	 * 进行点检
	 * @param request
	 * @param loginData
	 * @param isLeader
	 * @param conn
	 * @throws Exception
	 */
	public void checkPoint(HttpServletRequest request, LoginData loginData,
			boolean isLeader, SqlSessionManager conn) throws Exception {
		Map<String, String[]> parameterMap = request.getParameterMap();
		List<UsageCheckForm> ucForms = new AutofillArrayList<UsageCheckForm>(UsageCheckForm.class);
		Pattern p = Pattern.compile("(\\w+).(\\w+)\\[(\\d+)\\]");
		List<UsageCheckForm> refForms = new AutofillArrayList<UsageCheckForm>(UsageCheckForm.class);
		List<CheckUnqualifiedRecordForm> curForms = new AutofillArrayList<CheckUnqualifiedRecordForm>(CheckUnqualifiedRecordForm.class);

		String section_id = request.getParameter("section_id");
		String position_id = request.getParameter("position_id");
		String check_file_manage_id = request.getParameter("check_file_manage_id");
		String object_type = request.getParameter("object_type");

		// 整理提交数据
		for (String parameterKey : parameterMap.keySet()) {
			Matcher m = p.matcher(parameterKey);
			if (m.find()) {
				String entity = m.group(1);
				if ("submit".equals(entity)) {
					String column = m.group(2);
					int icounts = Integer.parseInt(m.group(3));
					String[] value = parameterMap.get(parameterKey);

					// ODO 全
					if ("manage_id".equals(column)) {
						ucForms.get(icounts).setManage_id(value[0]);
					} else if ("status".equals(column)) {
						ucForms.get(icounts).setChecked_status(value[0]);
					} else if ("item_seq".equals(column)) {
						ucForms.get(icounts).setItem_seq(value[0]);
					} else if ("digit".equals(column)) {
						ucForms.get(icounts).setDigit(value[0]);
					}
				} else if ("unqualified".equals(entity)) {
					// if (!isLeader) continue;
					String column = m.group(2);
					int icounts = Integer.parseInt(m.group(3));
					String[] value = parameterMap.get(parameterKey);

					// ODO 全
					if ("manage_id".equals(column)) {
						curForms.get(icounts).setManage_id(value[0]);
					} else if ("object_type".equals(column)) {
						curForms.get(icounts).setObject_type(value[0]);
					} else if ("check_item".equals(column)) {
						curForms.get(icounts).setCheck_item(value[0]);
					} else if ("unqualified_status".equals(column)) {
						curForms.get(icounts).setUnqualified_status(value[0]);
					}
				} else if ("refer".equals(entity)) {
					String column = m.group(2);
					int icounts = Integer.parseInt(m.group(3));
					String[] value = parameterMap.get(parameterKey);

					// ODO 全
					if ("manage_id".equals(column)) {
						refForms.get(icounts).setManage_id(value[0]);
					} else if ("item_seq".equals(column)) {
						refForms.get(icounts).setItem_seq(value[0]);
					} else if ("digit".equals(column)) {
						refForms.get(icounts).setDigit(value[0]);
					}
				}
			}
		}

		CheckResultMapper mapper = conn.getMapper(CheckResultMapper.class);
		String blockID = "";
		String returnMessage = "";
		for (UsageCheckForm ucForm : ucForms) {
			if (isEmpty(check_file_manage_id)) {
				// 治具
				CheckResultEntity entity = new CheckResultEntity();
				BeanUtil.copyToBean(ucForm, entity, CopyOptions.COPYOPTIONS_NOEMPTY);
				entity.setCheck_file_manage_id("00000000000");
				entity.setItem_seq("00");
				entity.setOperator_id(loginData.getOperator_id());
				entity.setPosition_id(position_id);
				entity.setSection_id(section_id);
				// 发布中断
				if (2 == entity.getChecked_status() || 3 == entity.getChecked_status()) {
					blockID = entity.getManage_id();
				} else if (4 == entity.getChecked_status() && returnMessage.length() == 0) {
					// 通知设备管理员
					returnMessage = "在" + loginData.getSection_name() + request.getParameter("process_code") + "由线长提出了治具不需使用的要求";
					PostMessageMapper pmMapper = conn.getMapper(PostMessageMapper.class);
					PostMessageEntity pmEntity = new PostMessageEntity();
					pmEntity.setContent(returnMessage);
					pmEntity.setSender_id(loginData.getOperator_id());
					pmEntity.setLevel(1);
					pmEntity.setReason(PostMessageService.INFECT_ERROR_CONFIRMED);
					pmMapper.createPostMessage(pmEntity);
					
					CommonMapper commonMapper = conn.getMapper(CommonMapper.class);
					String lastInsertID = commonMapper.getLastInsertID();
					pmEntity.setPost_message_id(lastInsertID);

					// 找到设备管理员
					OperatorMapper oMapper = conn.getMapper(OperatorMapper.class);
					OperatorEntity cond_operator = new OperatorEntity();
					cond_operator.setRole_id(RvsConsts.ROLE_DT_MANAGER);
					List<OperatorNamedEntity> dtManagers = oMapper.searchOperator(cond_operator);
					for (OperatorNamedEntity dtManager : dtManagers) {
						pmEntity.setReceiver_id(dtManager.getOperator_id());
						pmMapper.createPostMessageSendation(pmEntity);
					}
				}
				mapper.insertToolCheck(entity);
			} else {
				// 设备
				CheckResultEntity entity = new CheckResultEntity();
				BeanUtil.copyToBean(ucForm, entity, CopyOptions.COPYOPTIONS_NOEMPTY);
				entity.setOperator_id(loginData.getOperator_id());
				entity.setPosition_id(position_id);
				entity.setSection_id(section_id);
				entity.setCheck_file_manage_id(check_file_manage_id);

				// 插入点检信息
				mapper.insertDeviceCheck(entity);
				// 删除等待点检信息
				mapper.removeWaitDeviceCheck(entity);

				if (entity.getChecked_status() == null) {
					_logger.error(entity.getManage_id() + "BLK by" + entity.getManage_id() + check_file_manage_id);
				} else if (2 == entity.getChecked_status() || 3 == entity.getChecked_status()) {
					blockID = entity.getManage_id();
				}
			}
		}

		for (UsageCheckForm refForm : refForms) {
			CheckResultEntity entity = new CheckResultEntity();
			entity.setCheck_file_manage_id(check_file_manage_id);
			BeanUtil.copyToBean(refForm, entity, CopyOptions.COPYOPTIONS_NOEMPTY);
			mapper.insertDeviceCheckRefer(entity);
		}

		// 产生中断
		String amId = null;
		if (blockID.length() > 0) {
			AlarmMesssageService amService = new AlarmMesssageService();
			String manager_operator_id = "";
			AlarmMesssageEntity amEntity = new AlarmMesssageEntity();
			amEntity.setLevel(1);
			amEntity.setMaterial_id("0000000000");
			amEntity.setOperator_id(loginData.getOperator_id());
			amEntity.setReason(RvsConsts.WARNING_REASON_INFECT_ERROR);

			if (isEmpty(check_file_manage_id)) {
				// 治具
				JigManageMapper tmMapper = conn.getMapper(JigManageMapper.class);
				JigManageEntity smlEntity = tmMapper.getByKey(blockID);
	
				amEntity.setSection_id(smlEntity.getSection_id());
				amEntity.setLine_id(smlEntity.getLine_id());
				amEntity.setPosition_id(smlEntity.getPosition_id());
				manager_operator_id = smlEntity.getManager_operator_id();
			} else {
				// 设备
				DevicesManageMapper dmMapper = conn.getMapper(DevicesManageMapper.class);
				DevicesManageEntity smlEntity = dmMapper.getByKey(blockID);

				amEntity.setSection_id(smlEntity.getSection_id());
				amEntity.setLine_id(smlEntity.getLine_id());
				amEntity.setPosition_id(smlEntity.getPosition_id());
				manager_operator_id = smlEntity.getManager_operator_id();
			}
			if (!isEmpty(manager_operator_id))
				amId = amService.createInfectAlarmMessage(amEntity, manager_operator_id, conn);
		}

		// 建立不合格记录
		if (curForms.size() > 0) {
			CheckUnqualifiedRecordMapper curMapper = conn.getMapper(CheckUnqualifiedRecordMapper.class);
			JigManageMapper tmMapper = conn.getMapper(JigManageMapper.class);
			DevicesManageMapper dmMapper = conn.getMapper(DevicesManageMapper.class);
			String sManageNosMessage = "";
			for (CheckUnqualifiedRecordForm curForm : curForms) {
				CheckUnqualifiedRecordEntity entity = new CheckUnqualifiedRecordEntity();
				entity.setLine_leader_id(loginData.getOperator_id());
				BeanUtil.copyToBean(curForm, entity, CopyOptions.COPYOPTIONS_NOEMPTY);
				// 找到责任者
				if (isEmpty(check_file_manage_id)) {
					JigManageEntity tmentity = tmMapper.getByKey(entity.getManage_id());
					if (tmentity != null) {
						entity.setResponsible_operator_id(tmentity.getResponsible_operator_id());
						sManageNosMessage += tmentity.getManage_code() + "_n_";
					}
				} else {
					entity.setResponsible_operator_id(loginData.getOperator_id()); // TODO 管理员以外最后的点检者
					entity.setObject_type(Integer.parseInt(object_type));
					DevicesManageEntity dmentity = dmMapper.getByKey(entity.getManage_id());
					if (dmentity != null) {
						sManageNosMessage += dmentity.getManage_code() + "_n_";
					}
				}
				
				entity.setAlarm_message_id(amId);
				curMapper.create(entity);
			}
			PostMessageMapper pmMapper = conn.getMapper(PostMessageMapper.class);
			PostMessageEntity pmEntity = new PostMessageEntity();
			pmEntity.setContent("在" + loginData.getSection_name() + request.getParameter("process_code") + "提出了点检不合格");
			pmEntity.setSender_id(loginData.getOperator_id());
			pmEntity.setLevel(1);
			pmEntity.setReason(PostMessageService.INFECT_ERROR_CONFIRMED);
			pmMapper.createPostMessage(pmEntity);
			
			CommonMapper commonMapper = conn.getMapper(CommonMapper.class);
			String lastInsertID = commonMapper.getLastInsertID();
			pmEntity.setPost_message_id(lastInsertID);

			// 找到设备管理员
			OperatorMapper oMapper = conn.getMapper(OperatorMapper.class);
			OperatorEntity cond_operator = new OperatorEntity();
			cond_operator.setRole_id(RvsConsts.ROLE_DT_MANAGER);
			List<OperatorNamedEntity> dtManagers = oMapper.searchOperator(cond_operator);
			for (OperatorNamedEntity dtManager : dtManagers) {
				pmEntity.setReceiver_id(dtManager.getOperator_id());
				pmMapper.createPostMessageSendation(pmEntity);
			}

			// 发送邮件到设备管理员
			if (sManageNosMessage.length() > 0) {
				HttpAsyncClient httpclient = new DefaultHttpAsyncClient();
				httpclient.start();
				try {
					HttpGet getRequest = new HttpGet("http://localhost:8080/rvspush/trigger/breakToTec/" + sManageNosMessage
							+ "/" + section_id + "/" + position_id);
					_logger.info("finger:" + getRequest.getURI());
					httpclient.execute(getRequest, null);
				} catch (Exception e) {
					_logger.info(e.getMessage(), e);
				} finally {
					Thread.sleep(100);
					httpclient.shutdown();
				}
			}
		} 

		// 上级管理者确认
		if (isLeader) {
			String upperCheck = request.getParameter("upper_check");
			if (!isEmpty(upperCheck)) {
				
				CheckResultEntity entity = new CheckResultEntity();
				entity.setOperator_id(loginData.getOperator_id());

				if (isEmpty(check_file_manage_id)) {
					entity.setPosition_id(position_id);
					entity.setSection_id(section_id);
					mapper.setJigUpperConfirm(entity);
				} else {
					String manage_id = upperCheck;
					entity.setManage_id(manage_id);
					entity.setCheck_file_manage_id(check_file_manage_id);
					mapper.setDeviceUpperConfirm(entity);
				}
			}
		}
	}

	public void refix(String position_id, LoginData loginData,
			SqlSessionManager conn) throws Exception {
		AlarmMesssageMapper amDao = conn.getMapper(AlarmMesssageMapper.class);

		List<String> alarm_messsage_ids = amDao.getToolInfectByPosition(loginData.getSection_id(), position_id);
		
		for (String alarm_messsage_id : alarm_messsage_ids) {
			// 处理人处理信息
			AlarmMesssageSendationEntity sendation = new AlarmMesssageSendationEntity();
			sendation.setAlarm_messsage_id(alarm_messsage_id);
			sendation.setComment("点检不合格解除");
			sendation.setRed_flg(1);
			sendation.setSendation_id(loginData.getOperator_id());
			sendation.setResolve_time(new Date());

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
	 * 取得设备的点检表模板
	 * @param manage_id
	 * @param check_file_manage_id
	 * @param operator_id
	 * @param isLeader
	 * @param adjustDate
	 * @param conn
	 * @return
	 */
	public String getDeviceCheckSheet(String manage_id, String check_file_manage_id, String operator_id,
			boolean isLeader, Date adjustDate, SqlSession conn) {
		String retContent = "";

		BufferedReader input = null;

		// 当日
		Calendar adjustCal = Calendar.getInstance();
		adjustCal.setTime(adjustDate);
		adjustCal.set(Calendar.HOUR_OF_DAY, 0);
		adjustCal.set(Calendar.MINUTE, 0);
		adjustCal.set(Calendar.SECOND, 0);
		adjustCal.set(Calendar.MILLISECOND, 0);
		Date today = adjustCal.getTime();

		// 取得本期
		String bperiod = RvsUtils.getBussinessYearString(adjustCal);

		// 取得点检表信息
		CheckFileManageMapper cfmMapper = conn.getMapper(CheckFileManageMapper.class);
		CheckFileManageEntity cfmEntity = cfmMapper.getByKey(check_file_manage_id);
		// 取得点检项目信息

		// 取得管理设备信息
		DevicesManageMapper dmMapper = conn.getMapper(DevicesManageMapper.class);
		DevicesManageEntity dmEntity = dmMapper.getByKey(manage_id);

		CheckResultMapper crMapper = conn.getMapper(CheckResultMapper.class);

		try {
			input = new BufferedReader(new InputStreamReader(new FileInputStream(PathConsts.BASE_PATH + "\\DeviceInfection\\xml\\"
					+ cfmEntity.getCheck_manage_code() + ".html"),"UTF-8"));
			StringBuffer buffer = new StringBuffer();
			String text;

			while ((text = input.readLine()) != null)
				buffer.append(text);

			String content = buffer.toString();

			// 工程
			if (content.indexOf("<line/>") >= 0 || content.indexOf("<position/>") >= 0) {
				String sLine = "";
				SectionMapper sMapper = conn.getMapper(SectionMapper.class);
				SectionEntity sEntity = sMapper.getSectionByID(dmEntity.getSection_id());
				if (sEntity != null) {
					sLine += sEntity.getName() + "<br/>";
				}
				if (content.indexOf("<line/>") >= 0) {
					LineMapper lMapper = conn.getMapper(LineMapper.class);
					LineEntity lEntity = lMapper.getLineByID(dmEntity.getLine_id());
					if (lEntity != null) {
						sLine += lEntity.getName();
					}
					content = content.replaceAll("<line/>" , sLine);
				}
				if (content.indexOf("<position/>") >= 0) {
					PositionMapper pMapper = conn.getMapper(PositionMapper.class);
					// 工位
					PositionEntity pEntity = pMapper.getPositionByID(dmEntity.getPosition_id());
					if (pEntity != null) {
						sLine += pEntity.getProcess_code() + " ";
					}
					content = content.replaceAll("<position/>" , sLine);
				}
			}

			// 替换共通数据
			content = content.replaceAll("<manageNo/>", dmEntity.getManage_code() 
					+ "<dtag manage_code='"+dmEntity.getManage_code()+"' manage_id='"+dmEntity.getDevices_manage_id()+"'"
							+ " model_name='"+ dmEntity.getModel_name() +"'"
							+ " device_name='"+ dmEntity.getName() +"'>");
			content = content.replaceAll("<manageNo replacable/>", dmEntity.getManage_code() 
					+ "<br/><input type='button' value='替换新品' class='ui-button manage_replace'/><dtag manage_code='"+dmEntity.getManage_code()+"' manage_id='"+dmEntity.getDevices_manage_id()+"'"
							+ " model_name='"+ dmEntity.getModel_name() +"'"
							+ " device_name='"+ dmEntity.getName() +"'>");
			content = content.replaceAll("<model/>", dmEntity.getModel_name());
			content = content.replaceAll("<period type='full'/>", bperiod);
			content = content.replaceAll("<period type='num'/>", bperiod.replaceAll("P", ""));

			if (content.indexOf("<useStart/>") >= 0) {
				Calendar calUse = Calendar.getInstance();
				Calendar calThreeMonthBefore = Calendar.getInstance();
				calThreeMonthBefore.setTimeInMillis(adjustCal.getTimeInMillis());
				calThreeMonthBefore.add(Calendar.MONTH, 3);
//				calThreeMonthBefore.add(Calendar.DATE, -1);

				if (dmEntity.getImport_date()!=null) {
					calUse.setTime(dmEntity.getImport_date());
				}
				content = content.replaceAll("<useStart/>", DateUtil.toString(dmEntity.getImport_date(), "yyyy年 M月 d日"));

				int posUseEnd = content.indexOf("<useEnd");
				if (content.indexOf("<useEnd") >= 0) {
					int period = 3;
					// 有效区间
					for (int i = 0; i < content.length(); i++) {
						if (content.charAt(posUseEnd + i) == '>') {
							String useEndTag = content.substring(posUseEnd, posUseEnd + i + 1);
							String sPeriod = useEndTag.replaceAll("<useEnd period='(\\d+)'/>", "$1");
							period = Integer.parseInt(sPeriod);
							break;
						}
					}

					calUse.add(Calendar.MONTH, period);
					calUse.add(Calendar.DATE, -1);

					String useEndTag = "";
					if (calUse.before(adjustCal)) {
						useEndTag = "<span class='useEnd' expire='1'>" + DateUtil.toString(calUse.getTime(), "yyyy年 M月 d日") + "</span>";
					} else if (calUse.before(calThreeMonthBefore)) {
						useEndTag = "<span class='useEnd' expire='0'>" + DateUtil.toString(calUse.getTime(), "yyyy年 M月 d日") + "</span>";
					} else {
						useEndTag = "<span class='useEnd' expire='-1'>" + DateUtil.toString(calUse.getTime(), "yyyy年 M月 d日") + "</span>";
					}
					// 比较
					content = content.replaceAll("<useEnd/>", useEndTag);
					content = content.replaceAll("<useEnd period='\\d+'/>", useEndTag);
				}
			}

			if (dmEntity.getName() != null) {
				content = content.replaceAll("<name/>", dmEntity.getName());
			}

			// 要返回的页面内容
			retContent = content;

			// 确定表单的归档类型
			int fileAxisType = 0; 
			fileAxisType = cfmEntity.getCycle_type();

			// 计算范围用日历
			Calendar monCal = Calendar.getInstance();
			if (CheckFileManageEntity.ACCESS_PLACE_DAILY == cfmEntity.getAccess_place()) {
				monCal.setTimeInMillis(adjustCal.getTimeInMillis());
				monCal.set(Calendar.DATE, 1);
			} else {
				if (TYPE_FILED_YEAR == fileAxisType) {
					// 去期间头
					monCal = getStartOfPeriod(adjustCal);
				} else {
					// 去月首
					monCal.setTimeInMillis(adjustCal.getTimeInMillis());
					monCal.set(Calendar.DATE, 1);

					if (TYPE_FILED_WEEK_OF_MONTH == fileAxisType) {
						int week = monCal.get(Calendar.DAY_OF_WEEK);
						if (week == Calendar.SUNDAY) {
							monCal.add(Calendar.DATE, 1);
						} else if (week == Calendar.MONDAY) {
						} else {
							monCal.add(Calendar.DATE, 9 - week);
						}
						if (today.before(monCal.getTime())) {
							monCal.add(Calendar.MONTH, -1);
							week = monCal.get(Calendar.DAY_OF_WEEK);
							if (week == Calendar.SUNDAY) {
								monCal.add(Calendar.DATE, 1);
							} else if (week == Calendar.MONDAY) {
							} else {
								monCal.add(Calendar.DATE, 9 - week);
							}
						}
					}
				}
			}
			retContent = retContent.replaceAll("<date type='year'/>", DateUtil.toString(monCal.getTime(), "yyyy"));
			retContent = retContent.replaceAll("<date type='month'/>", DateUtil.toString(monCal.getTime(), "M"));

			// 全归档期间的开始终了时间
			Date startPDate = new Date();
			Date endPDate = new Date();

			// 取得点检项目
			List<DeviceCheckItemEntity> dCsis = cfmMapper.getSeqItemsByFile(check_file_manage_id);

			// 填写项目附加数据
			Map<String, String> limitOfSeq = new HashMap<String, String>();

			// 各周期下的员工签章与日期 周期 -> 坐标 -> 值
			Map<Integer, Map<Integer, Date>> cycleTypeTime = new HashMap<Integer, Map<Integer, Date>>();
			Map<Integer, Map<Integer, String>> cycleTypeJobNo = new HashMap<Integer, Map<Integer, String>>();

			for (DeviceCheckItemEntity item : dCsis) {
				// 判断型号
				if (!isEmpty(item.getSpecified_model_name())) {
					if (isEmpty(dmEntity.getModel_name())) {
						continue;
					}
					if (item.getSpecified_model_name().indexOf(dmEntity.getModel_name()) < 0) {
						continue;
					}
				}

				int itemAxisType = item.getCycle_type();
				int axis = getAxis(adjustCal, itemAxisType, cfmEntity.getCycle_type());

				// 每行每个单元格取值
				for (int iAxis=0;iAxis<=axis;iAxis++) {

					Date[] dates = getDayOfAxis(monCal, iAxis, itemAxisType, cfmEntity.getCycle_type());
					if (iAxis==0) startPDate = dates[0]; 
					endPDate = dates[1];

					CheckResultEntity cre = new CheckResultEntity();
					cre.setCheck_confirm_time_start(dates[0]);
					cre.setCheck_confirm_time_end(dates[1]);
					cre.setManage_id(manage_id);
					cre.setCheck_file_manage_id(check_file_manage_id);
					cre.setItem_seq(item.getItem_seq());

					// 已点检或之前范围
					List<CheckResultEntity> listCre = crMapper.getDeviceCheckInPeriod(cre);

					if (!cycleTypeTime.containsKey(itemAxisType)) {
						cycleTypeTime.put(itemAxisType, new HashMap<Integer, Date>());
					}
					if (!cycleTypeJobNo.containsKey(itemAxisType)) {
						cycleTypeJobNo.put(itemAxisType, new HashMap<Integer, String>());
					}

					Map<Integer, Date> axisTime = cycleTypeTime.get(itemAxisType);
					Map<Integer, String> axisJobNo = cycleTypeJobNo.get(itemAxisType);

					if (axisTime.get(iAxis) == null) {
						axisTime.put(iAxis, new Date(0));
					}

					// 取得已点检单元格信息
					if (listCre != null && listCre.size() > 0) {
						for (CheckResultEntity rCre : listCre) {
							String itemSeq = rCre.getItem_seq();
							boolean current = (iAxis==axis);
							int shift = (iAxis+1);

							if (rCre.getChecked_status() == 0) {
								// 未点检并且有限制
								if (iAxis==axis) {
									retContent = setTodayUncheck(iAxis, item, itemAxisType, retContent, dmEntity, limitOfSeq, true);
								} else {
									// 剩余未点检数据划掉
									retContent = retContent.replaceAll("<point type='\\w+' item_seq='" + item.getItem_seq() + "' cycle_type='"
										+ itemAxisType + "' ([^>]*)?shift='" + (iAxis + 1) + "'/>", "/");
								}
							} else {

								retContent = retContent.replaceAll("<point type='check' item_seq='"+itemSeq+"' cycle_type='\\d+' (model_relative='[^']*' )?tab='\\d+' shift='"+shift+"'/>"
										, getStatusD(""+rCre.getChecked_status(), isLeader, (current ? 0 : -1), manage_id, itemSeq));
								String limits = "";
								Pattern pInputData = Pattern.compile("<point type='number' item_seq='("+itemSeq+")' cycle_type='\\d+' "
										+ "(model_relative='[^']*' )?(upper_limit='\\-?[\\d\\.]+' )?(lower_limit='\\-?[\\d\\.]+' )?"
										+ "(refer_upper_from='[^']*' )?(refer_lower_from='[^']*' )?tab='\\d+' shift='"+ shift +"'/>");
								Matcher mInputData = pInputData.matcher(retContent);
								if (mInputData.find()) {
									if (!limitOfSeq.containsKey(itemSeq)) {
										getOfSeq(limitOfSeq, itemSeq, mInputData);
									}
									limits = limitOfSeq.get(itemSeq);
									retContent = retContent.replaceAll(mInputData.group()
											, getStatusDV(""+rCre.getChecked_status(), (rCre.getDigit()== null? "" : getNoScale(rCre.getDigit().toPlainString())), isLeader, itemSeq, (current ? 0 : -1), manage_id, limits));
								}
								if (axisJobNo.get(iAxis) == null) {
									axisJobNo.put(iAxis, rCre.getJob_no());
								}
								// 得到点检最终完成时间
								Date dCheckConfirmTime = rCre.getCheck_confirm_time();
								if (dCheckConfirmTime.after(axisTime.get(iAxis))) {
									axisTime.put(iAxis, dCheckConfirmTime);
								}
							}
						}
					}

					// 当前范围未点检/且不限制
					if (iAxis==axis) {
						retContent = setTodayUncheck(iAxis, item, itemAxisType, retContent, dmEntity, limitOfSeq, false);
					} else {
						// 剩余未点检数据划掉
						retContent = retContent.replaceAll("<point type='\\w+' item_seq='" + item.getItem_seq() + "' cycle_type='"
							+ itemAxisType + "' ([^>]*)?shift='" + (iAxis + 1) + "'/>", "/");
					}
				}
			}

			// 点检者名字
			for (Integer cycleType : cycleTypeJobNo.keySet()) {
				Map<Integer, String> axisJobNo = cycleTypeJobNo.get(cycleType);
				Map<Integer, Date> axisTime = cycleTypeTime.get(cycleType);

				for (Integer iAxis : axisJobNo.keySet()) {
					String jobNo = axisJobNo.get(iAxis);
					Date checkedDate = axisTime.get(iAxis);

					Pattern pInputData = Pattern.compile("<confirm type='responser' cycle_type='" + cycleType + "' tab='\\d+' (model_relative='[^']*' )?shift='"+(iAxis+1)+"' st='(.{4})'/>");
					Matcher mInputData = pInputData.matcher(retContent);
					while (mInputData.find()) {
						if (mInputData.group(1) != null && mInputData.group(1).startsWith("model_relative")) {
							if (mInputData.group(1).indexOf(dmEntity.getModel_name()) < 0) {
								retContent = retContent.replaceAll(mInputData.group(), "");
								continue;
							}
						}
						String direction = mInputData.group(2);
						if ("vert".equals(direction)) {
							retContent = retContent.replaceAll(mInputData.group(0), "<img src='/images/sign_v/"+jobNo+"' class='sign_v'>");
						} else if ("hori".equals(direction)) {
							retContent = retContent.replaceAll(mInputData.group(0), "<img src='/images/sign/"+jobNo+"'>");
						}
					}

					retContent = retContent.replaceAll("<cdate type='responser' data_type='1' cycle_type='" + cycleType + "' tab='\\d+' shift='"+(iAxis+1)+"'/>", DateUtil.toString(checkedDate, "M-d"));
					retContent = retContent.replaceAll("<cdate type='responser' data_type='2' cycle_type='" + cycleType + "' tab='\\d+' shift='"+(iAxis+1)+"'/>", bperiod + DateUtil.toString(checkedDate, " M月d日"));
					retContent = retContent.replaceAll("<cdate type='responser' data_type='3' cycle_type='" + cycleType + "' tab='\\d+' shift='"+(iAxis+1)+"'/>", DateUtil.toString(checkedDate, "yyyy年M月d日"));
				}
			}

			// 取得点检表的线长确认周期
			Integer signCycle = null;
			// 从文件里取得线长填写周期吧
			Pattern lReferData = Pattern.compile("<confirm type='leader' cycle_type='(\\d)' tab='\\d+' shift='\\d+' st='.{4}'/>");
			Matcher mLReferData = lReferData.matcher(retContent);
			if (mLReferData.find()) {
				String sSignCycle = mLReferData.group(1);
				if (sSignCycle != null) {
					signCycle = Integer.parseInt(sSignCycle);
				}
			}

			if (signCycle != null) {
				CheckResultEntity cre = new CheckResultEntity();
				cre.setCheck_confirm_time_start(startPDate);
				cre.setCheck_confirm_time_end(endPDate);
				cre.setCheck_file_manage_id(check_file_manage_id);
				cre.setManage_id(manage_id);

				retContent = setLeaderCheck(cre, crMapper, monCal, adjustCal, signCycle, cfmEntity.getCycle_type(), retContent, bperiod, isLeader);
			}

			// 取得参照信息<refer
			if (retContent.indexOf("<refer type='input'") >= 0 || retContent.indexOf("<refer type='choose'") >= 0) {
				CheckResultEntity cre = new CheckResultEntity();
				cre.setCheck_confirm_time_start(startPDate);
				cre.setCheck_confirm_time_end(endPDate);
				cre.setCheck_file_manage_id(check_file_manage_id);
				cre.setManage_id(manage_id);

				// 取得设备的温度设置
				String lowerLimit = "70";
				String upperLimit = "75";
				DryingOvenDeviceMapper dodMapper = conn.getMapper(DryingOvenDeviceMapper.class);
				DryingOvenDeviceEntity dodCnd = new DryingOvenDeviceEntity();
				dodCnd.setDevice_manage_id(manage_id);
				List<DryingOvenDeviceEntity> dodRet = dodMapper.search(dodCnd);
				if (dodRet.size() > 0) {
					String settingTemperature = "" + dodRet.get(0).getSetting_temperature();
					lowerLimit = CodeListUtils.getValue("drying_oven_lower_limit", settingTemperature);
					upperLimit = CodeListUtils.getValue("drying_oven_upper_limit", settingTemperature);
				}

				Pattern pReferData = Pattern.compile("<refer type='choose' lower_limit='(\\-?[\\d\\.]+)' upper_limit='(\\-?[\\d\\.]+)'/>");
				Matcher mReferData = pReferData.matcher(retContent);
				while (mReferData.find()) {
					String setLowerLimit = mReferData.group(1);
					String setUpperLimit = mReferData.group(2);

					if (lowerLimit.equals(setLowerLimit) && upperLimit.equals(setUpperLimit)) {
						retContent = retContent.replaceAll(mReferData.group(0),
								mReferData.group(0).replaceAll("<refer type='choose'", "<refer type='choosed'") + "■");
						break;
					}
				}
				retContent = retContent.replaceAll(pReferData.pattern(), "□");
			}

			// 备注
			CheckResultEntity commentCondition = new CheckResultEntity();
			commentCondition.setCheck_confirm_time_start(monCal.getTime());
			commentCondition.setCheck_confirm_time_end(endPDate);
			Set<String> manageIds = new HashSet<String>();
			manageIds.add(manage_id);
			commentCondition.setManage_ids(manageIds);
			List<String> commentExists = getCommentsExists(1, commentCondition, crMapper);
			if (commentExists.size() > 0) {
				retContent += "<comment exists/>";
			}

//			if (mSignData.find()) {
//				String ss = mSignData.group();
//				String shift = mSignData.group(1);
//				retContent = retContent.replaceAll(ss, shift);
//			}
			// 

			// 清除多余
			// retContent = retContent.replaceAll("#[^#]*#", "");

		} catch (IOException ioException) {
			_logger.error(ioException.getMessage(), ioException);
		} finally {
			try {
				if (input != null) input.close();
			} catch (IOException e) {
			}
			input = null;
		}
		return retContent;
	}

	/**
	 * 
	 * @param cre 查询条件
	 * @param crMapper
	 * @param monCal 文档开始时间
	 * @param adjustCal 当前时间
	 * @param signCycle 线长填写周期
	 * @param fileType 文档归档类型
	 * @param retContent 点检表内容
	 * @param bperiod 本期文字
	 * @param isLeader 是否管理者
	 * @return
	 */
	private String setLeaderCheck(CheckResultEntity cre, CheckResultMapper crMapper, Calendar monCal,
			Calendar adjustCal, Integer signCycle, Integer fileType,
			String retContent, String bperiod, boolean isLeader) {
		List<CheckResultEntity> lUppers = crMapper.getDeviceUpperStamp(cre);

		int axis = getAxis(adjustCal, signCycle, fileType);
		for (int iAxis=0;iAxis<=axis;iAxis++) {
			Date[] dates = getDayOfAxis(monCal, iAxis, signCycle, fileType);
			Calendar endCal = Calendar.getInstance();
			endCal.setTime(dates[1]);
			endCal.add(Calendar.DATE, 1);
			boolean hit = false;
			for (CheckResultEntity lUpper : lUppers) {
				Date checkedDate = lUpper.getCheck_confirm_time();
				if (checkedDate.compareTo(dates[0]) >= 0
						&& checkedDate.compareTo(endCal.getTime()) < 0) {
					String jobNo = lUpper.getJob_no();
					hit = true;
					retContent = retContent.replaceAll("<confirm type='leader' cycle_type='" + signCycle + "' tab='\\d+' shift='"+(iAxis+1)+"' st='vert'/>", "<img src='/images/sign_v/"+jobNo+"' class='sign_v'>");
					retContent = retContent.replaceAll("<confirm type='leader' cycle_type='" + signCycle + "' tab='\\d+' shift='"+(iAxis+1)+"' st='hori'/>", "<img src='/images/sign/"+jobNo+"'>");

					retContent = retContent.replaceAll("<cdate type='leader' data_type='1' cycle_type='" + signCycle + "' tab='\\d+' shift='"+(iAxis+1)+"'/>", DateUtil.toString(checkedDate, "M-d"));
					retContent = retContent.replaceAll("<cdate type='leader' data_type='2' cycle_type='" + signCycle + "' tab='\\d+' shift='"+(iAxis+1)+"'/>", bperiod + DateUtil.toString(checkedDate, " M月d日"));
					retContent = retContent.replaceAll("<cdate type='leader' data_type='3' cycle_type='" + signCycle + "' tab='\\d+' shift='"+(iAxis+1)+"'/>", DateUtil.toString(checkedDate, "yyyy年M月d日"));
					break;
				}
			}
			if (!hit) {
				if (iAxis==axis && isLeader) {
					retContent = retContent.replaceAll("<confirm type='leader' cycle_type='" + signCycle + "' tab='\\d+' shift='"+(iAxis+1)+"' st='.{4}'/>", "<input type='checkbox' id='upper_check' value='上级确认'/><label for='upper_check'>上级确认</label>");
				} else {
					retContent = retContent.replaceAll("<confirm type='leader' cycle_type='" + signCycle + "' tab='\\d+' shift='"+(iAxis+1)+"' st='.{4}'/>", "/");
				}
				retContent = retContent.replaceAll("<cdate type='leader' data_type='\\d' cycle_type='" + signCycle + "' tab='\\d+' shift='"+(iAxis+1)+"'/>", "/");
			}
		}
		return retContent;
	}

	/**
	 * 显示当时需点检项目
	 * @param lock 
	 * @return 
	 */
	private String setTodayUncheck(int iAxis, DeviceCheckItemEntity item, int itemAxisType, String retContent, DevicesManageEntity dmEntity,
			Map<String, String> limitOfSeq, boolean lock ) {

		int shift = (iAxis+1);

		// refer_upper_from='02' refer_lower_from='01' 
		Pattern pInputData = Pattern.compile("<point type='number' item_seq='(" + item.getItem_seq() + ")' cycle_type='"+itemAxisType+"' "
				+ "(model_relative='[^']*' )?(upper_limit='\\-?[\\d\\.]+' )?(lower_limit='\\-?[\\d\\.]+' )?"
				+ "(refer_upper_from='[^']*' )?(refer_lower_from='[^']*' )?tab='\\d+' shift='"+shift+"'/>");
		Matcher mInputData = pInputData.matcher(retContent);
		// 填写数据
		while (mInputData.find()) {
			String sSeq = mInputData.group(1);
			if (!isEmpty(mInputData.group(2))) {
				String referModel = mInputData.group(2);
				if (referModel.indexOf(dmEntity.getModel_name()) < 0) {
					retContent = retContent.replaceAll(mInputData.group(), "/");
					continue;
				}
			}
			// 整理附加数据
			if (!limitOfSeq.containsKey(sSeq)) {
				getOfSeq(limitOfSeq, sSeq, mInputData);
			}
			String matchKey = "d_" + dmEntity.getDevices_manage_id() + "_"+sSeq+"_" + iAxis;
			retContent = retContent.replaceAll(mInputData.group(),
					"<input name='" + matchKey + "' type='text' id='" + matchKey + "_i' "+limitOfSeq.get(sSeq)+" ovalue=''" + (lock ? " lock" : "") + ">");
		}
		// 选择合格项
		pInputData = Pattern.compile("<point type='check' item_seq='(" + item.getItem_seq() + ")' cycle_type='"+itemAxisType+"' (model_relative='[^']*' )?tab='\\d+' shift='"+shift+"'/>");
		mInputData = pInputData.matcher(retContent);
		while (mInputData.find()) {
			if (!isEmpty(mInputData.group(2))) {
				String referModel = mInputData.group(2);
				if (referModel.indexOf(dmEntity.getModel_name()) < 0) {
					retContent = retContent.replaceAll(mInputData.group(), "/");
					continue;
				}
			}
			String matchKey = "d_" + dmEntity.getDevices_manage_id() + "_"+mInputData.group(1)+"_" + iAxis;
			retContent = retContent.replaceAll(mInputData.group(),
					"<input name='" + matchKey + "' type='radio' id='" + matchKey + "_cb3_m' value=1><label for='" + matchKey + "_cb3_m'>〇</label>"
					+ "<input name='" + matchKey + "' type='radio' id='" + matchKey + "_cb3_b' value=2" + (lock ? " lock" : "") + "><label for='" + matchKey + "_cb3_b'>×</label>");
		}

		return retContent;
	}

	/**
	 * 力矩工具表单
	 * @param section_id
	 * @param position_id
	 * @param check_file_manage_id
	 * @param operator_id
	 * @param isLeader
	 * @param adjustDate
	 * @param conn
	 * @return
	 */
	public String getTorsionDeviceCheckSheet(String section_id, String position_id,
			String check_file_manage_id, String operator_id, boolean isLeader,
			Date adjustDate, SqlSession conn) {
		CheckResultMapper crMapper = conn.getMapper(CheckResultMapper.class);
		TorsionDeviceMapper tdMapper = conn.getMapper(TorsionDeviceMapper.class);

		String retContent = "";

		// 取得点检表信息
		CheckFileManageMapper cfmMapper = conn.getMapper(CheckFileManageMapper.class);
		CheckFileManageEntity cfmEntity = cfmMapper.getByKey(check_file_manage_id);

		BufferedReader input = null;
		StringBuffer checkContent = new StringBuffer("<style>.row_check_1 td:nth-child(n+1){border:1px solid;border-top:2px solid;}.row_date td{border:1px solid;}.row_operator td{border:1px solid;}.row_check_more td{border:1px solid;}</style>");

		Calendar adjustCal = Calendar.getInstance();
		adjustCal.setTime(adjustDate);
		adjustCal.set(Calendar.HOUR_OF_DAY, 0);
		adjustCal.set(Calendar.MINUTE, 0);
		adjustCal.set(Calendar.SECOND, 0);
		adjustCal.set(Calendar.MILLISECOND, 0);

		int axis = getAxis(adjustCal, TYPE_ITEM_WEEK, TYPE_FILED_WEEK_OF_MONTH);
		// 全归档期间的开始终了时间
		Date startMDate = new Date();
		Date endMDate = new Date();
		// 期间开始点

		Calendar monCal = Calendar.getInstance();
		Calendar monEndCal = Calendar.getInstance();
		monCal.setTimeInMillis(adjustCal.getTimeInMillis());
		monCal.set(Calendar.DATE, 1);
		monEndCal.setTime(monCal.getTime());
		monEndCal.add(Calendar.MONTH, 1);

		int week = monCal.get(Calendar.DAY_OF_WEEK);
		if (week == Calendar.SUNDAY) {
			monCal.add(Calendar.DATE, 1);
		} else if (week == Calendar.MONDAY) {
		} else {
			monCal.add(Calendar.DATE, 9 - week);
		}
		if (adjustCal.before(monCal)) {
			monCal.add(Calendar.MONTH, -1);
			week = monCal.get(Calendar.DAY_OF_WEEK);
			if (week == Calendar.SUNDAY) {
				monCal.add(Calendar.DATE, 1);
			} else if (week == Calendar.MONDAY) {
			} else {
				monCal.add(Calendar.DATE, 9 - week);
			}
		}
		// startMDate = monCal.getTime();

		TorsionDeviceEntity tdCond = new TorsionDeviceEntity();
		tdCond.setSection_id(section_id);
		tdCond.setPosition_id(position_id);
		List<TorsionDeviceEntity> tds = tdMapper.searchTorsionDevice(tdCond);

		// 收集manage_ids
		Set<String> manageIds = new HashSet<String>();
		for (TorsionDeviceEntity td : tds) {
			manageIds.add(td.getManage_id());
		}

		// 查询备注
		List<String> commentExists = new ArrayList<String>();
		if (manageIds.size() > 0) {
			CheckResultEntity commentCondition = new CheckResultEntity();
			commentCondition.setCheck_confirm_time_start(monCal.getTime());
			commentCondition.setCheck_confirm_time_end(monEndCal.getTime());
			commentCondition.setManage_ids(manageIds);
			commentExists = getCommentsExists(1, commentCondition, crMapper);
		}

		List<TorsionDeviceEntity> tdSeq = new ArrayList<TorsionDeviceEntity>();
		String manage_code = "(Nothing)";
		for (TorsionDeviceEntity td : tds) {
			if (!manage_code.equals(td.getManage_code())) {
				setTorsion(tdSeq, checkContent, manage_code, axis, isLeader,
						monCal, check_file_manage_id, startMDate, endMDate, commentExists, crMapper);
				manage_code = td.getManage_code();
			}
			tdSeq.add(td);
		}
		if (!"(Nothing)".equals(manage_code)) { // 最后一条
			setTorsion(tdSeq, checkContent, manage_code, axis, isLeader,
					monCal, check_file_manage_id, startMDate, endMDate, commentExists, crMapper);
		}
		try {
			input = new BufferedReader(new InputStreamReader(new FileInputStream(PathConsts.BASE_PATH + "\\DeviceInfection\\xml\\" + cfmEntity.getCheck_manage_code() + ".html"),"UTF-8"));
			StringBuffer buffer = new StringBuffer();
			String text;

			while ((text = input.readLine()) != null)
				buffer.append(text);

			String content = buffer.toString();

			content = content.replaceAll("<date type='year'/>", DateUtil.toString(monCal.getTime(), "yyyy"));
			content = content.replaceAll("<date type='month'/>", DateUtil.toString(monCal.getTime(), "M"));

			CheckResultEntity cre = new CheckResultEntity();
			cre.setCheck_confirm_time_start(monCal.getTime());
			cre.setCheck_confirm_time_end(adjustCal.getTime());
			cre.setCheck_file_manage_id(check_file_manage_id);
			cre.setPosition_id(position_id);

			content = setLeaderCheck(cre, crMapper, monCal, adjustCal, TYPE_ITEM_MONTH, cfmEntity.getCycle_type(), content, "", isLeader);

			retContent = content.replaceAll("#content#", checkContent.toString());
		} catch (IOException ioException) {
			_logger.error(ioException.getMessage(), ioException);
		} finally {
			try {
				input.close();
			} catch (IOException e) {
			}
			input = null;
		}
		// 读取文件
		return retContent;
	}

	/**
	 * 电烙铁工具表单
	 * @param section_id
	 * @param position_id
	 * @param check_file_manage_id
	 * @param operator_id
	 * @param isLeader
	 * @param adjustDate
	 * @param conn
	 * @return
	 */
	public String getElectricIronDeviceCheckSheet(String section_id, String position_id,
			String check_file_manage_id, String operator_id, boolean isLeader,
			Date adjustDate, SqlSession conn) {
		CheckResultMapper crMapper = conn.getMapper(CheckResultMapper.class);
		ElectricIronDeviceMapper tdMapper = conn.getMapper(ElectricIronDeviceMapper.class);

		String retContent = "";

		BufferedReader input = null;
		StringBuffer checkContent = new StringBuffer("<style>.row_check_1 td:nth-child(n+1){border:1px solid;border-top:2px solid;}.row_operator td{border:1px solid;}.row_check_more td{border:1px solid;}</style>");

		Calendar adjustCal = Calendar.getInstance();
		adjustCal.setTime(adjustDate);
		adjustCal.set(Calendar.HOUR_OF_DAY, 0);
		adjustCal.set(Calendar.MINUTE, 0);
		adjustCal.set(Calendar.SECOND, 0);
		adjustCal.set(Calendar.MILLISECOND, 0);

		int axis = getAxis(adjustCal, TYPE_ITEM_DAY, TYPE_FILED_MONTH);
		// 期间开始点
		Calendar monCalBase = Calendar.getInstance();
		monCalBase.setTimeInMillis(adjustCal.getTimeInMillis());
		monCalBase.set(Calendar.DATE, 1);

		ElectricIronDeviceEntity eidCond = new ElectricIronDeviceEntity();
		eidCond.setSection_id(section_id);
		eidCond.setPosition_id(position_id);
		Integer kind = 0;
		String templateFile = "MR0501-4B.html";
		switch (check_file_manage_id) {
		case ELECTRIC_IRON_FILE_A : kind = 1; templateFile = "MR0501-4A.html"; break; // TODO p
		case ELECTRIC_IRON_FILE_B : kind = 2; break;
		case ELECTRIC_IRON_FILE_B2 : kind = 3; templateFile = "MR0501-4B2.html";break;
		}
		eidCond.setKind(kind);

		List<ElectricIronDeviceEntity> eids = tdMapper.searchElectricIronDevice(eidCond);

		// 收集manage_ids
		Set<String> manageIds = new HashSet<String>();
		for (ElectricIronDeviceEntity eid : eids) {
			manageIds.add(eid.getManage_id());
		}

		List<String> commentExists = new ArrayList<String>();
		if (manageIds.size() > 0) {
			CheckResultEntity commentCondition = new CheckResultEntity();
			commentCondition.setCheck_confirm_time_start(monCalBase.getTime());
			commentCondition.setCheck_confirm_time_end(adjustCal.getTime());
			commentCondition.setManage_ids(manageIds);
			commentExists = getCommentsExists(1, commentCondition, crMapper);
		}

		List<ElectricIronDeviceEntity> eidSeq = new ArrayList<ElectricIronDeviceEntity>();
		String manage_code = "(Nothing)";
		for (ElectricIronDeviceEntity eid : eids) {
			if (!manage_code.equals(eid.getManage_code())) {
				setElectricIron(eidSeq, checkContent, manage_code, axis, isLeader,
						monCalBase, check_file_manage_id, adjustCal, commentExists, crMapper);
				manage_code = eid.getManage_code();
			}
			eidSeq.add(eid);
		}
		if (!"(Nothing)".equals(manage_code)) {
			setElectricIron(eidSeq, checkContent, manage_code, axis, isLeader,
					monCalBase, check_file_manage_id, adjustCal, commentExists, crMapper);
		}
		try {
			input = new BufferedReader(new InputStreamReader(new FileInputStream(PathConsts.BASE_PATH + "\\DeviceInfection\\xml\\" + templateFile),"UTF-8"));
			StringBuffer buffer = new StringBuffer();
			String text;

			while ((text = input.readLine()) != null)
				buffer.append(text);

			String content = buffer.toString();

			// content = content.replaceAll("<period type='num'/>", bperiod.replaceAll("P", ""));
			// 工程
			if (content.indexOf("<line/>") >= 0 || content.indexOf("<position/>") >= 0) {
				String sLine = "";
				PositionMapper pMapper = conn.getMapper(PositionMapper.class);
				SectionMapper sMapper = conn.getMapper(SectionMapper.class);
				SectionEntity sEntity = sMapper.getSectionByID(section_id);
				if (sEntity != null) {
					sLine += sEntity.getName() + " ";
				}
				PositionEntity pEntity = pMapper.getPositionByID(position_id);
				if (pEntity != null) {
					sLine += pEntity.getProcess_code() + " ";
				}
				content = content.replaceAll("<line/>" , sLine);
				content = content.replaceAll("<position/>" , sLine);
			}

			content = content.replaceAll("<date type='year'/>", DateUtil.toString(monCalBase.getTime(), "yyyy"));
			content = content.replaceAll("<date type='month'/>", DateUtil.toString(monCalBase.getTime(), "M"));

			CheckResultEntity cre = new CheckResultEntity();
			cre.setCheck_confirm_time_start(monCalBase.getTime());
			cre.setCheck_confirm_time_end(adjustCal.getTime());
			cre.setCheck_file_manage_id(check_file_manage_id);
			cre.setPosition_id(position_id);

			content = setLeaderCheck(cre, crMapper, monCalBase, adjustCal, TYPE_ITEM_MONTH, TYPE_FILED_MONTH, content, "", isLeader);

			retContent = content.replaceAll("#content#", checkContent.toString());

		} catch (IOException ioException) {
		} finally {
			try {
				input.close();
			} catch (IOException e) {
			}
			input = null;
		}
		// 读取文件
		return retContent;
	}

	/**
	 * 力矩工具动态数据项目生成（2行/多行）
	 * 
	 * @param tdSeq
	 * @param checkContent
	 * @param manage_code
	 * @param axis
	 * @param isLeader
	 * @param monCalBase
	 * @param check_file_manage_id
	 * @param startMDate
	 * @param endPDate
	 * @param commentExists 
	 * @param crMapper
	 */
	private void setTorsion(List<TorsionDeviceEntity> tdSeq, StringBuffer checkContent, String manage_code,
			int axis, boolean isLeader,
			Calendar monCalBase, 
			String check_file_manage_id, Date startMDate, Date endPDate,
			List<String> commentExists, CheckResultMapper crMapper) {

		if (tdSeq.size() == 1) {

			TorsionDeviceEntity trTorsion = tdSeq.get(0);

			checkContent.append("<tr class='row_check_more'>");
			checkContent.append("<td rowspan='2'>"+manage_code+"</td>");
			checkContent.append("<td rowspan='2'>"+getNoScale(trTorsion.getRegular_torque())+"</td>");
			checkContent.append("<td rowspan='2'>±</td><td rowspan='2'>"+getNoScale(trTorsion.getDeviation())+"</td><td rowspan='2'>N·m</td>");
			checkContent.append("<td rowspan='2'>"+trTorsion.getUsage_point()+"</td>");
			checkContent.append("<td rowspan='2'>"+CodeListUtils.getValue("torsion_device_hp_scale", ""+trTorsion.getHp_scale(), "")+"</td>");
			checkContent.append("<td rowspan='2'>"+trTorsion.getRegular_torque_lower_limit()+"</td>");
			checkContent.append("<td rowspan='2'>～</td><td rowspan='2'>"+trTorsion.getRegular_torque_upper_limit()+"</td>");
			checkContent.append("<td>实测值</td>");

			Map<Integer, String> jobNo = new HashMap<Integer, String>();
			Map<Integer, Date> checkedDate = new HashMap<Integer, Date>();

			getTDataRow(checkContent, axis, isLeader, monCalBase, trTorsion, check_file_manage_id, startMDate, endPDate, crMapper, jobNo, checkedDate);

			checkContent.append("<td colspan=5 rowspan='2'><input type='radio' class='t_comment' name='comment_target' id='comment_"+trTorsion.getManage_id()+"' value='"+trTorsion.getManage_id()+"'/>"
					+ "<label for='comment_"+trTorsion.getManage_id()+"'" + (commentExists.contains(trTorsion.getManage_id()) ? " exists" : "") + ">备注</label>" + "</td>");
			checkContent.append("</tr>");

			// 签章行
			checkContent.append("<tr class='row_operator'>");
			checkContent.append("<td>点检者</td>");
			getTStampRow(checkedDate, jobNo, axis, checkContent);
			checkContent.append("</tr>");

		} else if (tdSeq.size() > 1) {
			int rowspan = tdSeq.size() * 2;
			Map<Integer, String> jobNo = new HashMap<Integer, String>();
			Map<Integer, Date> checkedDate = new HashMap<Integer, Date>();

			for (int j=0;j< tdSeq.size();j++) {
				TorsionDeviceEntity trTorsion = tdSeq.get(j);
				if (j==0) {
					checkContent.append("<tr class='row_check_more'>");
					checkContent.append("<td rowspan='"+rowspan+"'>"+manage_code+"</td>");
					checkContent.append("<td rowspan='2'>"+getNoScale(trTorsion.getRegular_torque())+"</td>");
					checkContent.append("<td rowspan='2'>±</td><td rowspan='2'>"+getNoScale(trTorsion.getDeviation())+"</td><td rowspan='2'>N·m</td>");
					checkContent.append("<td rowspan='"+rowspan+"'>"+trTorsion.getUsage_point()+"</td>");
					checkContent.append("<td rowspan='2'>"+CodeListUtils.getValue("torsion_device_hp_scale", ""+trTorsion.getHp_scale(), "")+"</td>");
					checkContent.append("<td rowspan='2'>"+trTorsion.getRegular_torque_lower_limit()+"</td>");
					checkContent.append("<td rowspan='2'>～</td><td rowspan='2'>"+trTorsion.getRegular_torque_upper_limit()+"</td>");
					checkContent.append("<td>实测值</td>");

					getTDataRow(checkContent, axis, isLeader, monCalBase, trTorsion, check_file_manage_id, startMDate, endPDate, crMapper, jobNo, checkedDate);
					checkContent.append("<td colspan=5 rowspan='"+rowspan+"'><input type='radio' class='t_comment' name='comment_target' id='comment_"+trTorsion.getManage_id()+"' value='"+trTorsion.getManage_id()+"'/>"
							+ "<label for='comment_"+trTorsion.getManage_id()+"'" + (commentExists.contains(trTorsion.getManage_id()) ? " exists" : "") + "'>备注</label>" + "</td>");
					checkContent.append("</tr>");
				} else {
					checkContent.append("<tr class='row_check_more'>");
					checkContent.append("<td rowspan='2'>"+getNoScale(trTorsion.getRegular_torque())+"</td>");
					checkContent.append("<td rowspan='2'>±</td><td rowspan='2'>"+getNoScale(trTorsion.getDeviation())+"</td><td rowspan='2'>N·m</td>");
					checkContent.append("<td rowspan='2'>"+CodeListUtils.getValue("torsion_device_hp_scale", ""+trTorsion.getHp_scale(), "")+"</td>");
					checkContent.append("<td rowspan='2'>"+trTorsion.getRegular_torque_lower_limit()+"</td>");
					checkContent.append("<td rowspan='2'>～</td><td rowspan='2'>"+trTorsion.getRegular_torque_upper_limit()+"</td>");
					checkContent.append("<td>实测值</td>");

					getTDataRow(checkContent, axis, isLeader, monCalBase, trTorsion, check_file_manage_id, startMDate, endPDate, crMapper, jobNo, checkedDate);
					checkContent.append("</tr>");
				}

				// 签章行
				checkContent.append("<tr class='row_operator'>");
				checkContent.append("<td>点检者</td>");
				getTStampRow(checkedDate, jobNo, axis, checkContent);
				checkContent.append("</tr>");
			}
		}
		tdSeq.clear();
	}

	/**
	 * 力矩工具动态签章行生成
	 * 
	 * @param checkedDate
	 * @param jobNo
	 * @param axis
	 * @param checkContent
	 */
	private void getTStampRow(Map<Integer, Date> checkedDate, Map<Integer, String> jobNo, int axis, StringBuffer checkContent) {
		for (int i=0; i< 5; i++) {
			if (jobNo.containsKey(i)) {
				checkContent.append("<td>"+DateUtil.toString(checkedDate.get(i), "MM-dd")+"</td><td colspan=2><img src='/images/sign/"+jobNo.get(i)+"'></td>");
			} else {
				checkContent.append("<td colspan=3></td>");
			}
		}
	}

	private void getEIStampRow(Map<Integer, String> jobNo, int axis, StringBuffer checkContent) {
		for (int i=0; i< 31; i++) {
			if (jobNo.containsKey(i)) {
				checkContent.append("<td><img src='/images/sign_v/"+jobNo.get(i)+"' class='sign_v'></td>");
			} else {
				checkContent.append("<td></td>");
			}
		}
	}

	/**
	 * 力矩设备的行
	 * @param checkContent
	 * @param axis
	 * @param monCalBase
	 * @param trTorsion
	 * @param check_file_manage_id
	 * @param startPDate
	 * @param endPDate
	 * @param crMapper
	 * @param checkedDate2 
	 * @param jobNo 
	 */
	private void getTDataRow(StringBuffer checkContent, int axis, boolean isLeader,
			Calendar monCalBase, TorsionDeviceEntity trTorsion,
			String check_file_manage_id, Date startPDate, Date endPDate,
			CheckResultMapper crMapper, Map<Integer, String> jobNo, Map<Integer, Date> checkedDate) {
		// 每个单元格取值
		Calendar monCal = Calendar.getInstance();
		monCal.setTimeInMillis(monCalBase.getTimeInMillis());
		int iAxis=0;

		for (iAxis=0;iAxis<=axis;iAxis++) {
			boolean current = (iAxis==axis);

			String thisText = "<td></td>";
			Date[] dates = getDayOfAxis(monCal, iAxis, TYPE_ITEM_WEEK, TYPE_FILED_WEEK_OF_MONTH);
			if (iAxis==0) startPDate = dates[0]; 
			endPDate = dates[1];

			CheckResultEntity cre = new CheckResultEntity();
			cre.setCheck_confirm_time_start(dates[0]);
			cre.setCheck_confirm_time_end(dates[1]);
			cre.setManage_id(trTorsion.getManage_id());
			cre.setCheck_file_manage_id(check_file_manage_id);
			cre.setItem_seq(trTorsion.getSeq());

			// 已点检或之前范围
			List<CheckResultEntity> listCre = crMapper.getTorsionDeviceCheckInPeriod(cre);

			// 取得已点检单元格信息
			if (listCre != null && listCre.size() > 0) {
				CheckResultEntity rCre = listCre.get(0);
				if (!checkedDate.containsKey(iAxis) || checkedDate.get(iAxis).before(rCre.getCheck_confirm_time())) {
					jobNo.put(iAxis, rCre.getJob_no());
					checkedDate.put(iAxis, rCre.getCheck_confirm_time());
				}
				thisText = "<td colspan=3>" + getStatusDV(""+rCre.getChecked_status(), (rCre.getDigit()== null? "" : getNoScale(rCre.getDigit().toPlainString())), 
						isLeader, trTorsion.getSeq(), (current ? 0 : -1), trTorsion.getManage_id(), 
						" lower_limit='" + trTorsion.getRegular_torque_lower_limit() + 
						"' upper_limit='"+ trTorsion.getRegular_torque_upper_limit() + "'" ) +
						"</td>";
			}

			// 当前范围未点检
			else if (current) {
				// 填写数据
				String matchKey = "d_" + trTorsion.getManage_id() + "_"+trTorsion.getSeq()+"_" + iAxis;
				thisText = "<td colspan=3 lock><input name='" + matchKey + "' type='text' id='" + matchKey + "_i' lower_limit='"
						+ trTorsion.getRegular_torque_lower_limit() + "' upper_limit='"+ trTorsion.getRegular_torque_upper_limit() +"' ovalue=''/></td>";
			} else {
				thisText = "<td colspan=3>/</td>";
			}
			checkContent.append(thisText);
		}
		for (;iAxis<5;iAxis++) {
			checkContent.append("<td colspan=3></td>");
		}
	}

	private void setElectricIron(List<ElectricIronDeviceEntity> eidSeq, StringBuffer checkContent, String manage_code,
			int axis, boolean isLeader,
			Calendar monCalBase, 
			String check_file_manage_id, Calendar adjustCal,
			List<String> commentExists, 
			CheckResultMapper crMapper) {

		if (eidSeq.size() == 0) return;

		Map<Integer, String> jobNo = new HashMap<Integer, String>();

		CheckResultEntity enti = new CheckResultEntity();
		String manage_id = eidSeq.get(0).getManage_id();
		enti.setManage_id(manage_id);
		enti.setCheck_file_manage_id(check_file_manage_id);

		enti.setCheck_confirm_time_start(monCalBase.getTime());
		// 月底
		Calendar monthEnd = Calendar.getInstance();
		monthEnd.setTimeInMillis(monCalBase.getTimeInMillis());
		monthEnd.add(Calendar.MONTH, 1);
		monthEnd.add(Calendar.DATE, -1);
		enti.setCheck_confirm_time_end(monthEnd.getTime());

		if (eidSeq.size() == 1) {

			ElectricIronDeviceEntity trElectricIron = eidSeq.get(0);
			checkContent.append("<tr class='row_check_1'>");
			checkContent.append("<td class='TDb3 BSd1 LSd2 RSd1 ' rowspan='4'>"+trElectricIron.getManage_code()+
					"<input type='radio' class='t_comment' name='comment_target' id='comment_"+trElectricIron.getManage_id()+"' value='"+trElectricIron.getManage_id()+"'/>"
							+ "<label for='comment_"+trElectricIron.getManage_id()+"'"+ (commentExists.contains(trElectricIron.getManage_id()) ? " exists" : "") + ">备注</label>" +
					"</td>");
			checkContent.append("<td class='TSd1 BSd1 LSd1 R0 '>绝缘电阻：</td>");
			checkContent.append("<td class='TSd1 BSd1 LSd1 R0 '>10MΩ以上<br>(单位：MΩ)</td>");
			checkContent.append("<td class='TSd1 BSd1 LSd1 R0 '>1次/月</td>");
			checkContent.append("<td class='HC WT TDb3 BSd1 LSd1 RSd1 ' rowspan='3'>实测值</td>");
			getEIDataRow(checkContent, axis, isLeader, monCalBase, trElectricIron, check_file_manage_id, adjustCal, crMapper, jobNo, "51");
			checkContent.append("</tr>");

			checkContent.append("<tr class='row_check_more'>");
			checkContent.append("<td class='T0 BSd1 LSd1 R0 '>接      地</td>");
			checkContent.append("<td class='TSd1 BSd1 LSd1 R0 '>20Ω以下<br>(单位：Ω）</td>");
			checkContent.append("<td class='TSd1 BSd1 LSd1 R0 '>1次/月</td>");
			getEIDataRow(checkContent, axis, isLeader, monCalBase, trElectricIron, check_file_manage_id, adjustCal, crMapper, jobNo, "50");
			checkContent.append("</tr>");

			checkContent.append("<tr class='row_check_more'>");
			checkContent.append("<td class='T0 BSd1 LSd1 R0 ' rowspan='2'>烙铁头温度</td>");
			checkContent.append("<td class='HC T0 BSd1 LSd1 RSd1' rowspan='2'>"+trElectricIron.getTemperature_lower_limit()+"   ～   "
					+trElectricIron.getTemperature_upper_limit() + "<br><br>（单位：℃）</td>");
			checkContent.append("<td class='HC WT TDb3 BSd1 LSd1 RSd1' rowspan='2'>1次/日<br>(或使用时)</td>");
			getEIDataRow(checkContent, axis, isLeader, monCalBase, trElectricIron, check_file_manage_id, adjustCal, crMapper, jobNo, "01");
			checkContent.append("</tr>");

			checkContent.append("<tr class='row_operator'>");
			checkContent.append("<td class='HC WT TSd1 BSd1 LSd1 RSd1 '>点检者</td>");
			getEIStampRow(jobNo, axis, checkContent);
			checkContent.append("</tr>");

		} else if (eidSeq.size() > 1) {}
		eidSeq.clear();
	}

	/**
	 * 
	 * @param checkContent 点检文件内容
	 * @param axis 坐标总数
	 * @param isLeader 管理员
	 * @param monCalBase 点检开始月
	 * @param trEI 点检数据
	 * @param check_file_manage_id 点检文件
	 * @param currentCal 当前日期
	 * @param crMapper
	 * @param jobNo 每个单元的点检者
	 */
	private void getEIDataRow(StringBuffer checkContent, int axis, boolean isLeader,
			Calendar monCalBase, ElectricIronDeviceEntity trEI,
			String check_file_manage_id, Calendar currentCal,
			CheckResultMapper crMapper, Map<Integer, String> jobNo, String item_seq) {
		// 每个单元格取值
		Calendar monCal = Calendar.getInstance();
		monCal.setTimeInMillis(monCalBase.getTimeInMillis());
		int iAxis=0;

		boolean checked01 = false; boolean checked02 = false;
		for (iAxis=0;iAxis<=axis;iAxis++) {
			boolean current = (iAxis==axis);

			String thisText = "<td></td>";
			Date[] dates = getDayOfAxis(monCal, iAxis, TYPE_ITEM_DAY, TYPE_FILED_MONTH);

			CheckResultEntity cre = new CheckResultEntity();
			cre.setCheck_confirm_time(dates[0]);
			cre.setManage_id(trEI.getManage_id());
			cre.setCheck_file_manage_id(check_file_manage_id);
			cre.setItem_seq(item_seq);

			// 已点检或之前范围
			List<CheckResultEntity> listCre = crMapper.getEIDeviceCheckOfDate(cre);

			// 取得已点检单元格信息
			if (listCre != null && listCre.size() > 0) {
				CheckResultEntity rCre = listCre.get(0);
				if (rCre.getChecked_status() == 0 && current) {
					// 填写数据
					String matchKey = "d_" + trEI.getManage_id() + "_"+item_seq+"_" + iAxis;
					if ("01".equals(item_seq)) {
						thisText = "<td><input name='" + matchKey + "' type='text' id='" + matchKey + "_i' lower_limit='"
								+ trEI.getTemperature_lower_limit() + "' upper_limit='"+ trEI.getTemperature_upper_limit() +"' ovalue='' lock/></td>";
					}
				} else {
					jobNo.put(iAxis, rCre.getJob_no());
					if ("01".equals(item_seq)) {
						thisText = "<td>" + getStatusDV(""+rCre.getChecked_status(), (rCre.getDigit()== null? "" : getNoScale(rCre.getDigit().toPlainString())), 
								isLeader, item_seq, (current ? 0 : -1), trEI.getManage_id(), 
								" lower_limit='" + trEI.getTemperature_lower_limit() + 
								"' upper_limit='"+ trEI.getTemperature_upper_limit() + "'" ) +
								"</td>";
					} else if ("50".equals(item_seq)) {
						checked02 = true;
						thisText = "<td>" + getStatusDV(""+rCre.getChecked_status(), (rCre.getDigit()== null? "" : getNoScale(rCre.getDigit().toPlainString())), 
								isLeader, item_seq, (current ? 0 : -1), trEI.getManage_id(), 
								" upper_limit='20'" ) +
								"</td>";
					} else if ("51".equals(item_seq)) {
						checked01 = true;
						thisText = "<td>" + getStatusDV(""+rCre.getChecked_status(), (rCre.getDigit()== null? "" : getNoScale(rCre.getDigit().toPlainString())), 
								isLeader, item_seq, (current ? 0 : -1), trEI.getManage_id(), 
								" lower_limit='10'" ) +
								"</td>";
					}
				}
			}

			// 当前范围未点检
			else if (current) {
				// 填写数据
				String matchKey = "d_" + trEI.getManage_id() + "_"+item_seq+"_" + iAxis;
				if ("01".equals(item_seq)) {
					thisText = "<td><input name='" + matchKey + "' type='text' id='" + matchKey + "_i' lower_limit='"
							+ trEI.getTemperature_lower_limit() + "' upper_limit='"+ trEI.getTemperature_upper_limit() +"' ovalue=''/></td>";
				} else if ("50".equals(item_seq)) {
					thisText = "<td lock>";
					if (checked02) {
						thisText = "<td>";
					}
					thisText += "<input name='" + matchKey + "' type='text' id='" + matchKey + "_i' upper_limit='20' ovalue=''/></td>";
				} else if ("51".equals(item_seq)) {
					thisText = "<td lock>";
					if (checked01) {
						thisText = "<td>";
					}
					thisText += "<input name='" + matchKey + "' type='text' id='" + matchKey + "_i' lower_limit='10' ovalue=''/></td>";
				}
				// 选择合格项 TODO
			} else {
				thisText = "<td>/</td>";
			}
			checkContent.append(thisText);
		}
		for (;iAxis<31;iAxis++) {
			checkContent.append("<td></td>");
		}
	}

	private void getOfSeq(Map<String, String> limitOfSeq, String sSeq,
			Matcher mInputData) {
		String otherCo = "";
		int groupCount = mInputData.groupCount();
		String sUpperLimit = null;
		String sLowerLimit = null;
		String sUpperLimitFrom = null;
		String sLowerLimitFrom = null;

		if (groupCount >= 3)
			sUpperLimit = mInputData.group(3);
		if (groupCount >= 4)
			sLowerLimit = mInputData.group(4);
		if (groupCount >= 5)
			sUpperLimitFrom = mInputData.group(5);
		if (groupCount >= 6)
			sLowerLimitFrom = mInputData.group(6);

		if (!isEmpty(sUpperLimit)) {
			otherCo += sUpperLimit + " ";
		}
		if (!isEmpty(sLowerLimit)) {
			otherCo += sLowerLimit + " ";
		}
		if (!isEmpty(sUpperLimitFrom)) {
			otherCo += sUpperLimitFrom + " ";
		}
		if (!isEmpty(sLowerLimitFrom)) {
			otherCo += sLowerLimitFrom + " ";
		}
		limitOfSeq.put(sSeq, otherCo);
	}

	private String getNoScale(BigDecimal plainBd) {
		return getNoScale(plainBd.toPlainString());
	}
	private String getNoScale(String plainString) {
		if ("999999.999".equals(plainString)) {
			return "∞";
		}
		return plainString.replaceAll("^(\\-?\\d*\\.\\d*[1-9])0*$", "$1").replaceAll("^(\\-?\\d*)\\.0*$", "$1");
	}

	private Date[] getDayOfAxis(Calendar start, int axisDiff, int axisType, Integer fileType) {
		Date[] retDs = new Date[2];
		Calendar jCal = Calendar.getInstance(); 
		jCal.setTimeInMillis(start.getTimeInMillis());
		jCal.set(Calendar.HOUR_OF_DAY, 0);
		jCal.set(Calendar.MINUTE, 0);
		jCal.set(Calendar.SECOND, 0);
		jCal.set(Calendar.MILLISECOND, 0);
		if (axisType == TYPE_ITEM_DAY && fileType == TYPE_FILED_MONTH) {
			int iMonthC = jCal.get(Calendar.MONTH);
			jCal.add(Calendar.DATE, axisDiff);
			int iMonthN = jCal.get(Calendar.MONTH);
			if (iMonthN != iMonthC) {
				jCal.set(Calendar.YEAR, 1900);
			}
			retDs[0] = jCal.getTime();
			retDs[1] = jCal.getTime();
		} else if (axisType == TYPE_ITEM_WEEK && fileType == TYPE_FILED_WEEK_OF_MONTH) {
			Date[][] weekEnds = getWeekEndsOfMonth(start);
			if (axisDiff >= weekEnds.length) {
				jCal.set(Calendar.YEAR, 1900);
				retDs[0] = jCal.getTime();
				retDs[1] = jCal.getTime();
			} else {
				return weekEnds[axisDiff];
			}
		} else if (axisType == TYPE_ITEM_MONTH && fileType == TYPE_FILED_WEEK_OF_MONTH) {
			Date[][] weekEnds = getWeekEndsOfMonth(start);

			retDs[0] = weekEnds[0][0];

			retDs[1] = weekEnds[weekEnds.length - 1][1];
		} else if (axisType == TYPE_ITEM_MONTH && fileType == TYPE_FILED_YEAR) {
			jCal.add(Calendar.MONTH, axisDiff);
			jCal.set(Calendar.DATE, 1);
			retDs[0] = jCal.getTime();
			jCal.add(Calendar.MONTH, 1);
			jCal.add(Calendar.DATE, -1);
			retDs[1] = jCal.getTime();
//		} else if (axisType == TYPE_HALF_MONTH_OF_YEAR) {
//			int month = axisDiff / 2;
//			int half = axisDiff % 2;
//			if (half == 0) {
//				jCal.add(Calendar.MONTH, month);
//				jCal.set(Calendar.DATE, 1);
//				retDs[0] = jCal.getTime();
//				jCal.set(Calendar.DATE, 15);
//				retDs[1] = jCal.getTime();
//			} else {
//				jCal.add(Calendar.MONTH, month);
//				jCal.set(Calendar.DATE, 16);
//				retDs[0] = jCal.getTime();
//				jCal.set(Calendar.DATE, 1);
//				jCal.add(Calendar.MONTH, 1);
//				jCal.add(Calendar.DATE, -1);
//				retDs[1] = jCal.getTime();
//			}
		} else if (axisType == TYPE_ITEM_MONTH && fileType == TYPE_FILED_MONTH) {
			retDs[0] = jCal.getTime();
			jCal.add(Calendar.MONTH, 1);
			jCal.add(Calendar.DATE, -1);
			retDs[1] = jCal.getTime();
		} else if (axisType == TYPE_ITEM_PERIOD && fileType == TYPE_FILED_YEAR) {
			jCal.add(Calendar.MONTH, 6 * axisDiff);
			jCal.set(Calendar.DATE, 1);
			retDs[0] = jCal.getTime();
			jCal.add(Calendar.MONTH, 6);
			jCal.add(Calendar.DATE, -1);
			retDs[1] = jCal.getTime();
		} else if (axisType == TYPE_ITEM_YEAR && fileType == TYPE_FILED_YEAR) {
			jCal.set(Calendar.DATE, 1);
			retDs[0] = jCal.getTime();
			jCal.add(Calendar.MONTH, 12);
			jCal.add(Calendar.DATE, -1);
			retDs[1] = jCal.getTime();
		}
		return retDs;
	}

	/**
	 * 获得设备工具/治具的备注
	 * @param manage_id 设备工具/治具ID
	 * @param object_type 设备工具/治具类型
	 * @param check_file_manage_id 归档文档
	 * @param adjustDate 时期
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public String getCheckCommentInPeriod(String manage_id, String object_type, String check_file_manage_id, Date adjustDate, SqlSession conn) throws Exception {

		CheckResultMapper crMapper = conn.getMapper(CheckResultMapper.class);

		int axisType = 0; 
		if ("2".equals(object_type)) {
			// 治具
			axisType = TYPE_FILED_YEAR;
		} else {
			// 取得点检表信息
			CheckFileManageMapper cfmMapper = conn.getMapper(CheckFileManageMapper.class);
			CheckFileManageEntity cfmEntity = cfmMapper.getByKey(check_file_manage_id);

			if (CheckFileManageEntity.ACCESS_PLACE_DAILY == cfmEntity.getAccess_place()) {
				axisType = TYPE_FILED_MONTH;
			} else {
				axisType = cfmEntity.getCycle_type();
			}
		}

		CheckResultEntity condition = new CheckResultEntity();
		condition.setManage_id(manage_id);

		String dayString = DateUtil.toString(adjustDate, DateUtil.ISO_DATE_PATTERN);
		PeriodsEntity pd = getPeriodsOfDate(dayString, conn);
		if (axisType == TYPE_FILED_MONTH || axisType == TYPE_FILED_WEEK_OF_MONTH) {
			condition.setCheck_confirm_time_start(pd.getStartOfMonth());
			condition.setCheck_confirm_time_end(pd.getEndOfMonth());
		} else if (axisType == TYPE_FILED_YEAR) {
			condition.setCheck_confirm_time_start(pd.getStartOfPeriod());
			condition.setCheck_confirm_time_end(pd.getEndOfPeriod());
		}

		if ("2".equals(object_type)) {
			return crMapper.getJigCheckCommentInPeriodByManageIdGroup(condition);
		} else if ("1".equals(object_type)) {
			return crMapper.getDeviceCheckCommentInPeriodByManageIdGroup(condition);
		} else {
			return "";
		}
	}

	/**
	 * 输入点检备注
	 * @param manage_id
	 * @param object_type
	 * @param operator_id
	 * @param comment 
	 * @param adjustDate
	 * @param conn
	 */
	public void inputCheckComment(String manage_id, String object_type, String operator_id,
			String comment, Date adjustDate, SqlSessionManager conn) {
		CheckResultMapper crMapper = conn.getMapper(CheckResultMapper.class);
		CheckResultEntity condition = new CheckResultEntity();
		condition.setManage_id(manage_id);
		condition.setObject_type(object_type);
		condition.setOperator_id(operator_id);
		condition.setCheck_confirm_time(adjustDate);
		condition.setComment(comment);
		crMapper.inputCheckComment(condition);
	}

	/**
	 * 取得当月每星期的开始终了日期
	 * @param adjustDate
	 * @return
	 */
	public Date[][] getWeekEndsOfMonth(Calendar adjustDate) {
		Calendar jCal = Calendar.getInstance();
		jCal.setTimeInMillis(adjustDate.getTimeInMillis());
		jCal.set(Calendar.HOUR_OF_DAY, 0);
		jCal.set(Calendar.MINUTE, 0);
		jCal.set(Calendar.SECOND, 0);
		jCal.set(Calendar.MILLISECOND, 0);

		// 本月第一个周一
		jCal.set(Calendar.DATE, 1);
		int week = jCal.get(Calendar.DAY_OF_WEEK);
		int month = jCal.get(Calendar.MONTH);
		if (week == Calendar.SUNDAY) {
			jCal.add(Calendar.DATE, 1);
		} else if (week == Calendar.MONDAY) {
		} else {
			jCal.add(Calendar.DATE, 9 - week);
		}

		// 如果不在本月范围内,则在上月范围内
		if (adjustDate.getTimeInMillis() < jCal.getTimeInMillis()) {
			Calendar jEndOfLastMonth = Calendar.getInstance();
			jEndOfLastMonth.setTimeInMillis(adjustDate.getTimeInMillis());
			jEndOfLastMonth.set(Calendar.DATE, 1);
			jEndOfLastMonth.add(Calendar.DATE, -1);
			return getWeekEndsOfMonth(jEndOfLastMonth);
		}

		String monthString = DateUtil.toString(jCal.getTime(), "yy-MM");
		if (weekEndOfMonth.containsKey(monthString)) {
			return weekEndOfMonth.get(monthString);
		} else {
			List<Date[]> seLOfWeeks = new ArrayList<Date[]>();
			while (month == jCal.get(Calendar.MONTH)){
				Date[] se = new Date[2];
				se[0] = jCal.getTime();
				jCal.add(Calendar.DATE, 6);
				se[1] = jCal.getTime();
				jCal.add(Calendar.DATE, 1);
				seLOfWeeks.add(se);
			}

			int seLOfWeeksSize = seLOfWeeks.size();
			Date[][] seAOfWeeks = new Date[seLOfWeeksSize][];
			for (int i=0; i<seLOfWeeksSize; i++) {
				seAOfWeeks[i] = seLOfWeeks.get(i);
			}
			weekEndOfMonth.put(monthString, seAOfWeeks);
			return weekEndOfMonth.get(monthString);
		}
	}

	/** ============ 归档 =========== 
	 * @throws IOException */
	public void makeFileGroup(CheckedFileStorageEntity cfsEntity,
			List<String> sEncodedDeviceList, SqlSession conn) throws IOException {
		Date today = new Date();

		// 当日
		Calendar adjustCal = Calendar.getInstance();
		adjustCal.setTime(cfsEntity.getFiling_date());
		adjustCal.set(Calendar.HOUR_OF_DAY, 0);
		adjustCal.set(Calendar.MINUTE, 0);
		adjustCal.set(Calendar.SECOND, 0);
		adjustCal.set(Calendar.MILLISECOND, 0);

		DevicesManageMapper dmMapper = conn.getMapper(DevicesManageMapper.class);

		// 取得管理票文件作为模板
		String check_file_manage_file_name = cfsEntity.getTemplate_file_name();
		String srcPathPart = PathConsts.BASE_PATH + PathConsts.DEVICEINFECTION + "\\" + check_file_manage_file_name;
		String ext = ".xlsx";
		String srcPath = srcPathPart + ext;
		File srcFile = new File(srcPath);
		if (!srcFile.exists()) {
			ext = ".xls";
			srcPath = srcPathPart + ext;
		}

		// 取得点检表信息
		CheckFileManageMapper cfmMapper = conn.getMapper(CheckFileManageMapper.class);
		String check_file_manage_id = cfsEntity.getCheck_file_manage_id();
		CheckFileManageEntity cfmEntity = cfmMapper.getByKey(check_file_manage_id);

		// 复制模板到临时文件
		String cacheFilename =  cfsEntity.getStorage_file_name() + today.getTime() + ext;
		String cachePath = PathConsts.BASE_PATH + PathConsts.LOAD_TEMP + "\\" + DateUtil.toString(today, "yyyyMM") + "\\" + cacheFilename;
		FileUtils.copyFile(new File(srcPath), new File(cachePath));
		// SAMPLE D:\rvs\Infections\147P\QR-B31002-10A\QR-B31002-10A_报价物料课 受理报价_147P12月.xls
		String targetPath = PathConsts.BASE_PATH + PathConsts.INFECTIONS + "\\" + 
				RvsUtils.getBussinessYearString(adjustCal) + "\\" +
				cfmEntity.getCheck_manage_code();
		String targetFile = targetPath + "\\" + cfsEntity.getStorage_file_name() + ".pdf";

		CheckResultMapper crMapper = conn.getMapper(CheckResultMapper.class);

		XlsUtil cacheXls = null;
		try {
			cacheXls = new XlsUtil(cachePath, false);
			cacheXls.SelectActiveSheet();

			// 取得本期
			String bperiod = RvsUtils.getBussinessYearString(adjustCal);

			// 工程
			String line_id = cfsEntity.getLine_id();
			String sLineName = "";
			SectionMapper sMapper = conn.getMapper(SectionMapper.class);
			SectionEntity sEntity = sMapper.getSectionByID(cfsEntity.getSection_id());
			if (sEntity != null) {
				sLineName += sEntity.getName() + "\n";
			}
			if (!isEmpty(line_id)) {
				LineMapper lMapper = conn.getMapper(LineMapper.class);
				LineEntity lEntity = lMapper.getLineByID(line_id);
				if (lEntity != null) {
					sLineName += lEntity.getName();
				}
				cacheXls.Replace("#G[LINE#", sLineName);
			}
			// 工位
			String position_id = cfsEntity.getPosition_id();
			if (!isEmpty(position_id)) {
				PositionMapper pMapper = conn.getMapper(PositionMapper.class);
				// 工位
				PositionEntity pEntity = pMapper.getPositionByID(cfsEntity.getPosition_id());
				if (pEntity != null) {
					sLineName += pEntity.getProcess_code() + " ";
				}
				Dispatch positionCell = cacheXls.Locate("#G[POSITION#");
				if (positionCell != null) {
					String FoundValue = Dispatch.get(positionCell, "Value").toString();
					if (FoundValue.equals("#G[POSITION#")) {
						cacheXls.SetValue(positionCell, sLineName);
					} else {
						cacheXls.Replace("#G[POSITION#", sLineName.replaceAll("\\\n", " "));
					}
				}
			}

			// 替换共通数据
			cacheXls.Replace("#G[PERIOD#", bperiod);
			cacheXls.Replace("#G[PERIODC#", bperiod.replaceAll("P", ""));
			cacheXls.Replace("#G[YEAR#", DateUtil.toString(cfsEntity.getFiling_date(), "yyyy"));
			cacheXls.Replace("#G[MONTH#", DateUtil.toString(cfsEntity.getFiling_date(), "M"));

			// 文档章
			//#J
			Dispatch cell = cacheXls.Locate("#J");
			while (cell != null) {
				String stamp = Dispatch.get(cell, "Value").toString();
				String jobNo = stamp.replaceAll("#J\\[(.*)#", "$1");
				cacheXls.SetValue(cell, "");
				cacheXls.sign(PathConsts.BASE_PATH + PathConsts.IMAGES + "\\sign\\" + jobNo.toUpperCase(), cell);
				cell = cacheXls.Locate("#J");
			}

			// 确定表单的归档类型
			int fileAxisType = 0; 
			if (CheckFileManageEntity.ACCESS_PLACE_DAILY == cfmEntity.getAccess_place()) {
				fileAxisType = TYPE_FILED_MONTH;
			}

			// 计算范围用日历
			Calendar monCal = Calendar.getInstance();
			if (CheckFileManageEntity.ACCESS_PLACE_DAILY == cfmEntity.getAccess_place()) {
				monCal.setTimeInMillis(adjustCal.getTimeInMillis());
				monCal.set(Calendar.DATE, 1);
			} else {
				if (TYPE_FILED_YEAR == fileAxisType) {
					// 去期间头
					monCal = getStartOfPeriod(adjustCal);
				} else {
					// 去月首
					monCal.setTimeInMillis(adjustCal.getTimeInMillis());
					monCal.set(Calendar.DATE, 1);
					if (TYPE_FILED_WEEK_OF_MONTH == fileAxisType) {
						int week = monCal.get(Calendar.DAY_OF_WEEK);
						if (week == Calendar.SUNDAY) {
							monCal.add(Calendar.DATE, 1);
						} else if (week == Calendar.MONDAY) {
						} else {
							monCal.add(Calendar.DATE, 9 - week);
						}
					}
				}
			}

			Integer itemType = 0;
			int axis = getMaxAxis(itemType, fileAxisType);

			// 取得输入项定位
			Map<String, CheckPosBean> checkPosData = new HashMap<String, CheckPosBean>();
			List<CheckPosBean> checkPosDate = new ArrayList<CheckPosBean>();
			List<CheckPosBean> checkPosName = new ArrayList<CheckPosBean>();
			CheckPosBean checkPosManageNo = new CheckPosBean();
			CheckPosBean checkPosModel = new CheckPosBean();
			CheckPosBean checkDeviceName = new CheckPosBean();

			CheckPosBean checkUseStart = new CheckPosBean();
			CheckPosBean checkUseEnd = new CheckPosBean();

			cell = cacheXls.Locate("#D");
			String FoundValue = null;
			if (cell != null) {
				FoundValue = Dispatch.get(cell, "Value").toString();
			}
			while (FoundValue != null) {
				cell = cacheXls.Locate("#D");
				if (cell == null) {
					FoundValue = null;
				} else {
					FoundValue = Dispatch.get(cell, "Value").toString();
					CheckPosBean cpBean = new CheckPosBean();
					cpBean.startX = Integer.parseInt(XlsUtil.getExcelColNo(cell));
					cpBean.startY = Integer.parseInt(XlsUtil.getExcelRowNo(cell));
					cpBean.content = FoundValue.replaceAll("#[^#]*#", "#tag#");
					String tagText = FoundValue.replaceAll("[^#]*#([^#]*)#[^#]*", "$1");
					String[] tags = tagText.split("\\[");
					String seq = null; 
					for (String tag : tags) {
						if (tag.startsWith("S")) {
							// 取得序列信息
							seq = tag.substring(1, 3);
						}
						else if (tag.startsWith("T")) {
							// 单元格中的跳动
							cpBean.shiftX = Integer.parseInt(tag.substring(1));
						}
						else if (tag.startsWith("U")) {
							// 单元格中的跳动
							cpBean.shiftY = Integer.parseInt(tag.substring(1));
						}
					}
					checkPosData.put(seq, cpBean);

					cacheXls.SetValue(cell, "");
				}
			}

			// #G[MANAGENO
			cell = cacheXls.Locate("#G[MANAGENO");
			if (cell != null) {
				FoundValue = Dispatch.get(cell, "Value").toString();
				checkPosManageNo.startX = Integer.parseInt(XlsUtil.getExcelColNo(cell));
				checkPosManageNo.startY = Integer.parseInt(XlsUtil.getExcelRowNo(cell));
				String tagText = FoundValue.replaceAll("[^#]*#([^#]*)#[^#]*", "$1");
				String[] tags = tagText.split("\\[");
				for (String tag : tags) {
					if (tag.startsWith("U")) {
						// 单元格中的跳动
						checkPosManageNo.shiftY = Integer.parseInt(tag.substring(1));
					}
				}
			}

			// #G[MODEL[U1#	#G[USESTART[U1#	#G[USELIMIT[U1#
			cell = cacheXls.Locate("#G[MODEL");
			if (cell != null) {
				FoundValue = Dispatch.get(cell, "Value").toString();
				checkPosModel.content = FoundValue;
				checkPosModel.startX = Integer.parseInt(XlsUtil.getExcelColNo(cell));
				checkPosModel.startY = Integer.parseInt(XlsUtil.getExcelRowNo(cell));
				String tagText = FoundValue.replaceAll("[^#]*#([^#]*)#[^#]*", "$1");
				String[] tags = tagText.split("\\[");
				for (String tag : tags) {
					if (tag.startsWith("U")) {
						// 单元格中的跳动
						checkPosModel.shiftY = Integer.parseInt(tag.substring(1));
					}
				}
			}

			// #G[NAME
			cell = cacheXls.Locate("#G[NAME");
			if (cell != null) {
				FoundValue = Dispatch.get(cell, "Value").toString();
				checkDeviceName.content = FoundValue;
				checkDeviceName.startX = Integer.parseInt(XlsUtil.getExcelColNo(cell));
				checkDeviceName.startY = Integer.parseInt(XlsUtil.getExcelRowNo(cell));
				String tagText = FoundValue.replaceAll("[^#]*#([^#]*)#[^#]*", "$1");
				String[] tags = tagText.split("\\[");
				for (String tag : tags) {
					if (tag.startsWith("U")) {
						// 单元格中的跳动
						checkDeviceName.shiftY = Integer.parseInt(tag.substring(1));
					}
				}
			}

			cell = cacheXls.Locate("#G[USESTART");
			if (cell != null) {
				FoundValue = Dispatch.get(cell, "Value").toString();
				checkUseStart.content = FoundValue;
				checkUseStart.startX = Integer.parseInt(XlsUtil.getExcelColNo(cell));
				checkUseStart.startY = Integer.parseInt(XlsUtil.getExcelRowNo(cell));
				String tagText = FoundValue.replaceAll("[^#]*#([^#]*)#[^#]*", "$1");
				String[] tags = tagText.split("\\[");
				for (String tag : tags) {
					if (tag.startsWith("U") && !tag.startsWith("USE")) {
						// 单元格中的跳动
						checkUseStart.shiftY = Integer.parseInt(tag.substring(1));
					}
				}
			}

			cell = cacheXls.Locate("#G[USELIMIT");
			if (cell != null) {
				FoundValue = Dispatch.get(cell, "Value").toString();
				checkUseEnd.content = FoundValue;
				checkUseEnd.startX = Integer.parseInt(XlsUtil.getExcelColNo(cell));
				checkUseEnd.startY = Integer.parseInt(XlsUtil.getExcelRowNo(cell));
				String tagText = FoundValue.replaceAll("[^#]*#([^#]*)#[^#]*", "$1");
				String[] tags = tagText.split("\\[");
				for (String tag : tags) {
					if (tag.startsWith("U") && !tag.startsWith("USE")) {
						// 单元格中的跳动
						checkUseEnd.shiftY = Integer.parseInt(tag.substring(1));
					} else if (tag.startsWith("P")) {
						checkUseEnd.shiftX = Integer.parseInt(tag.substring(1));
					}
				}
			}

			cell = cacheXls.Locate("#T");
			while (cell != null) {
				FoundValue = Dispatch.get(cell, "Value").toString();
				CheckPosBean aCheckPosDate = new CheckPosBean(); 
				aCheckPosDate.startX = Integer.parseInt(XlsUtil.getExcelColNo(cell));
				aCheckPosDate.startY = Integer.parseInt(XlsUtil.getExcelRowNo(cell));
				String tagText = FoundValue.replaceAll("[^#]*#([^#]*)#[^#]*", "$1");
				String[] tags = tagText.split("\\[");
				for (int i=1 ; i< tags.length; i++) {
					String tag = tags[i];
					if (tag.startsWith("T")) {
						// 单元格中的跳动
						aCheckPosDate.shiftX = Integer.parseInt(tag.substring(1));
					}
				}
				aCheckPosDate.content = tagText.substring(2, 3);
				checkPosDate.add(aCheckPosDate);
				cacheXls.SetValue(cell, "");
				cell = cacheXls.Locate("#T");
			}

			cell = cacheXls.Locate("#N");
			while (cell != null) {
				FoundValue = Dispatch.get(cell, "Value").toString();
				CheckPosBean aCheckPosName = new CheckPosBean(); 
				aCheckPosName.startX = Integer.parseInt(XlsUtil.getExcelColNo(cell));
				aCheckPosName.startY = Integer.parseInt(XlsUtil.getExcelRowNo(cell));
				String tagText = FoundValue.replaceAll("[^#]*#([^#]*)#[^#]*", "$1");
				String[] tags = tagText.split("\\[");
				aCheckPosName.shiftY = 0;
				for (String tag : tags) {
					if (tag.startsWith("T")) {
						// 单元格中的跳动
						aCheckPosName.shiftX = Integer.parseInt(tag.substring(1));
					} else if (tag.startsWith("U")) {
						aCheckPosName.shiftY = Integer.parseInt(tag.substring(1));
					}
				}
				checkPosName.add(aCheckPosName);
				cacheXls.SetValue(cell, "");
				cell = cacheXls.Locate("#N");
			}

			// 所有设备一览
//			if ("53".equals(check_file_manage_id)) {
//				// 电烙铁工具
//				setDeviceElectricIron(cacheXls, sEncodedDeviceList, axis, fileAxisType, 
//						cfmEntity, crMapper, monCal, conn);
//			} else if ("98".equals(check_file_manage_id)) {
//				// 力矩工具
//				setDeviceTrosion(cacheXls, sEncodedDeviceList, axis, fileAxisType, 
//						cfmEntity, crMapper, monCal, check_file_manage_id, conn);
//			} else {
				// 普通设备工具
				setDeviceNormal(cacheXls, sEncodedDeviceList, checkPosData, checkPosManageNo, checkPosModel, 
						checkDeviceName, checkUseStart, checkUseEnd, checkPosDate, checkPosName, axis, fileAxisType, 
						cfmEntity, crMapper, monCal, conn);
//			}

			// #P QR-B31002-12A_B038_147P12月
			// 取得参照信息<refer
			cell = cacheXls.Locate("#P");
			FoundValue = null;
			List<CheckResultEntity> refers = null;			
			if (cell != null) {
				FoundValue = Dispatch.get(cell, "Value").toString();

				CheckResultEntity cre = new CheckResultEntity();
				cre.setCheck_confirm_time_start(cfsEntity.getStart_record_date());
				cre.setCheck_confirm_time_end(cfsEntity.getFiling_date());
				cre.setCheck_file_manage_id(cfsEntity.getCheck_file_manage_id());
				cre.setManage_id(sEncodedDeviceList.get(0));
				refers = crMapper.getDeviceReferInPeriod(cre);
			}
			while (FoundValue != null) {
				int vIdex = FoundValue.indexOf("[I");
				if (vIdex < 0) {
					vIdex = FoundValue.indexOf("[C");
				}
				String seq = FoundValue.substring(vIdex + 2, vIdex + 4);
				if (refers.size() > 0) {
					for (CheckResultEntity refer : refers) {
						if (seq.equals(refer.getItem_seq())) {
							if (FoundValue.indexOf("#P[I") >= 0) {
								// 输入
								FoundValue = FoundValue.replaceAll("#P\\[I"+seq+"[^#]*#", getNoScale(refer.getDigit()));
							} if (FoundValue.indexOf("#P[C") >= 0) {
								// 选择项
								String choosedValue = getNoScale(refer.getDigit());
								FoundValue = FoundValue.replaceAll("#P\\[C"+seq+"[^#]*\\[V"+choosedValue+"[^#]*#", "☑"); // √
								FoundValue = FoundValue.replaceAll("#P\\[C"+seq+"[^#]*\\[V[^#]*#", "□");
							}
							cacheXls.SetValue(cell, FoundValue);
							break;
						}
					}
				} else {
					cacheXls.SetValue(cell, "");
				}

				FoundValue = null;
				cell = cacheXls.Locate("#P");
				if (cell != null) FoundValue = Dispatch.get(cell, "Value").toString();
			}

//			cacheXls.Replace(source, target);
//			"#[^#]*#"
			File fTargetPath = new File(targetPath);
			if (!fTargetPath.exists()) {
				fTargetPath.mkdirs();
			}
			cacheXls.SaveAsPdf(targetFile); // SaveAsPdf
			cacheXls.Release();

		} catch (Exception e) {
			_logger.error(e.getMessage(), e);
			if (cacheXls != null) {
				cacheXls.CloseExcel(false);
			}
		} finally {
			cacheXls = null;
		}

		// 查询备注
		// 备注信息
		List<Map<String, String>> comments = new ArrayList<Map<String, String>>();
		DevicesManageEntity conditionOfDevice = new DevicesManageEntity();
		conditionOfDevice.setProvide_date_start(cfsEntity.getStart_record_date());
		conditionOfDevice.setProvide_date_end(cfsEntity.getFiling_date());
		CheckResultEntity conditionOfComment = new CheckResultEntity();
		conditionOfComment.setCheck_confirm_time_start(cfsEntity.getStart_record_date());
		conditionOfComment.setCheck_confirm_time_end(cfsEntity.getFiling_date());
		for (int iDev = 0; iDev < sEncodedDeviceList.size(); iDev ++) {
			// 取得管理设备信息
			String devices_manage_id = sEncodedDeviceList.get(iDev);
			conditionOfDevice.setDevices_manage_id(devices_manage_id);
			DevicesManageEntity provide_date = dmMapper.checkProvideInPeriod(conditionOfDevice);
			DevicesManageEntity waste_date = dmMapper.checkWasteInPeriod(conditionOfDevice);
			// 发布日期
			if (provide_date != null) {
				Map<String, String> comment = new HashMap<String, String>();
				comment.put("manage_code", provide_date.getManage_code());
				comment.put("job_no", provide_date.getProvider());
				comment.put("comment", ApplicationMessage.WARNING_MESSAGES.getMessage("info.infect.device.filing.provide", 
						provide_date.getManage_code(), provide_date.getProcess_code()));
				comment.put("comment_date", DateUtil.toString(provide_date.getProvide_date(), DateUtil.ISO_DATE_PATTERN));
				comments.add(comment);
			}
			// 废弃日期
			if (waste_date != null) {
				Map<String, String> comment = new HashMap<String, String>();
				comment.put("manage_code", waste_date.getManage_code());
				comment.put("job_no", waste_date.getProvider());
				comment.put("comment", ApplicationMessage.WARNING_MESSAGES.getMessage("info.infect.device.filing.waste", 
						waste_date.getManage_code(), waste_date.getProcess_code()));
				comment.put("comment_date", DateUtil.toString(waste_date.getWaste_date(), DateUtil.ISO_DATE_PATTERN));
				comments.add(comment);
			}
			// 备注信息\
			conditionOfComment.setManage_id(devices_manage_id);
			List<CheckResultEntity> commentsList = crMapper.getDeviceCheckCommentInPeriodByManageId(conditionOfComment);
			for (CheckResultEntity cre : commentsList) {
				Map<String, String> comment = new HashMap<String, String>();
				comment.put("manage_code", cre.getManage_code());
				comment.put("job_no", cre.getJob_no());
				comment.put("comment", cre.getComment());
				comment.put("comment_date", DateUtil.toString(cre.getCheck_confirm_time(), DateUtil.ISO_DATE_PATTERN));
				comments.add(comment);
			}
		}

		if (comments.size() > 0) {
			// 取得点检表信息
			String templateCommentFileXls = PathConsts.BASE_PATH + PathConsts.REPORT_TEMPLATE + "\\点检备注.xlsx";
			FileUtils.copyFile(new File(templateCommentFileXls), new File(cachePath + "_comment.xls"));

			String targetCommentFileXls = targetPath + "\\" + cfsEntity.getStorage_file_name() + "_comment.pdf";
			cacheXls = null;
			try {
				cacheXls = new XlsUtil(cachePath + "_comment.xls", false);
				cacheXls.SelectActiveSheet();

				int setLine = 5; // Const
				for (Map<String, String> comment : comments) {
					cacheXls.SetValue("B" + setLine, comment.get("manage_code"));
					cacheXls.SetValue("C" + setLine, comment.get("comment_date"));
					cacheXls.sign(PathConsts.BASE_PATH + PathConsts.IMAGES + "\\sign\\" + comment.get("job_no").toUpperCase(), "D" + setLine);
					cacheXls.SetValue("E" + setLine, comment.get("comment"));
					setLine +=2;
				}

//				/ 保存到 PDF
				cacheXls.SaveAsPdf(targetCommentFileXls);
			} catch (Exception e) {
				_logger.error(e.getMessage(), e);
				if (cacheXls != null) {
					cacheXls.CloseExcel(false);
				}
			} finally {
				cacheXls = null;
			}
			// PDF 合并
			joinPdf(targetFile, targetCommentFileXls);
		}
	}

	private void joinPdf(String thisFilePath, String appedixFilePath) {
		// 用iText合并文件
		try {
			PdfReader reader;
			Document document = new Document();

			FileUtils.copyFile(new File(thisFilePath), new File(thisFilePath + "_body.pdf"));

			PdfCopy copy = new PdfCopy(document, new FileOutputStream(thisFilePath));
			document.open();

			PdfImportedPage newPage;

			reader = new PdfReader(thisFilePath+ "_body.pdf");
			int iPageNum = reader.getNumberOfPages();
			for (int j = 1; j <= iPageNum; j++) {
				document.newPage();
				newPage = copy.getImportedPage(reader, j);

				copy.addPage(newPage);
			}

			reader = new PdfReader(appedixFilePath);
			iPageNum = reader.getNumberOfPages();
			for (int j = 1; j <= iPageNum; j++) {
				document.newPage();
				newPage = copy.getImportedPage(reader, j);

				copy.addPage(newPage);
			}

			document.close();

			reader.close();

			new File(appedixFilePath).delete();
			new File(thisFilePath+ "_body.pdf").delete();

		} catch (Exception de) {
			System.out.println("rvsreport_" + de.getMessage());
		}
	}

	private static final int INSERT_START_ROW_FOR_EI =4;
	private static final int INSERT_START_ROW_FOR_TR =6;
	private void setDeviceElectricIron(XlsUtil cacheXls,
			List<String> sEncodedDeviceList, int axis, int axisType,
			CheckFileManageEntity cfmEntity, CheckResultMapper crMapper,
			Calendar monCal, SqlSession conn) {
		String check_file_manage_id = ELECTRIC_IRON_FILE_A;

		Date monthStart = monCal.getTime();
		// 月底
		Calendar cMonthEnd = Calendar.getInstance();
		cMonthEnd.setTimeInMillis(monCal.getTimeInMillis());
		cMonthEnd.add(Calendar.MONTH, 1);
		cMonthEnd.add(Calendar.DATE, -1);
		Date monthEnd = cMonthEnd.getTime();

		ElectricIronDeviceMapper eidMapper = conn.getMapper(ElectricIronDeviceMapper.class);
		int insertRow = INSERT_START_ROW_FOR_EI;

		for (int iDev = 0; iDev < sEncodedDeviceList.size(); iDev ++) {
			// 取得管理设备信息
			String devices_manage_id = sEncodedDeviceList.get(iDev);
			// DevicesManageEntity dmEntity = dmMapper.getByKey(devices_manage_id);

			CheckResultEntity enti = new CheckResultEntity();
			enti.setManage_id(devices_manage_id);
			enti.setCheck_file_manage_id(check_file_manage_id);

			enti.setCheck_confirm_time_start(monthStart);
			enti.setCheck_confirm_time_end(monthEnd);

			List<CheckResultEntity> lMonth = crMapper.getDeviceCheckInPeriod(enti);

			ElectricIronDeviceEntity eidCond = new ElectricIronDeviceEntity();
			eidCond.setManage_id(devices_manage_id);
			List<ElectricIronDeviceEntity> rsts = eidMapper.searchElectricIronDevice(eidCond);

			if (rsts.size() == 1) {
				// 单温
				// Sheets("Sheet2").Select
				cacheXls.getAndActiveSheetBySeq(2);

				// Rows("1:3").Select
				Dispatch selection = cacheXls.Select("1:2");
				// Selection.Copy
				Dispatch.call(selection, "Copy");

				// Sheets("Sheet1").Select
				cacheXls.getAndActiveSheetBySeq(1);
				// Rows("4:4").Select
				selection = cacheXls.Select(insertRow + ":" + insertRow);
				// Selection.Insert Shift:=xlDown
				Dispatch.call(selection, "Insert", new Variant(1));

				// 管理编号
				cacheXls.SetValue("A" + insertRow, rsts.get(0).getManage_code());
				// 类型
				cacheXls.SetValue("A" + (insertRow+1), CodeListUtils.getValue("electric_iron_kind_simple", "" + rsts.get(0).getKind()));

//				// 接地
//				if (val50.getDigit() != null)
//					cacheXls.SetValue("C" + insertRow, getNoScale(val50.getDigit().toPlainString()) + "Ω");
//				// 绝缘电阻
//				if (val51.getDigit() != null)
//					cacheXls.SetValue("C" + (insertRow+1), getNoScale(val51.getDigit().toPlainString()) + "MΩ");

				// 上下限
				cacheXls.SetValue("D" + insertRow, rsts.get(0).getTemperature_lower_limit().toString());
				cacheXls.SetValue("F" + insertRow, rsts.get(0).getTemperature_upper_limit().toString());

				// 每个单元格取值
				for (int iAxis=0;iAxis<=axis;iAxis++) {
					Date[] dates = getDayOfAxis(monCal, iAxis, axisType, cfmEntity.getCycle_type());

					CheckResultEntity cre = new CheckResultEntity();
					cre.setCheck_confirm_time_start(dates[0]);
					cre.setCheck_confirm_time_end(dates[1]);
					cre.setManage_id(devices_manage_id);
					cre.setCheck_file_manage_id(check_file_manage_id);

					// 已点检或之前范围
					List<CheckResultEntity> listCre = crMapper.getDeviceCheckInPeriod(cre);
					String jobNo = null;
					Dispatch cell = null;
					// 取得已点检单元格信息
					if (listCre != null && listCre.size() > 0) {
						for (CheckResultEntity rCre : listCre) {
							String itemSeq = rCre.getItem_seq();
							if (!itemSeq.equals(rsts.get(0).getSeq())) {
								break;
							}

							String cellName = XlsUtil.getExcelColCode(9 + iAxis - 1) 
									+ (insertRow);
							cell = cacheXls.getRange(cellName);
							cacheXls.SetValue(cell, getFileStatusD(""+rCre.getChecked_status(), rCre.getDigit()));

							if (jobNo == null) jobNo = rCre.getJob_no();
						}
					} else {
						String cellName = XlsUtil.getExcelColCode(9 + iAxis - 1) 
								+ (insertRow);
						cell = cacheXls.getRange(cellName);
						cacheXls.SetValue(cell, "/");
					}

					// 签章
					if (!isEmpty(jobNo)) {
						
						String cellName = XlsUtil.getExcelColCode(9 + iAxis - 1) 
								+ (insertRow + 1);
						cell = cacheXls.getRange(cellName);
						if (cfmEntity.getAccess_place() == 1) {
							cacheXls.sign(PathConsts.BASE_PATH + PathConsts.IMAGES + "\\sign_v\\" + jobNo.toUpperCase(), cell);
						} else {
							cacheXls.sign(PathConsts.BASE_PATH + PathConsts.IMAGES + "\\sign\\" + jobNo.toUpperCase(), cell);
						}
					}
				}

				insertRow += 2;

			} else if (rsts.size() == 2) {
				// 双温
				cacheXls.getAndActiveSheetBySeq(2);

				Dispatch selection = cacheXls.Select("5:7");
				Dispatch.call(selection, "Copy");

				cacheXls.getAndActiveSheetBySeq(1);
				selection = cacheXls.Select(insertRow + ":" + insertRow);
				Dispatch.call(selection, "Insert", new Variant(1));

				// 管理编号
				cacheXls.SetValue("A" + insertRow, rsts.get(0).getManage_code());
				// 类型
				cacheXls.SetValue("A" + (insertRow+1), CodeListUtils.getValue("electric_iron_kind_simple", "" + rsts.get(0).getKind()));

//				// 接地
//				cacheXls.SetValue("C" + insertRow, getNoScale(val50.getDigit().toPlainString()) + "Ω");
//				// 绝缘电阻
//				cacheXls.SetValue("C" + (insertRow+2), getNoScale(val51.getDigit().toPlainString()) + "MΩ");

				// 上下限
				cacheXls.SetValue("D" + insertRow, rsts.get(0).getTemperature_lower_limit().toString());
				cacheXls.SetValue("F" + insertRow, rsts.get(0).getTemperature_upper_limit().toString());
				cacheXls.SetValue("D" + (insertRow+1), rsts.get(1).getTemperature_lower_limit().toString());
				cacheXls.SetValue("F" + (insertRow+1), rsts.get(1).getTemperature_upper_limit().toString());

				// 每个单元格取值
				for (int iAxis=0;iAxis<=axis;iAxis++) {
					Date[] dates = getDayOfAxis(monCal, iAxis, axisType, cfmEntity.getCycle_type());

					CheckResultEntity cre = new CheckResultEntity();
					cre.setCheck_confirm_time_start(dates[0]);
					cre.setCheck_confirm_time_end(dates[1]);
					cre.setManage_id(devices_manage_id);
					cre.setCheck_file_manage_id(check_file_manage_id);

					// 已点检或之前范围
					List<CheckResultEntity> listCre = crMapper.getDeviceCheckInPeriod(cre);
					String jobNo = null;
					Dispatch cell = null;
					// 取得已点检单元格信息
					if (listCre != null && listCre.size() > 0) {
						for (CheckResultEntity rCre : listCre) {
							String itemSeq = rCre.getItem_seq();
							int rowShift = 0;
							if (itemSeq.equals(rsts.get(1).getSeq())) {
								rowShift = 1;
							}
							else if (itemSeq.equals(rsts.get(0).getSeq())) {
								rowShift = 0;
							} else break;

							String cellName = XlsUtil.getExcelColCode(9 + iAxis - 1) 
									+ (insertRow + rowShift);
							cell = cacheXls.getRange(cellName);
							cacheXls.SetValue(cell, getFileStatusD(""+rCre.getChecked_status(), rCre.getDigit()));

							if (jobNo == null) jobNo = rCre.getJob_no();
						}
					} else {
						String cellName = XlsUtil.getExcelColCode(9 + iAxis - 1) 
								+ (insertRow);
						cell = cacheXls.getRange(cellName);
						cacheXls.SetValue(cell, "/");
						cellName = XlsUtil.getExcelColCode(9 + iAxis - 1) 
								+ (insertRow + 1);
						cell = cacheXls.getRange(cellName);
						cacheXls.SetValue(cell, "/");
					}

					// 签章
					if (!isEmpty(jobNo)) {
						
						String cellName = XlsUtil.getExcelColCode(9 + iAxis - 1) 
								+ (insertRow + 2);
						cell = cacheXls.getRange(cellName);
						if (cfmEntity.getAccess_place() == 1) {
							cacheXls.sign(PathConsts.BASE_PATH + PathConsts.IMAGES + "\\sign_v\\" + jobNo.toUpperCase(), cell);
						} else {
							cacheXls.sign(PathConsts.BASE_PATH + PathConsts.IMAGES + "\\sign\\" + jobNo.toUpperCase(), cell);
						}
					}
				}

				insertRow += 3;
			}
		}
	}

	/**
	 * 设定力矩工具表格
	 * 
	 * @param cacheXls
	 * @param sEncodedDeviceList
	 * @param axis
	 * @param axisType
	 * @param cfmEntity
	 * @param crMapper
	 * @param monCal
	 * @param conn
	 */
	private void setDeviceTrosion(XlsUtil cacheXls,
			List<String> sEncodedDeviceList, int axis, int axisType,
			CheckFileManageEntity cfmEntity, CheckResultMapper crMapper,
			Calendar monCal, String check_file_manage_id, SqlSession conn) {


		TorsionDeviceMapper tdMapper = conn.getMapper(TorsionDeviceMapper.class);
		int insertRow = INSERT_START_ROW_FOR_TR;

		for (int iDev = 0; iDev < sEncodedDeviceList.size(); iDev ++) {
			// 取得管理设备信息
			String devices_manage_id = sEncodedDeviceList.get(iDev);
			// DevicesManageEntity dmEntity = dmMapper.getByKey(devices_manage_id);

			TorsionDeviceEntity tdCond = new TorsionDeviceEntity();
			tdCond.setManage_id(devices_manage_id);
			List<TorsionDeviceEntity> rsts = tdMapper.searchTorsionDevice(tdCond);

			int rstsSize = rsts.size();
			if (rstsSize == 1) {

				// Sheets("Sheet2").Select
				cacheXls.getAndActiveSheetBySeq(2);

				// Rows("1:3").Select
				Dispatch selection = cacheXls.Select("2:4");
				// Selection.Copy
				Dispatch.call(selection, "Copy");

				// Sheets("Sheet1").Select
				cacheXls.getAndActiveSheetBySeq(1);
				// Rows("4:4").Select
				selection = cacheXls.Select(insertRow + ":" + insertRow);
				// Selection.Insert Shift:=xlDown
				Dispatch.call(selection, "Insert", new Variant(1));
				TorsionDeviceEntity trTorsion = rsts.get(0);

				// 规格力矩值 BASE
				cacheXls.SetValue("A" + insertRow, getNoScale(trTorsion.getRegular_torque()));
				// 规格力矩值 DIFF
				cacheXls.SetValue("C" + insertRow, getNoScale(trTorsion.getDeviation()));
				// 使用的工程
				cacheXls.SetValue("E" + insertRow, trTorsion.getUsage_point());
				// HP-10 HP-100
				cacheXls.SetValue("F" + insertRow, CodeListUtils.getValue("torsion_device_hp_scale", ""+trTorsion.getHp_scale(), ""));
				// 点检力矩[N·m]下限
				cacheXls.SetValue("G" + insertRow, trTorsion.getRegular_torque_lower_limit().toPlainString());
				// 点检力矩[N·m]上限
				cacheXls.SetValue("I" + insertRow, trTorsion.getRegular_torque_upper_limit().toPlainString());
				// 管理编号
				cacheXls.SetValue("J" + insertRow, trTorsion.getManage_code());

				// 每个单元格取值
				for (int iAxis=0;iAxis<=axis;iAxis++) {
					Date[] dates = getDayOfAxis(monCal, iAxis, axisType, cfmEntity.getCycle_type());

					CheckResultEntity cre = new CheckResultEntity();
					cre.setCheck_confirm_time_start(dates[0]);
					cre.setCheck_confirm_time_end(dates[1]);
					cre.setManage_id(devices_manage_id);
					cre.setCheck_file_manage_id(check_file_manage_id);

					// 已点检或之前范围
					List<CheckResultEntity> listCre = crMapper.getDeviceCheckInPeriod(cre);
					Dispatch cell = null;
					// 取得已点检单元格信息
					if (listCre != null && listCre.size() > 0) {
						String jobNo = null;
						Date getDate = null;

						for (CheckResultEntity rCre : listCre) {
							String itemSeq = rCre.getItem_seq();
							if (!itemSeq.equals(rsts.get(0).getSeq())) {
								break;
							}

							String cellName = XlsUtil.getExcelColCode(12 + iAxis - 1) 
									+ (insertRow);
							cell = cacheXls.getRange(cellName);
							cacheXls.SetValue(cell, getFileStatusD(""+rCre.getChecked_status(), rCre.getDigit()));

							if (jobNo == null) jobNo = rCre.getJob_no();
							if (getDate == null) getDate = rCre.getCheck_confirm_time();
						}

						// 日期行
						if (getDate != null) {
							String cellName = XlsUtil.getExcelColCode(12 + iAxis - 1) 
									+ (insertRow + 1);
							cell = cacheXls.getRange(cellName);
							cacheXls.SetValue(cell, DateUtil.toString(getDate, "MM-dd"));
							cacheXls.SetNumberFormatLocal(cell, "m-d");
						}

						// 签章行
						if (!isEmpty(jobNo)) {
							
							String cellName = XlsUtil.getExcelColCode(12 + iAxis - 1) 
									+ (insertRow + 2);
							cell = cacheXls.getRange(cellName);
							if (cfmEntity.getAccess_place() == 1) {
								cacheXls.sign(PathConsts.BASE_PATH + PathConsts.IMAGES + "\\sign_v\\" + jobNo.toUpperCase(), cell);
							} else {
								cacheXls.sign(PathConsts.BASE_PATH + PathConsts.IMAGES + "\\sign\\" + jobNo.toUpperCase(), cell);
							}
						}
					} else {
						String cellName = XlsUtil.getExcelColCode(12 + iAxis - 1) 
								+ (insertRow);
						cell = cacheXls.getRange(cellName);
						cacheXls.SetValue(cell, "/");
					}
				}

				insertRow += 3;
			} else if (rstsSize > 1) {
				// Sheets("Sheet2").Select
				cacheXls.getAndActiveSheetBySeq(2);

				Dispatch selection = cacheXls.Select("6:6");
				Dispatch.call(selection, "Copy");
				cacheXls.getAndActiveSheetBySeq(1);
				selection = cacheXls.Select(insertRow + ":" + insertRow);
				Dispatch.call(selection, "Insert", new Variant(1));

				int iRowPlus = 1;
				for ( ; iRowPlus<rstsSize;iRowPlus++) {
					cacheXls.getAndActiveSheetBySeq(2);
					selection = cacheXls.Select("7:7");
					Dispatch.call(selection, "Copy");
					cacheXls.getAndActiveSheetBySeq(1);
					selection = cacheXls.Select((insertRow + iRowPlus) + ":" + (insertRow + iRowPlus));
					Dispatch.call(selection, "Insert", new Variant(1));
				}

				cacheXls.getAndActiveSheetBySeq(2);
				selection = cacheXls.Select("8:9");
				Dispatch.call(selection, "Copy");
				cacheXls.getAndActiveSheetBySeq(1);
				selection = cacheXls.Select((insertRow + iRowPlus) + ":" + (insertRow + iRowPlus));
				Dispatch.call(selection, "Insert", new Variant(1));

				for (int j=0;j< rstsSize;j++) {
					TorsionDeviceEntity trTorsion = rsts.get(j);
					// 规格力矩值 BASE
					cacheXls.SetValue("A" + (insertRow + j), getNoScale(trTorsion.getRegular_torque()));
					// 规格力矩值 DIFF
					cacheXls.SetValue("C" + (insertRow + j), getNoScale(trTorsion.getDeviation()));
					// HP-10 HP-100
					cacheXls.SetValue("F" + (insertRow + j), CodeListUtils.getValue("torsion_device_hp_scale", ""+trTorsion.getHp_scale(), ""));
					// 点检力矩[N·m]下限
					cacheXls.SetValue("G" + (insertRow + j), trTorsion.getRegular_torque_lower_limit().toPlainString());
					// 点检力矩[N·m]上限
					cacheXls.SetValue("I" + (insertRow + j), trTorsion.getRegular_torque_upper_limit().toPlainString());
					if (j==0) {
						// 使用的工程
						cacheXls.SetValue("E" + insertRow, trTorsion.getUsage_point());
						// 管理编号
						cacheXls.SetValue("J" + insertRow, trTorsion.getManage_code());
					}

					// 每个单元格取值
					for (int iAxis=0;iAxis<=axis;iAxis++) {
						Date[] dates = getDayOfAxis(monCal, iAxis, axisType, cfmEntity.getCycle_type());

						CheckResultEntity cre = new CheckResultEntity();
						cre.setCheck_confirm_time_start(dates[0]);
						cre.setCheck_confirm_time_end(dates[1]);
						cre.setManage_id(devices_manage_id);
						cre.setItem_seq(trTorsion.getSeq());
						cre.setCheck_file_manage_id(check_file_manage_id);

						// 已点检或之前范围
						List<CheckResultEntity> listCre = crMapper.getTorsionDeviceCheckInPeriod(cre);
						Dispatch cell = null;

						// 取得已点检单元格信息
						if (listCre != null && listCre.size() > 0) {

							String jobNo = null;
							Date getDate = null;

							for (CheckResultEntity rCre : listCre) {
								String cellName = XlsUtil.getExcelColCode(12 + iAxis - 1) 
										+ (insertRow + j);
								cell = cacheXls.getRange(cellName);
								cacheXls.SetValue(cell, getFileStatusD(""+rCre.getChecked_status(), rCre.getDigit()));
	
								if (jobNo == null) jobNo = rCre.getJob_no();
								if (getDate == null) getDate = rCre.getCheck_confirm_time();
							}

							// 日期行
							if (getDate != null) {
								String cellName = XlsUtil.getExcelColCode(12 + iAxis - 1) 
										+ (insertRow + rstsSize);
								cell = cacheXls.getRange(cellName);
								cacheXls.SetValue(cell, DateUtil.toString(getDate, "MM-dd"));
								cacheXls.SetNumberFormatLocal(cell, "m-d");
							}

							// 签章行
							if (!isEmpty(jobNo)) {
								
								String cellName = XlsUtil.getExcelColCode(12 + iAxis - 1) 
										+ (insertRow + rstsSize + 1);
								cell = cacheXls.getRange(cellName);
								if (cfmEntity.getAccess_place() == 1) {
									cacheXls.sign(PathConsts.BASE_PATH + PathConsts.IMAGES + "\\sign_v\\" + jobNo.toUpperCase(), cell);
								} else {
									cacheXls.sign(PathConsts.BASE_PATH + PathConsts.IMAGES + "\\sign\\" + jobNo.toUpperCase(), cell);
								}
							}
						} else {
							String cellName = XlsUtil.getExcelColCode(12 + iAxis - 1) 
									+ (insertRow + j);
							cell = cacheXls.getRange(cellName);
							cacheXls.SetValue(cell, "/");
						}
					}
				}

				insertRow += (2 + rstsSize);
			}
		}
	}

	private void setDeviceNormal(XlsUtil cacheXls, List<String> sEncodedDeviceList,
			Map<String, CheckPosBean> checkPosData, CheckPosBean checkPosManageNo, CheckPosBean checkPosModel, 
			CheckPosBean checkDeviceName, CheckPosBean checkUseStart, CheckPosBean checkUseEnd, 
			List<CheckPosBean> checkPosDate, List<CheckPosBean> checkPosName, int axis, int axisType, 
			CheckFileManageEntity cfmEntity, CheckResultMapper crMapper, Calendar monCal,
			SqlSession conn) {
		DevicesManageMapper dmMapper = conn.getMapper(DevicesManageMapper.class);

		for (int iDev = 0; iDev < sEncodedDeviceList.size(); iDev ++) {
			// 取得管理设备信息
			DevicesManageEntity dmEntity = dmMapper.getByKey(sEncodedDeviceList.get(iDev));

			String cellName = null;
			Dispatch cell = null;

			// 设备管理编号
			if (checkPosManageNo.startX > 0 && checkPosManageNo.shiftY > 0) {
				cellName = XlsUtil.getExcelColCode(checkPosManageNo.startX - 1) 
						+ (checkPosManageNo.startY + checkPosManageNo.shiftY * iDev);
				cell = cacheXls.getRange(cellName);
				cacheXls.SetValue(cell, dmEntity.getManage_code());
			}

			// 型号
			if (!isEmpty(checkPosModel.content)) {
				cellName = XlsUtil.getExcelColCode(checkPosModel.startX - 1) 
						+ (checkPosModel.startY + checkPosModel.shiftY * iDev);
				cell = cacheXls.getRange(cellName);
				cacheXls.SetValue(cell, dmEntity.getModel_name());
			}
			// 名称
			if (!isEmpty(checkDeviceName.content)) {
				cellName = XlsUtil.getExcelColCode(checkDeviceName.startX - 1) 
						+ (checkDeviceName.startY + checkDeviceName.shiftY * iDev);
				cell = cacheXls.getRange(cellName);
				cacheXls.SetValue(cell, dmEntity.getName());
			}

			if (!isEmpty(checkUseStart.content)) {

				Calendar calUse = Calendar.getInstance();
				if (dmEntity.getImport_date()!=null) {
					calUse.setTime(dmEntity.getImport_date());
				}

				cellName = XlsUtil.getExcelColCode(checkUseStart.startX - 1) 
						+ (checkUseStart.startY + checkUseStart.shiftY * iDev);
				cell = cacheXls.getRange(cellName);
				cacheXls.SetValue(cell, DateUtil.toString(dmEntity.getImport_date(), "yyyy年 M月 d日"));

				if (!isEmpty(checkUseEnd.content)) {
					int iExpiration = checkUseEnd.shiftX;
					calUse.add(Calendar.MONTH, iExpiration);
					calUse.add(Calendar.DATE, -1);

					cellName = XlsUtil.getExcelColCode(checkUseEnd.startX - 1) 
							+ (checkUseEnd.startY + checkUseEnd.shiftY * iDev);
					cell = cacheXls.getRange(cellName);
					cacheXls.SetValue(cell, DateUtil.toString(calUse.getTime(), "yyyy年 M月 d日"));
				}
			}

			// 每个单元格取值
			for (int iAxis=0;iAxis<=axis;iAxis++) {
				Date[] dates = getDayOfAxis(monCal, iAxis, axisType, cfmEntity.getCycle_type());

				CheckResultEntity cre = new CheckResultEntity();
				cre.setCheck_confirm_time_start(dates[0]);
				cre.setCheck_confirm_time_end(dates[1]);
				cre.setManage_id(dmEntity.getDevices_manage_id());
				cre.setCheck_file_manage_id(cfmEntity.getCheck_file_manage_id());

				// 已点检或之前范围
				List<CheckResultEntity> listCre = crMapper.getDeviceCheckInPeriod(cre);
				String jobNo = null;
				Date checkedDate = new Date(0);

				// 预先全划掉
				for (String itemSeq : checkPosData.keySet()) {
					CheckPosBean checkPos = checkPosData.get(itemSeq);
					int shift = iAxis;
					cellName = XlsUtil.getExcelColCode(checkPos.startX + shift * checkPos.shiftX - 1) 
							+ (checkPos.startY + checkPos.shiftY * iDev);
					cell = cacheXls.getRange(cellName);
					cacheXls.SetValue(cell, "/");
				}

				// 取得已点检单元格信息
				if (listCre != null && listCre.size() > 0) {
					for (CheckResultEntity rCre : listCre) {
						String itemSeq = rCre.getItem_seq();

						CheckPosBean checkPos = checkPosData.get(itemSeq);
						int shift = iAxis;

//						// TODO 特殊对应 日常里的月
//						if (CheckFileManageEntity.ACCESS_PLACE_DAILY == cfmEntity.getAccess_place() && 
//								rCre.getCycle_type() == TYPE_MONTH_OF_YEAR) {
//							shift = 0;
//						}
//
						cellName = XlsUtil.getExcelColCode(checkPos.startX + shift * checkPos.shiftX - 1) 
								+ (checkPos.startY + checkPos.shiftY * iDev);
						cell = cacheXls.getRange(cellName);
						cacheXls.SetValue(cell, checkPos.content.replaceAll("#tag#", getFileStatusD(""+rCre.getChecked_status(), rCre.getDigit())));

						if (jobNo == null) jobNo = rCre.getJob_no();
						// 得到点检最终完成时间
						Date dCheckConfirmTime = rCre.getCheck_confirm_time();
						if (dCheckConfirmTime.after(checkedDate)) {
							checkedDate = dCheckConfirmTime;
						}
					}
				}

				for (CheckPosBean cpd : checkPosDate) {
					if (!isEmpty(cpd.content)) {
						cellName = XlsUtil.getExcelColCode(cpd.startX + iAxis * cpd.shiftX - 1) 
								+ cpd.startY;
						cell = cacheXls.getRange(cellName);
						if (checkedDate.getTime() > 0) {
							cacheXls.SetValue(cell, DateUtil.toString(checkedDate, "y-M-d"));
							if ("D".equalsIgnoreCase(cpd.content)) {
								cacheXls.SetNumberFormatLocal(cell, "m-d");
							} else if ("C".equalsIgnoreCase(cpd.content)) {
								cacheXls.SetNumberFormatLocal(cell, "m月d日");
							}
						} else {
//							String isTag = cacheXls.GetValue(cellName);
//							cacheXls.SetValue(cell, "");
						}
					}
				}

				// 签章
				if (!isEmpty(jobNo)) {
					for (CheckPosBean cpn : checkPosName) {
						if (checkPosManageNo.shiftY > 0) {
							cellName = XlsUtil.getExcelColCode(cpn.startX + iAxis * cpn.shiftX - 1) 
									+ (cpn.startY  + cpn.shiftY * iDev);
						} else {
							cellName = XlsUtil.getExcelColCode(cpn.startX + iAxis * cpn.shiftX - 1) 
									+ cpn.startY;
						}
						cell = cacheXls.getRange(cellName);
						String Taged = cacheXls.GetValue(cellName);
						if ("已千千千千千千千".equals(Taged)) {
							continue;
						}
						if (checkPosManageNo.shiftY > 0 || (iDev == 0)) {
							if (cfmEntity.getAccess_place() == 1) {
								cacheXls.sign(PathConsts.BASE_PATH + PathConsts.IMAGES + "\\sign_v\\" + jobNo.toUpperCase(), cell);
							} else {
								cacheXls.sign(PathConsts.BASE_PATH + PathConsts.IMAGES + "\\sign\\" + jobNo.toUpperCase(), cell);
							}
							cacheXls.SetValue(cell, "已千千千千千千千");
						}
					}
				}
			}
		}
		cacheXls.Replace("已千千千千千千千", "");
	}

	private static final int INSERT_START_ROW_FOR_JIG =6;
	private static final int INSERT_START_COL_FOR_JIG =6; // G

	/**
	 * 生成治具文档
	 */
//	public void makeFileJigs(CheckedFileStorageEntity cfsEntity,
//			List<String> sEncodedDeviceList, SqlSession conn) {
//		ToolsCheckResultMapper crMapper = conn.getMapper(ToolsCheckResultMapper.class);
//		JigManageMapper tmMapper = conn.getMapper(JigManageMapper.class);
//
//		Date filingDate = cfsEntity.getFiling_date();
//
//		Calendar adjustCal = Calendar.getInstance();
//		adjustCal.setTime(filingDate);
//		adjustCal.set(Calendar.HOUR_OF_DAY, 0);
//		adjustCal.set(Calendar.MINUTE, 0);
//		adjustCal.set(Calendar.SECOND, 0);
//		adjustCal.set(Calendar.MILLISECOND, 0);
//
//		// 复制模板到临时文件
//		String ext = ".xlsx";
//		String srcPath = PathConsts.BASE_PATH + PathConsts.DEVICEINFECTION + "\\专用工具定期清点保养记录模板.xlsx";
//		String cacheFilename =  cfsEntity.getStorage_file_name() + filingDate.getTime() + ext;
//		String cachePath = PathConsts.BASE_PATH + PathConsts.LOAD_TEMP + "\\" + DateUtil.toString(filingDate, "yyyyMM") + "\\" + cacheFilename;
//		try {
//			FileUtils.copyFile(new File(srcPath), new File(cachePath));
//		} catch (IOException e) {
//			_logger.error(e.getMessage(), e);
//			return;
//		}
//		String targetPath = PathConsts.BASE_PATH + PathConsts.INFECTIONS + "\\" + 
//				RvsUtils.getBussinessYearString(adjustCal) + "\\治具";
//		String targetFile = targetPath + "\\" + cfsEntity.getStorage_file_name() + ".pdf";
//
//		String section_id = cfsEntity.getSection_id();
//		String line_id = cfsEntity.getLine_id();
//		String position_id = cfsEntity.getPosition_id();
//
//		XlsUtil cacheXls = null;
//		try {
//			cacheXls = new XlsUtil(cachePath, false);
//			cacheXls.SelectActiveSheet();
//
//			// 取得本期
//			String bperiod = RvsUtils.getBussinessYearString(adjustCal);
//
//			// 工程
//			String sLineName = "";
//			SectionMapper sMapper = conn.getMapper(SectionMapper.class);
//			SectionEntity sEntity = sMapper.getSectionByID(section_id);
//			if (sEntity != null) {
//				sLineName += sEntity.getName() + "\n";
//			}
//
//			LineMapper lMapper = conn.getMapper(LineMapper.class);
//			LineEntity lEntity = lMapper.getLineByID(line_id);
//			if (lEntity != null) {
//				sLineName += lEntity.getName();
//			}
//			cacheXls.Replace("#G[LINE#", sLineName);
//
//			// 工位
//			PositionMapper pMapper = conn.getMapper(PositionMapper.class);
//			// 工位
//			PositionEntity pEntity = pMapper.getPositionByID(position_id);
//			if (pEntity != null) {
//				sLineName += pEntity.getProcess_code() + " ";
//			}
//			Dispatch positionCell = cacheXls.Locate("#G[POSITION#");
//			String FoundValue = Dispatch.get(positionCell, "Value").toString();
//			if (FoundValue.equals("#G[POSITION#")) {
//				cacheXls.SetValue(positionCell, sLineName);
//			} else {
//				cacheXls.Replace("#G[POSITION#", sLineName.replaceAll("\\\n", " "));
//			}
//
//			// 替换共通数据
//			cacheXls.Replace("#G[PERIOD#", bperiod);
//			cacheXls.Replace("#G[PERIODC#", bperiod.replaceAll("P", ""));
//
//			// 确定表单的归档类型
//			int axisType = TYPE_MONTH_OF_YEAR;
//
//			// 计算范围用日历
//			Calendar monCal = Calendar.getInstance();
//			// 去期间头
//			monCal = getStartOfPeriod(adjustCal);
//
//			int axis = getMaxAxis(axisType);
//
//			// 普通设备工具
//			setJig(cacheXls, sEncodedDeviceList, axis, axisType, cfsEntity, crMapper, tmMapper, monCal, conn);
//
//			File fTargetPath = new File(targetPath);
//			if (!fTargetPath.exists()) {
//				fTargetPath.mkdirs();
//			}
//			cacheXls.SaveAsPdf(targetFile); // SaveAsPdf
//			cacheXls.Release();
//
//		} catch (Exception e) {
//			_logger.error(e.getMessage(), e);
//			if (cacheXls != null) {
//				cacheXls.CloseExcel(false);
//			}
//		} finally {
//			cacheXls = null;
//		}
//	}

	/**
	 * 设定治具清点内容
	 * @param cacheXls
	 * @param sEncodedJigList 治具ID列表
	 * @param axis X坐标范围
	 * @param axisType
	 * @param cfsEntity 归档文件信息
	 * @param crMapper
	 * @param tmMapper
	 * @param monCal 去期间头
	 * @param conn
	 */
	private void setJig(XlsUtil cacheXls, List<String> sEncodedJigList,
			int axis, int axisType, 
			JigCheckResultMapper crMapper, JigManageMapper tmMapper, Calendar monCal, SqlSession conn) {
		// 循环填写各治具
		int insertRow = INSERT_START_ROW_FOR_JIG;

		for (int iJig = 0; iJig < sEncodedJigList.size(); iJig++) {
			insertRow++;

			String jig_id = sEncodedJigList.get(iJig);
			JigManageEntity tmEntity = tmMapper.getByKey(jig_id);

			String cellName = null;
			Dispatch cell = null;

			cacheXls.getAndActiveSheetBySeq(2);

			Dispatch selection = cacheXls.Select("1:2");
			Dispatch.call(selection, "Copy");

			// Sheets("Sheet1").Select
			cacheXls.getAndActiveSheetBySeq(1);
			// Rows("4:4").Select
			selection = cacheXls.Select(insertRow + ":" + insertRow);
			// Selection.Insert Shift:=xlDown
			Dispatch.call(selection, "Insert", new Variant(1));

			// No
			cacheXls.SetValue("A" + insertRow, "" + (iJig + 1));
			// 管理号码
			cacheXls.SetValue("B" + insertRow, getNoScale(tmEntity.getManage_code()));
			// 治具号码
			cacheXls.SetValue("C" + insertRow, getNoScale(tmEntity.getJig_no()));
			//专用工具名称
			cacheXls.SetValue("D" + insertRow, getNoScale(tmEntity.getJig_name()));
			// 备注 TODO
			// cacheXls.SetValue("S" + insertRow, getNoScale(tmEntity.getTools_name()));

			// 循环填写每月份 G->
			Calendar startCal = Calendar.getInstance();
			startCal.setTime(monCal.getTime());
			Calendar endcal = Calendar.getInstance();
			endcal.setTime(monCal.getTime());

			for (int iM = 0; iM < axis; iM++) {
				endcal.add(Calendar.MONTH, 1);
				JigCheckResultEntity condition = new JigCheckResultEntity();
				condition.setManage_id(jig_id);
				condition.setFirstDate(DateUtil.toString(startCal.getTime(), DateUtil.DATE_PATTERN));
				condition.setLastDate(DateUtil.toString(endcal.getTime(), DateUtil.DATE_PATTERN));
				List<JigCheckResultEntity> result = crMapper.searchCheckResult(condition);
				if (result.size() > 0) {
					String sCheckedStatus = result.get(0).getChecked_status();
					cacheXls.SetValue(XlsUtil.getExcelColCode(INSERT_START_COL_FOR_JIG + iM)
							+ insertRow, getNoScale(getFileStatusD(sCheckedStatus, null)));
				}
			}
			
		}
	}

	/**
	 * 工位检测未点检设备/治具
	 * @throws Exception 
	 * @throws NullPointerException 
	 */
	public void checkForPosition(String section_id, String position_id, String line_id, SqlSession conn) throws NullPointerException, Exception {
		CheckResultMapper crMapper = conn.getMapper(CheckResultMapper.class);
		CheckResultEntity condEntity = new CheckResultEntity();
		PeriodsEntity periodsEntity = getPeriodsOfDate(DateUtil.toString(new Date(), DateUtil.ISO_DATE_PATTERN), conn);
		try {
			getDevices(null, section_id, null, position_id, null, line_id, condEntity, periodsEntity, conn, crMapper, -1);

			getTorsionDevices(null, section_id, null, position_id, null, condEntity, periodsEntity, conn, crMapper, -1);
			getElectricIronDevices(null, section_id, null, position_id, null, condEntity, periodsEntity, conn, crMapper, -1);

		} catch(Exception tex) {
			_logger.error("dmmm:" + tex.getMessage(), tex);
		}
	}

	public String getPeripheralIsUseCheck(String manage_id, String device_type_id, String check_file_manage_id, SqlSession conn) throws ParseException {
		CheckResultMapper crMapper = conn.getMapper(CheckResultMapper.class);
		CheckResultEntity condEntity = new CheckResultEntity();
		condEntity.setManage_id(manage_id);
		//获取当前时间
		SimpleDateFormat df = new SimpleDateFormat(DateUtil.ISO_DATE_PATTERN);
		Calendar cal = Calendar.getInstance();

		// 允许时间
		int allowDays = 0;

		CheckFileManageMapper cfmMapper = conn.getMapper(CheckFileManageMapper.class);
		if (check_file_manage_id != null) {
			// 取得点检项目
			List<DeviceCheckItemEntity> dCsis = cfmMapper.getSeqItemsByFile(check_file_manage_id);

			if(dCsis.size() > 0) {
				Integer trigger = dCsis.get(0).getTrigger_state();
				if (trigger == TYPE_ITEM_WEEK) {
					allowDays = 7;
				}
			}
		}

		if (allowDays > 0) {
			cal.add(Calendar.DATE, -allowDays);
		}

		condEntity.setCheck_confirm_time_start(df.parse(df.format(cal.getTime())));
		int result_cnt = crMapper.getWeekCheck(condEntity);
		if (result_cnt > 0) {
			return "OK";
		} else {
			return "NG";
		}
	}


	private String getFileStatusD(String status, BigDecimal digit) {

		if (digit != null) {
			return getNoScale(digit);
		} else
		if (status == null || "0".equals(status)) {
			return "/";
		} else {
			if ("1".equals(status)) {
				return "〇";
			} else if ("2".equals(status)) {
				return "×";
			} else if ("3".equals(status)) {
				return "△";
			} else if ("4".equals(status)) {
				return "●";
			}
		}
		return "";
	}

	/**
	 * 定位用对象
	 * @author Gong
	 *
	 */
	private class CheckPosBean {
		private int startX = 0;
		private int startY = 0;
		private int shiftX = 1;
		private int shiftY = 1;
		private String content = "";
	}
}