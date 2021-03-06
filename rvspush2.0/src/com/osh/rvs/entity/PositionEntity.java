package com.osh.rvs.entity;

import java.io.Serializable;
import java.sql.Timestamp;

public class PositionEntity implements Serializable {

	private static final long serialVersionUID = 6743605232513994881L;
	/** 工位 ID */
	private String position_id;
	/** 工位名称 */
	private String name;
	
	/** 工程 ID */
	private String line_id;
	/** 工程名称 */
	private String line_name;
	/** 进度代码 */
	private String process_code;
	/** 删除类别 */
	private boolean delete_flg = false;
	/** 最后更新人 */
	private String updated_by;
	/** 最后更新时间 */
	private Timestamp updated_time;

	private Integer light_division_flg;
	/**
	 * 取得工位 ID
	 * @return position_id 工位 ID
	 */
	public String getPosition_id() {
		return position_id;
	}

	/**
	 * 工位 ID设定
	 * @param section_id 工位 ID
	 */
	public void setPosition_id(String position_id) {
		this.position_id = position_id;
	}

	/**
	 * 取得工位名称
	 * @return name 工位名称
	 */
	public String getName() {
		return name;
	}

	/**
	 * 工位名称设定
	 * @param name 工位名称
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 取得删除标记
	 * @return delete_flg 删除标记
	 */
	public boolean isDelete_flg() {
		return delete_flg;
	}

	/**
	 * 删除标记设定
	 * @param delete_flg 删除标记
	 */
	public void setDelete_flg(boolean delete_flg) {
		this.delete_flg = delete_flg;
	}

	/**
	 * 取得最后更新人
	 * @return updated_by 最后更新人
	 */
	public String getUpdated_by() {
		return updated_by;
	}

	/**
	 * 最后更新人设定
	 * @param updated_by 最后更新人
	 */
	public void setUpdated_by(String updated_by) {
		this.updated_by = updated_by;
	}

	/**
	 * 取得最后更新时间
	 * @return updated_time 最后更新时间
	 */
	public Timestamp getUpdated_time() {
		return updated_time;
	}

	/**
	 * 最后更新时间设定
	 * @param updated_time 最后更新时间
	 */
	public void setUpdated_time(Timestamp updated_time) {
		this.updated_time = updated_time;
	}

	public String getLine_id() {
		return line_id;
	}

	public void setLine_id(String line_id) {
		this.line_id = line_id;
	}

	public String getLine_name() {
		return line_name;
	}

	public void setLine_name(String line_name) {
		this.line_name = line_name;
	}

	public String getProcess_code() {
		return process_code;
	}

	public void setProcess_code(String process_code) {
		this.process_code = process_code;
	}

	/**
	 * 文字列化
	 * 
	 * @return 文字列
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(this.position_id).append(", ");
		buffer.append(this.name).append(", ");
		buffer.append(this.line_id).append(". ");
		return buffer.toString();
	}

	public Integer getLight_division_flg() {
		return light_division_flg;
	}

	public void setLight_division_flg(Integer light_division_flg) {
		this.light_division_flg = light_division_flg;
	}
}
