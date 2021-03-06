package com.osh.rvs.form.master;

import java.io.Serializable;

import org.apache.struts.action.ActionForm;

import framework.huiqing.bean.annotation.BeanField;
import framework.huiqing.bean.annotation.FieldType;

/**
 * 零件出入库工时标准
 *
 * @author liuxb
 *
 */
public class PartialBussinessStandardForm extends ActionForm implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -7315310554695971355L;

	/**
	 * 规格种别
	 */
	@BeanField(title = "规格种别", name = "spec_kind", primaryKey = true, type = FieldType.Integer, length = 1, notNull = true)
	private String spec_kind;

	/**
	 * 装箱数量
	 */
	@BeanField(title = "装箱数量", name = "box_count", type = FieldType.Integer, length = 3, notNull = true)
	private String box_count;

	/**
	 * 收货
	 */
	@BeanField(title = "收货", name = "recept", type = FieldType.Double, length = 5, scale = 2, notNull = true)
	private String recept;

	/**
	 * 拆盒
	 */
	@BeanField(title = "拆盒", name = "collect_case", type = FieldType.Double, length = 5, scale = 2, notNull = true)
	private String  collect_case;

	/**
	 * 核对
	 */
	@BeanField(title = "核对", name = "collation", type = FieldType.Double, length = 5, scale = 2, notNull = true)
	private String collation;

	/**
	 * 上架
	 */
	@BeanField(title = "上架", name = "on_shelf", type = FieldType.Double, length = 5, scale = 2, notNull = true)
	private String on_shelf;

	/**
	 * 分装
	 */
	@BeanField(title = "分装", name = "unpack", type = FieldType.Double, length = 5, scale = 2)
	private String unpack;

	/**
	 * 每批一箱标记
	 */
	@BeanField(title = "每批一箱标记", name = "box_count_flg", type = FieldType.Integer, length = 1)
	private String box_count_flg;

	/**
	 * 核对时上架标记
	 */
	@BeanField(title = "核对时上架标记", name = "collation_flg", type = FieldType.Integer, length = 1)
	private String collation_flg;

	/**
	 * 规格种别名称
	 */
	private String spec_kind_name;

	public String getSpec_kind() {
		return spec_kind;
	}

	public void setSpec_kind(String spec_kind) {
		this.spec_kind = spec_kind;
	}

	public String getBox_count() {
		return box_count;
	}

	public void setBox_count(String box_count) {
		this.box_count = box_count;
	}

	public String getRecept() {
		return recept;
	}

	public void setRecept(String recept) {
		this.recept = recept;
	}

	public String getCollect_case() {
		return collect_case;
	}

	public void setCollect_case(String collect_case) {
		this.collect_case = collect_case;
	}

	public String getCollation() {
		return collation;
	}

	public void setCollation(String collation) {
		this.collation = collation;
	}

	public String getOn_shelf() {
		return on_shelf;
	}

	public void setOn_shelf(String on_shelf) {
		this.on_shelf = on_shelf;
	}

	public String getUnpack() {
		return unpack;
	}

	public void setUnpack(String unpack) {
		this.unpack = unpack;
	}

	public String getSpec_kind_name() {
		return spec_kind_name;
	}

	public void setSpec_kind_name(String spec_kind_name) {
		this.spec_kind_name = spec_kind_name;
	}

	public String getBox_count_flg() {
		return box_count_flg;
	}

	public void setBox_count_flg(String box_count_flg) {
		this.box_count_flg = box_count_flg;
	}

	public String getCollation_flg() {
		return collation_flg;
	}

	public void setCollation_flg(String collation_flg) {
		this.collation_flg = collation_flg;
	}

}
