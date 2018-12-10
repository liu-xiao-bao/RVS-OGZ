<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<script type="text/javascript" src="js/partial/partial_collation.js"></script>
<div id="partial_collation">
	<div class="ui-widget-header ui-corner-top ui-helper-clearfix areaencloser">
		<span class="areatitle">零件核对一览</span>
		<a role="link" href="javascript:void(0)" class="HeaderButton areacloser">
			<span class="ui-icon ui-icon-circle-triangle-n"></span>
		</a>
	</div>
	<table id="collationlist" ></table>
	<div id="collationlistpager"></div>
	
	<div class="ui-widget-content" style="padding:6px;">
		<div style="float:left;width:30px;height:20px;background-color: #E48E38"></div>
		<div style="float:left;width:150px;height:20px;font-size:14px;">：此入库单中不存在</div>
		<div class="clear"></div>
	</div>
	
	<div class="areaencloser"></div>
	
	<div class="ui-widget-header ui-corner-all ui-helper-clearfix areabase">
		<div style="margin-top: 6px;margin-left: 6px;">
			<input type="button" class="ui-button" id="startbutton" value="开始">
			<input type="button" class="ui-button" id="breakbutton" value="中断">
			<input type="button" class="ui-button" id="endbutton" value="结束">
		</div>
	</div>
		
	<div class="areaencloser"></div>
	
	<div class="ui-widget-header ui-corner-top ui-helper-clearfix areaencloser">
		<span class="areatitle">作业信息</span>
	</div>
	<div class="ui-widget-content">
		<table class="condform">
			<tbody>
				<tr>
					<td class="ui-state-default td-title">日期:</td>
					<td class="td-content"><span id="label_warehouse_date"></span></td>
					<td class="ui-state-default td-title">DN 编号:</td>
					<td class="td-content"><span id="label_dn_no"></span></td>
					<td class="ui-state-default td-title">作业内容:</td>
					<td class="td-content"><span id="label_production_type_name"></span></td>
				</tr>
			</tbody>
		</table>
	</div>
	
	
	<div class="areaencloser"></div>
	
	<div class="ui-widget-content" id="scanner_container" style="min-height: 215px;display: none;" >
		<div class="ui-state-default td-title">扫描录入区域</div>
		<input type="text" id="scanner_inputer" title="扫描前请点入此处" class="scanner_inputer" style="width: 99.5%"></input>
		<div style="text-align: center;">
			<img src="images/barcode.png" style="margin: auto; width: 150px; padding-top: 4px;">
		</div>
	</div>
	
	<div id="partial_warehouse_dialog" style="display: none;">
	</div>
	
	<div id="choose_spec_kind_dialog" style="display: none;">
	</div>
	
	<div id="message_dialog" style="display: none;"></div>
	
	<input type="hidden" id="hide_key">
	<input type="hidden" id="hide_production_type">
</div>